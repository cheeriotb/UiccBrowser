/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.usecase

import android.content.Context
import com.github.cheeriotb.uiccbrowser.element.BerTlvElement
import com.github.cheeriotb.uiccbrowser.element.fcp.FcpTemplate
import com.github.cheeriotb.uiccbrowser.repository.CardRepository
import com.github.cheeriotb.uiccbrowser.repository.FileId
import com.github.cheeriotb.uiccbrowser.repository.Result
import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString

/**
 * Keeps the latest FCP template for the app-level current directory context.
 *
 * The cache contains only one directory. It is replaced when a different MF, DF, or ADF is
 * prepared, and is not affected by temporary EF access such as EF_ARR reads.
 */
class CurrentDirectoryFcpUseCase(private val context: Context) {

    /** Selects the parent directory of [efFileId] if the cached directory context differs. */
    suspend fun prepareForEf(slotId: Int, efFileId: FileId) {
        val key = DirectoryKey(efFileId.aid, efFileId.path)
        synchronized(lock) {
            if (cachedKey == key && cachedResult != null) return
            cachedKey = null
            cachedResult = null
        }

        val repo = CardRepository.from(context, slotId) ?: return
        if (!repo.isAccessible) return

        val result = repo.readDirectoryFileControlParameters(
                FileId(efFileId.aid, efFileId.path, FileId.FID_ALMIGHTY))
        if (!result.isOk || !isExpectedDirectoryFcp(result.data, key)) return

        synchronized(lock) {
            cachedKey = key
            cachedResult = result
        }
    }

    /** Returns the cached FCP only when it belongs to [efFileId]'s current directory context. */
    fun queryForEf(efFileId: FileId): Result? = synchronized(lock) {
        val key = DirectoryKey(efFileId.aid, efFileId.path)
        cachedResult.takeIf { cachedKey == key }
    }

    private fun isExpectedDirectoryFcp(data: ByteArray, key: DirectoryKey): Boolean {
        val fcp = FcpTemplate.decode(context.resources, data) ?: return false
        if (key.path == FileId.PATH_ADF) {
            val dfName = fcp.subElements
                .filterIsInstance<BerTlvElement>()
                .find { it.tag == FcpTemplate.TAG_DF_NAME_AID }
                ?.data
                ?: return false
            return byteArrayToHexString(dfName) == key.aid
        }

        val fileIdentifier = fcp.subElements
            .filterIsInstance<BerTlvElement>()
            .find { it.tag == FcpTemplate.TAG_FILE_IDENTIFIER }
            ?.data
            ?: return false
        return byteArrayToHexString(fileIdentifier) == expectedDirectoryFid(key.path)
    }

    private fun expectedDirectoryFid(path: String): String =
        if (path == FileId.PATH_MF) FID_MF else path.takeLast(FID_HEX_LENGTH)

    private data class DirectoryKey(val aid: String, val path: String)

    companion object {
        private const val FID_MF = "3F00"
        private const val FID_HEX_LENGTH = 4
        private val lock = Any()
        private var cachedKey: DirectoryKey? = null
        private var cachedResult: Result? = null

        /** Clears the process-wide cache and returns it to the initial empty state. */
        internal fun clearCache() {
            synchronized(lock) {
                cachedKey = null
                cachedResult = null
            }
        }
    }
}
