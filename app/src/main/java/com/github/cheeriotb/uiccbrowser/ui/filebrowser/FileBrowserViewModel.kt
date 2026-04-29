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
import com.github.cheeriotb.uiccbrowser.usecase.CacheFileControlParametersUseCase
import com.github.cheeriotb.uiccbrowser.usecase.FileEntry
import com.github.cheeriotb.uiccbrowser.usecase.GetFileListUseCase
import com.github.cheeriotb.uiccbrowser.repository.FileId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FileBrowserViewModel(
    application: Application,
    val rawResId: Int,
    private val slotId: Int,
    val aid: String,
    val parentPath: String,
    val title: String
) : AndroidViewModel(application) {

    private val cacheFiles = CacheFileControlParametersUseCase(application.applicationContext)
    private val getFileList = GetFileListUseCase(application.applicationContext)

    private val _entries = MutableStateFlow<List<FileEntry>>(emptyList())
    val entries: StateFlow<List<FileEntry>> = _entries.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            _isLoading.value = true
            if (aid != FileId.AID_NONE && parentPath == FileId.PATH_ADF) {
                cacheFiles.execute(rawResId, slotId, aid)
            }
            _entries.value = getFileList.execute(rawResId, slotId, aid, parentPath)
            _isLoading.value = false
        }
    }

    class Factory(
        private val application: Application,
        private val rawResId: Int,
        private val slotId: Int,
        private val aid: String,
        private val parentPath: String,
        private val title: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FileBrowserViewModel(application, rawResId, slotId, aid, parentPath, title) as T
    }

    companion object {
        /**
         * Builds the action bar title for a root-level destination.
         * If the JSON has an id field, it is shown in parentheses.
         */
        fun buildRootTitle(name: String, id: String): String =
            if (id.isEmpty()) name else "$name ($id)"

        /**
         * Builds the action bar title for a sub-level DF destination.
         * The path in parentheses is the display path of the DF (e.g. "7FFF/5FC0").
         */
        fun buildSubTitle(dfName: String, dfId: String, parentDisplayPath: String): String =
            "$dfName ($parentDisplayPath/$dfId)"

        /**
         * Converts a concatenated hex path into a slash-separated display path.
         * Each file ID is 4 hex characters, so "7FFF5FC0" becomes "7FFF/5FC0".
         */
        fun formatDisplayPath(hexPath: String): String =
            hexPath.chunked(4).joinToString("/")
    }
}
