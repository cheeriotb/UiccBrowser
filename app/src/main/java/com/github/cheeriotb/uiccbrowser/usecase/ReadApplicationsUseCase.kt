/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.usecase

import android.content.Context
import android.util.Log
import com.github.cheeriotb.uiccbrowser.element.BerTlvElement
import com.github.cheeriotb.uiccbrowser.element.ef.AppTemplate
import com.github.cheeriotb.uiccbrowser.element.fcp.FcpTemplate
import com.github.cheeriotb.uiccbrowser.repository.CardRepository
import com.github.cheeriotb.uiccbrowser.repository.FileId
import com.github.cheeriotb.uiccbrowser.repository.ReadAllRecordsParams
import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString

class ReadApplicationsUseCase(private val context: Context) {

    private val tag = ReadApplicationsUseCase::class.java.simpleName

    /**
     * Reads EF DIR records from the given slot, parses each record as an Application template,
     * and returns the AID of every application found on the card.
     *
     * Prerequisite: the FCP for EF DIR must already be cached via
     * CacheFileControlParametersUseCase (CardRepository.cacheFileControlParameters).
     *
     * Returns an empty list if EF DIR does not exist, could not be read, or contains no
     * recognisable Application template records.
     */
    suspend fun execute(slotId: Int): List<String> {
        val repo = CardRepository.from(context, slotId) ?: return emptyList()

        val dirFileId = FileId.Builder()
                .aid(FileId.AID_NONE)
                .path(FileId.PATH_MF)
                .fileId(FileId.EF_DIR)
                .build()

        // Retrieve the cached FCP for EF DIR to learn the record parameters.
        val fcpResults = repo.queryFileControlParameters(dirFileId)
        if (fcpResults.isEmpty() || !fcpResults[0].isOk) return emptyList()

        val (recordSize, numberOfRecords) =
                recordParamsFrom(fcpResults[0].data) ?: return emptyList()

        val params = ReadAllRecordsParams.Builder(dirFileId)
                .recordSize(recordSize)
                .numberOfRecords(numberOfRecords)
                .build()

        val records = repo.readAllRecords(params)

        val aids = mutableListOf<String>()
        for (record in records) {
            if (!record.isOk) continue
            val template = AppTemplate.decode(context.resources, record.data) ?: continue
            val aid = aidFrom(template) ?: continue
            Log.i(tag, "AID: $aid")
            aids.add(aid)
        }
        return aids
    }

    /**
     * Extracts (recordSize, numberOfRecords) from the raw FCP bytes of a linear fixed EF by
     * parsing with FcpTemplate and reading the File Descriptor (tag 0x82).
     * Returns null when the FCP cannot be parsed or does not describe a record-structured EF.
     */
    private fun recordParamsFrom(fcpBytes: ByteArray): Pair<Int, Int>? {
        val fcpElement = FcpTemplate.decode(context.resources, fcpBytes) ?: return null
        val fdElement = fcpElement.subElements
                .filterIsInstance<BerTlvElement>()
                .find { it.tag == FcpTemplate.TAG_FILE_DESCRIPTOR }
                ?: return null

        // File Descriptor for a linear fixed or cyclic EF is 5 bytes:
        //   [0] file descriptor byte
        //   [1] data coding byte
        //   [2..3] record length (big-endian)
        //   [4] number of records
        val fd = fdElement.data
        if (fd.size < 5) return null

        val recordSize = ((fd[2].toInt() and 0xFF) shl 8) or (fd[3].toInt() and 0xFF)
        val numberOfRecords = fd[4].toInt() and 0xFF
        return Pair(recordSize, numberOfRecords)
    }

    /**
     * Finds the Application Identifier (tag 0x4F) within a decoded Application template element
     * and returns its value as an upper-case hex string.
     */
    private fun aidFrom(template: BerTlvElement): String? {
        val aidElement = template.subElements
                .filterIsInstance<BerTlvElement>()
                .find { it.tag == AppTemplate.TAG_APPLICATION_ID }
                ?: return null
        return byteArrayToHexString(aidElement.data)
    }
}
