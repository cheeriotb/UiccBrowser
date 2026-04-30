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

    private var recordLength = 0

    init {
        viewModelScope.launch {
            _isLoading.value = true
            val binaryData = ReadBinaryUseCase(getApplication()).execute(slotId, fileId)
            if (binaryData != null) {
                _data.value = binaryData
                _isLoading.value = false
                return@launch
            }
            val info = ReadRecordUseCase(getApplication()).getInfo(slotId, fileId)
            if (info != null) {
                recordLength = info.recordLength
                _recordCount.value = info.numberOfRecords
                _data.value = ReadRecordUseCase(getApplication())
                    .execute(slotId, fileId, 1, recordLength)
            }
            _isLoading.value = false
        }
    }

    fun loadRecord(recordNo: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _data.value = ReadRecordUseCase(getApplication())
                .execute(slotId, fileId, recordNo, recordLength)
            _isLoading.value = false
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
