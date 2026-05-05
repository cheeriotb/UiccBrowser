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
import com.github.cheeriotb.uiccbrowser.repository.Result

class ReadRecordUseCase(private val context: Context) {

    data class LinearFixedInfo(val recordLength: Int, val numberOfRecords: Int)
    data class InfoOutcome(val info: LinearFixedInfo? = null, val error: Result? = null)
    data class ReadOutcome(val data: ByteArray? = null, val error: Result? = null)

    /**
     * Returns [LinearFixedInfo] if [fileId] is a Linear Fixed EF, null otherwise.
     * Uses only the FCP cache — no card I/O is performed.
     */
    suspend fun getInfo(slotId: Int, fileId: FileId): LinearFixedInfo? {
        return getInfoDetailed(slotId, fileId).info
    }

    suspend fun getInfoDetailed(slotId: Int, fileId: FileId): InfoOutcome {
        val repo = CardRepository.from(context, slotId) ?: return InfoOutcome()
        if (!repo.isAccessible) return InfoOutcome()

        val fcpResults = repo.queryFileControlParameters(fileId)
        val fcpError = fcpResults.firstOrNull { !it.isOk }
        if (fcpError != null) return InfoOutcome(error = fcpError)
        val fcpData = fcpResults.firstOrNull { it.isOk }?.data ?: return InfoOutcome()

        val fcpElement = FcpTemplate.decode(context.resources, fcpData) ?: return InfoOutcome()

        val fdElement = fcpElement.subElements
            .filterIsInstance<BerTlvElement>()
            .find { it.tag == FcpTemplate.TAG_FILE_DESCRIPTOR }
            ?: return InfoOutcome()

        // Bits 2-0 of the File Descriptor Byte: 0x02 = Linear Fixed EF (ETSI TS 102.221 §11.1.1.4.3)
        // The FD element must be at least 5 bytes to contain record length and number of records.
        if (fdElement.data.size < 5 || fdElement.data[0].toInt() and 0x07 != 0x02) return InfoOutcome()

        val recordLength = ((fdElement.data[2].toInt() and 0xFF) shl 8) or
                (fdElement.data[3].toInt() and 0xFF)
        val numberOfRecords = fdElement.data[4].toInt() and 0xFF

        return InfoOutcome(LinearFixedInfo(recordLength, numberOfRecords))
    }

    /**
     * Reads the record at [recordNo] (1-based) from the Linear Fixed EF identified by [fileId].
     * Returns null if the repository is inaccessible or the READ RECORD command fails.
     */
    suspend fun execute(slotId: Int, fileId: FileId, recordNo: Int, recordLength: Int): ByteArray? {
        return executeDetailed(slotId, fileId, recordNo, recordLength).data
    }

    suspend fun executeDetailed(
        slotId: Int,
        fileId: FileId,
        recordNo: Int,
        recordLength: Int
    ): ReadOutcome {
        val repo = CardRepository.from(context, slotId) ?: return ReadOutcome()
        if (!repo.isAccessible) return ReadOutcome()

        val result = repo.readRecord(ReadRecordParams(fileId, recordNo, recordLength))
        if (!result.isOk) return ReadOutcome(error = result)
        return ReadOutcome(data = result.data.takeIf { it.isNotEmpty() })
    }
}
