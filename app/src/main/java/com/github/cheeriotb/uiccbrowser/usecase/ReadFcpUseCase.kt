/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.usecase

import android.content.Context
import com.github.cheeriotb.uiccbrowser.repository.CardRepository
import com.github.cheeriotb.uiccbrowser.repository.FileId

class ReadFcpUseCase(private val context: Context) {

    suspend fun execute(slotId: Int, fileId: FileId): ByteArray? {
        val repo = CardRepository.from(context, slotId) ?: return null
        if (!repo.isAccessible) return null
        return repo.queryFileControlParameters(fileId).firstOrNull { it.isOk }?.data
    }
}
