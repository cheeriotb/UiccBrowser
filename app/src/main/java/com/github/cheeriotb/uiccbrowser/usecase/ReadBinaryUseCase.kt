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
import com.github.cheeriotb.uiccbrowser.repository.ReadBinaryParams
import com.github.cheeriotb.uiccbrowser.repository.Result

class ReadBinaryUseCase(private val context: Context) {

    data class ReadOutcome(val data: ByteArray? = null, val error: Result? = null)

    /**
     * Reads the full content of a Transparent EF identified by [fileId] on slot [slotId].
     *
     * Returns null if the repository is inaccessible, the FCP cannot be parsed, the EF is not
     * Transparent, or any READ BINARY command fails.
     */
    suspend fun execute(slotId: Int, fileId: FileId): ByteArray? {
        return executeDetailed(slotId, fileId).data
    }

    suspend fun executeDetailed(slotId: Int, fileId: FileId): ReadOutcome {
        val repo = CardRepository.from(context, slotId) ?: return ReadOutcome()
        if (!repo.isAccessible) return ReadOutcome()

        val fcpResults = repo.queryFileControlParameters(fileId)
        val fcpError = fcpResults.firstOrNull { !it.isOk }
        if (fcpError != null) return ReadOutcome(error = fcpError)
        val fcpData = fcpResults.firstOrNull { it.isOk }?.data ?: return ReadOutcome()

        val fcpElement = FcpTemplate.decode(context.resources, fcpData) ?: return ReadOutcome()

        val fdElement = fcpElement.subElements
            .filterIsInstance<BerTlvElement>()
            .find { it.tag == FcpTemplate.TAG_FILE_DESCRIPTOR }
            ?: return ReadOutcome()

        // Bits 2-0 of the File Descriptor Byte: 0x01 = Transparent EF (ETSI TS 102.221 §11.1.1.4.3)
        if (fdElement.data.isEmpty() || fdElement.data[0].toInt() and 0x07 != 0x01) return ReadOutcome()

        val fileSizeElement = fcpElement.subElements
            .filterIsInstance<BerTlvElement>()
            .find { it.tag == FcpTemplate.TAG_FILE_SIZE }
            ?: return ReadOutcome()

        val sizeBytes = fileSizeElement.data
        if (sizeBytes.size < 2) return ReadOutcome()
        val fileSize = ((sizeBytes[0].toInt() and 0xFF) shl 8) or (sizeBytes[1].toInt() and 0xFF)
        if (fileSize == 0) return ReadOutcome(data = ByteArray(0))

        val chunks = mutableListOf<ByteArray>()
        var offset = 0
        while (offset < fileSize) {
            val chunkSize = minOf(CardRepository.SIZE_MAX, fileSize - offset)
            val result = repo.readBinary(ReadBinaryParams(fileId, offset, chunkSize))
            if (!result.isOk) return ReadOutcome(error = result)
            if (result.data.isEmpty()) return ReadOutcome()
            chunks.add(result.data)
            offset += result.data.size
        }

        return ReadOutcome(data = chunks.reduce { acc, chunk -> acc + chunk })
    }
}
