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
import com.github.cheeriotb.uiccbrowser.element.EfDecoderRegistry
import com.github.cheeriotb.uiccbrowser.element.Element
import com.github.cheeriotb.uiccbrowser.repository.FileId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InfoViewModel(
    application: Application,
    private val fileId: FileId
) : AndroidViewModel(application) {

    private val _element = MutableStateFlow<Element?>(null)
    val element: StateFlow<Element?> = _element.asStateFlow()

    fun decode(data: ByteArray) {
        val decoder = EfDecoderRegistry.find(fileId.aid, fileId.path + fileId.fileId) ?: return
        _element.value = decoder(getApplication<Application>().resources, data)
    }

    class Factory(
        private val application: Application,
        private val fileId: FileId
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            InfoViewModel(application, fileId) as T
    }
}
