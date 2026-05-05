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
import com.github.cheeriotb.uiccbrowser.repository.FileId
import com.github.cheeriotb.uiccbrowser.repository.Result
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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _recordCount = MutableStateFlow(0)
    val recordCount: StateFlow<Int> = _recordCount.asStateFlow()

    private val _error = MutableStateFlow<Result?>(null)
    val error: StateFlow<Result?> = _error.asStateFlow()

    private var dataSource = DataSource.UNKNOWN
    private var recordLength = 0
    private var currentRecordNo = 1

    init {
        loadInitialData()
    }

    fun loadRecord(recordNo: Int) {
        currentRecordNo = recordNo
        readRecord(recordNo)
    }

    fun refresh() {
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

        if (readBinary()) return

        val readRecordUseCase = ReadRecordUseCase(getApplication())
        val infoResult = readRecordUseCase.getInfoDetailed(slotId, fileId)
        if (infoResult.error != null) {
            _error.value = infoResult.error
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
            _error.value = binaryResult.error
            return true
        }
        val data = binaryResult.data ?: return false
        dataSource = DataSource.TRANSPARENT
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
            _error.value = recordResult.error
        } else {
            _data.value = recordResult.data
        }
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
