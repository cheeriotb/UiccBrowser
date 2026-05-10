/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui.filebrowser

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.cheeriotb.uiccbrowser.element.BerTlvElement
import com.github.cheeriotb.uiccbrowser.element.fcp.FcpTemplate
import com.github.cheeriotb.uiccbrowser.repository.Result
import com.github.cheeriotb.uiccbrowser.usecase.CurrentDirectoryFcpUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CurrentDirectoryFcpViewModel(
    application: Application,
    private val slotId: Int,
    private val aid: String,
    private val path: String
) : AndroidViewModel(application) {

    private val currentDirectoryFcp =
        CurrentDirectoryFcpUseCase(application.applicationContext)

    private val _element = MutableStateFlow<BerTlvElement?>(null)
    val element: StateFlow<BerTlvElement?> = _element.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<Result?>(null)
    val error: StateFlow<Result?> = _error.asStateFlow()

    init {
        load()
    }

    fun clearError() {
        _error.value = null
    }

    /** Loads the current directory FCP from cache, preparing it only when missing. */
    private fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = cachedResult() ?: run {
                currentDirectoryFcp.prepareForDirectory(slotId, aid, path)
                cachedResult()
            }
            if (result == null) {
                _error.value = Result.Builder(path)
                    .data(Result.DATA_NONE)
                    .sw(Result.SW_NOT_FOUND)
                    .build()
            } else if (!result.isOk) {
                _error.value = result
            } else {
                _element.value = FcpTemplate.decode(getApplication<Application>().resources,
                    result.data)
            }
            _isLoading.value = false
        }
    }

    private fun cachedResult(): Result? =
        currentDirectoryFcp.queryForDirectory(aid, path)

    class Factory(
        private val application: Application,
        private val slotId: Int,
        private val aid: String,
        private val path: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CurrentDirectoryFcpViewModel(application, slotId, aid, path) as T
    }
}
