/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui.efdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.cheeriotb.uiccbrowser.repository.CardRepository
import com.github.cheeriotb.uiccbrowser.repository.FileId
import com.github.cheeriotb.uiccbrowser.repository.Result
import com.github.cheeriotb.uiccbrowser.repository.UpdateBinaryParams
import com.github.cheeriotb.uiccbrowser.repository.UpdateRecordParams
import com.github.cheeriotb.uiccbrowser.usecase.ReadBinaryUseCase
import com.github.cheeriotb.uiccbrowser.usecase.ReadRecordUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BinaryViewModel(
    application: Application,
    private val fileId: FileId,
    private val slotId: Int
) : AndroidViewModel(application) {

    private val _data = MutableStateFlow<ByteArray?>(null)
    val data: StateFlow<ByteArray?> = _data.asStateFlow()

    private val _editState = MutableStateFlow(EditState())
    val editState: StateFlow<EditState> = _editState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _recordCount = MutableStateFlow(0)
    val recordCount: StateFlow<Int> = _recordCount.asStateFlow()

    private val _error = MutableStateFlow<Result?>(null)
    val error: StateFlow<Result?> = _error.asStateFlow()

    private val _readError = MutableStateFlow<Result?>(null)
    val readError: StateFlow<Result?> = _readError.asStateFlow()

    private var dataSource = DataSource.UNKNOWN
    private var recordLength = 0
    private var currentRecordNo = 1

    init {
        loadInitialData()
    }

    fun loadRecord(recordNo: Int) {
        cancelEditMode()
        currentRecordNo = recordNo
        readRecord(recordNo)
    }

    fun refresh() {
        cancelEditMode()
        viewModelScope.launch {
            _isLoading.value = true
            when (buildRefreshTarget(dataSource, recordLength)) {
                RefreshTarget.BINARY -> readBinary()
                RefreshTarget.CURRENT_RECORD -> readCurrentRecord()
                RefreshTarget.INITIAL -> loadInitialDataInProgress()
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    /** Starts editing at the first byte and keeps the cursor in view. */
    fun startEditMode() {
        val data = _data.value ?: return
        _editState.value = EditState(
            enabled = data.isNotEmpty(),
            cursorIndex = if (data.isNotEmpty()) 0 else NO_CURSOR
        )
    }

    /** Stops editing and clears cursor/pending nibble state. */
    fun cancelEditMode() {
        _editState.value = EditState()
    }

    /** Moves the edit cursor after committing a pending single nibble with a leading zero. */
    fun moveCursor(index: Int) {
        val data = _data.value ?: return
        val current = _editState.value
        if (!current.enabled || index !in data.indices) return
        commitPendingNibbleWithLeadingZero()
        _editState.value = _editState.value.copy(cursorIndex = index)
    }

    fun inputHexDigit(digit: Char) {
        val normalized = digit.uppercaseChar()
        if (normalized !in HEX_DIGITS) return
        val data = _data.value ?: return
        val state = _editState.value
        val index = state.cursorIndex
        if (!state.enabled || index !in data.indices) return

        val pending = state.pendingNibble
        if (pending == null) {
            _editState.value = state.copy(pendingNibble = normalized)
            return
        }

        val updated = data.copyOf()
        updated[index] = ((hexValue(pending) shl 4) or hexValue(normalized)).toByte()
        _data.value = updated
        _editState.value = state.copy(
            cursorIndex = nextCursorIndex(index, updated.size),
            pendingNibble = null
        )
    }

    fun insertByte() {
        commitPendingNibbleWithLeadingZero()
        val data = _data.value ?: return
        val state = _editState.value
        val index = state.cursorIndex
        if (!state.enabled || index !in data.indices) return
        _data.value = insertByteAt(data, index)
        _editState.value = state.copy(cursorIndex = index, pendingNibble = null)
    }

    fun deleteByte() {
        commitPendingNibbleWithLeadingZero()
        val data = _data.value ?: return
        val state = _editState.value
        val index = state.cursorIndex
        if (!state.enabled || index !in data.indices) return
        _data.value = deleteByteAt(data, index)
        _editState.value = state.copy(cursorIndex = index, pendingNibble = null)
    }

    suspend fun save(repo: CardRepository): Result? {
        commitPendingNibbleWithLeadingZero()
        val data = _data.value ?: return null
        return when (dataSource) {
            DataSource.TRANSPARENT -> repo.updateBinary(
                UpdateBinaryParams(fileId = fileId, data = data)
            )
            DataSource.LINEAR_FIXED -> repo.updateRecord(
                UpdateRecordParams(fileId = fileId, recordNo = currentRecordNo, data = data)
            )
            DataSource.UNKNOWN -> null
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            loadInitialDataInProgress()
            _isLoading.value = false
        }
    }

    private suspend fun loadInitialDataInProgress() {
        dataSource = DataSource.UNKNOWN
        recordLength = 0
        currentRecordNo = 1
        _recordCount.value = 0
        cancelEditMode()

        if (readBinary()) return

        val readRecordUseCase = ReadRecordUseCase(getApplication())
        val infoResult = readRecordUseCase.getInfoDetailed(slotId, fileId)
        if (infoResult.error != null) {
            setReadError(infoResult.error)
            return
        }
        val info = infoResult.info ?: return
        dataSource = DataSource.LINEAR_FIXED
        recordLength = info.recordLength
        _recordCount.value = info.numberOfRecords
        readCurrentRecord()
    }

    private suspend fun readBinary(): Boolean {
        val binaryResult = ReadBinaryUseCase(getApplication()).executeDetailed(slotId, fileId)
        if (binaryResult.error != null) {
            setReadError(binaryResult.error)
            return true
        }
        val data = binaryResult.data ?: return false
        dataSource = DataSource.TRANSPARENT
        _readError.value = null
        _data.value = data
        return true
    }

    private fun readRecord(recordNo: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            readRecordInProgress(recordNo)
            _isLoading.value = false
        }
    }

    private suspend fun readCurrentRecord() {
        readRecordInProgress(currentRecordNo)
    }

    private suspend fun readRecordInProgress(recordNo: Int) {
        val recordResult = ReadRecordUseCase(getApplication())
            .executeDetailed(slotId, fileId, recordNo, recordLength)
        if (recordResult.error != null) {
            setReadError(recordResult.error)
        } else {
            _readError.value = null
            _data.value = recordResult.data
        }
    }

    private fun commitPendingNibbleWithLeadingZero() {
        val data = _data.value ?: return
        val state = _editState.value
        val pending = state.pendingNibble ?: return
        val index = state.cursorIndex
        if (!state.enabled || index !in data.indices) return

        val updated = data.copyOf()
        updated[index] = hexValue(pending).toByte()
        _data.value = updated
        _editState.value = state.copy(
            cursorIndex = nextCursorIndex(index, updated.size),
            pendingNibble = null
        )
    }

    data class EditState(
        val enabled: Boolean = false,
        val cursorIndex: Int = NO_CURSOR,
        val pendingNibble: Char? = null
    )

    private fun setReadError(result: Result) {
        _readError.value = result
        _error.value = result
    }

    internal enum class DataSource {
        UNKNOWN,
        TRANSPARENT,
        LINEAR_FIXED
    }

    internal enum class RefreshTarget {
        INITIAL,
        BINARY,
        CURRENT_RECORD
    }

    companion object {
        internal fun buildRefreshTarget(
            dataSource: DataSource,
            recordLength: Int
        ): RefreshTarget = when (dataSource) {
            DataSource.TRANSPARENT -> RefreshTarget.BINARY
            DataSource.LINEAR_FIXED ->
                if (recordLength > 0) RefreshTarget.CURRENT_RECORD else RefreshTarget.INITIAL
            DataSource.UNKNOWN -> RefreshTarget.INITIAL
        }

        /** Converts [position] in the 9-column grid to a byte index, or null for offsets/blanks. */
        internal fun byteIndexForGridPosition(position: Int, dataSize: Int): Int? {
            if (position < 0 || position % 9 == 0) return null
            val row = position / 9
            val byteIndex = row * 8 + position % 9 - 1
            return if (byteIndex in 0 until dataSize) byteIndex else null
        }

        /** Converts [byteIndex] to the adapter position in the 9-column grid. */
        internal fun gridPositionForByteIndex(byteIndex: Int): Int =
            (byteIndex / 8) * 9 + byteIndex % 8 + 1

        internal fun nextCursorIndex(cursorIndex: Int, dataSize: Int): Int =
            if (cursorIndex + 1 < dataSize) cursorIndex + 1 else cursorIndex

        internal fun insertByteAt(data: ByteArray, index: Int): ByteArray {
            if (index !in data.indices) return data
            val result = data.copyOf()
            result[index] = 0x00
            for (i in index + 1 until data.size) {
                result[i] = data[i - 1]
            }
            return result
        }

        internal fun deleteByteAt(data: ByteArray, index: Int): ByteArray {
            if (index !in data.indices) return data
            val result = data.copyOf()
            for (i in index until data.lastIndex) {
                result[i] = data[i + 1]
            }
            result[data.lastIndex] = 0xFF.toByte()
            return result
        }

        private const val NO_CURSOR = -1
        private const val HEX_DIGITS = "0123456789ABCDEF"

        private fun hexValue(digit: Char): Int = HEX_DIGITS.indexOf(digit)
    }

    class Factory(
        private val application: Application,
        private val fileId: FileId,
        private val slotId: Int
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BinaryViewModel(application, fileId, slotId) as T
    }
}
