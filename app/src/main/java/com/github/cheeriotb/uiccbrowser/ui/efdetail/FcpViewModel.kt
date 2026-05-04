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
import com.github.cheeriotb.uiccbrowser.element.BerTlvElement
import com.github.cheeriotb.uiccbrowser.element.fcp.FcpTemplate
import com.github.cheeriotb.uiccbrowser.repository.FileId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FcpViewModel(
    application: Application,
    @Suppress("UNUSED_PARAMETER") fileId: FileId
) : AndroidViewModel(application) {

    private val _element = MutableStateFlow<BerTlvElement?>(null)
    val element: StateFlow<BerTlvElement?> = _element.asStateFlow()

    fun decode(data: ByteArray) {
        _element.value = FcpTemplate.decode(getApplication<Application>().resources, data)
    }

    class Factory(
        private val application: Application,
        private val fileId: FileId
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FcpViewModel(application, fileId) as T
    }
}
