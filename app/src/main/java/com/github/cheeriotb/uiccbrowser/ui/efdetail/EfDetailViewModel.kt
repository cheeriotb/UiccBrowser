/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui.efdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.cheeriotb.uiccbrowser.element.EfDecoderRegistry
import com.github.cheeriotb.uiccbrowser.repository.FileId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EfDetailViewModel(
    val efName: String,
    val efFileId: String,
    val fileId: FileId
) : ViewModel() {

    val title: String = "$efName ($efFileId)"
    val hasDecoder: Boolean = EfDecoderRegistry.has(fileId.aid, fileId.path + fileId.fileId)

    private val _isEditModeEnabled = MutableStateFlow(false)

    /** Indicates whether write editing controls should be visible for this EF. */
    val isEditModeEnabled: StateFlow<Boolean> = _isEditModeEnabled.asStateFlow()

    fun enableEditMode() {
        _isEditModeEnabled.value = true
    }

    class Factory(
        private val efName: String,
        private val efFileId: String,
        private val aid: String,
        private val parentPath: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EfDetailViewModel(efName, efFileId, FileId(aid, parentPath, efFileId)) as T
    }
}
