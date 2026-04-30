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
import com.github.cheeriotb.uiccbrowser.repository.ReadRecordParams

class ReadRecordUseCase(private val context: Context) {

    data class LinearFixedInfo(val recordLength: Int, val numberOfRecords: Int)

    /**
     * Returns [LinearFixedInfo] if [fileId] is a Linear Fixed EF, null otherwise.
     * Uses only the FCP cache — no card I/O is performed.
     */
    suspend fun getInfo(slotId: Int, fileId: FileId): LinearFixedInfo? {
        val repo = CardRepository.from(context, slotId) ?: return null
        if (!repo.isAccessible) return null

        val fcpResults = repo.queryFileControlParameters(fileId)
        val fcpData = fcpResults.firstOrNull { it.isOk }?.data ?: return null

        val fcpElement = FcpTemplate.decode(context.resources, fcpData) ?: return null

        val fdElement = fcpElement.subElements
            .filterIsInstance<BerTlvElement>()
            .find { it.tag == FcpTemplate.TAG_FILE_DESCRIPTOR }
            ?: return null

        // Bits 2-0 of the File Descriptor Byte: 0x02 = Linear Fixed EF (ETSI TS 102.221 §11.1.1.4.3)
        // The FD element must be at least 5 bytes to contain record length and number of records.
        if (fdElement.data.size < 5 || fdElement.data[0].toInt() and 0x07 != 0x02) return null

        val recordLength = ((fdElement.data[2].toInt() and 0xFF) shl 8) or
                (fdElement.data[3].toInt() and 0xFF)
        val numberOfRecords = fdElement.data[4].toInt() and 0xFF

        return LinearFixedInfo(recordLength, numberOfRecords)
    }

    /**
     * Reads the record at [recordNo] (1-based) from the Linear Fixed EF identified by [fileId].
     * Returns null if the repository is inaccessible or the READ RECORD command fails.
     */
    suspend fun execute(slotId: Int, fileId: FileId, recordNo: Int, recordLength: Int): ByteArray? {
        val repo = CardRepository.from(context, slotId) ?: return null
        if (!repo.isAccessible) return null

        val result = repo.readRecord(ReadRecordParams(fileId, recordNo, recordLength))
        return if (result.isOk && result.data.isNotEmpty()) result.data else null
    }
}
