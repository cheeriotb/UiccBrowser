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
import com.github.cheeriotb.uiccbrowser.repository.FileId

class EfDetailViewModel(
    val efName: String,
    val efFileId: String,
    val fileId: FileId
) : ViewModel() {

    val title: String = "$efName ($efFileId)"

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
