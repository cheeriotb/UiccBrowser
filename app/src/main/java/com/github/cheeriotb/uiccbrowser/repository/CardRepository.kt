/*
 *  Copyright (C) 2020-2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.repository

import android.content.Context
import android.util.Log
import com.github.cheeriotb.uiccbrowser.cacheio.SelectResponse
import com.github.cheeriotb.uiccbrowser.cacheio.SelectResponseDataSource
import com.github.cheeriotb.uiccbrowser.cacheio.SelectResponseDataSourceImpl
import com.github.cheeriotb.uiccbrowser.cardio.Command
import com.github.cheeriotb.uiccbrowser.cardio.Interface
import com.github.cheeriotb.uiccbrowser.cardio.Iso7816
import com.github.cheeriotb.uiccbrowser.cardio.Response
import com.github.cheeriotb.uiccbrowser.cardio.TelephonyInterface
import com.github.cheeriotb.uiccbrowser.util.byteToHexString
import com.github.cheeriotb.uiccbrowser.util.hexStringToByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CardRepository private constructor (
    slotId: Int,
    private val cardIo: Interface,
    private val cacheIo: SelectResponseDataSource
) {
    private val tag = TelephonyInterface::class.java.simpleName + slotId
    private var _iccId: String? = null
    private var closingJob: Job? = null

    val iccId: String? get() = _iccId

    val isAccessible: Boolean
        get() = (_iccId != null)

    companion object {
        @Volatile
        private var instances: ArrayList<CardRepository>? = null

        fun from(
            context: Context,
            slotId: Int
        ): CardRepository? {
            synchronized(this) {
                if (instances == null) {
                    instances = ArrayList()
                    val cacheIo = SelectResponseDataSourceImpl.from(context)
                    var testSlotId = 0
                    do {
                        val cardIo: Interface = TelephonyInterface.from(context, testSlotId)
                        if (!cardIo.isAvailable) break
                        instances!!.add(CardRepository(testSlotId, cardIo, cacheIo))
                        testSlotId++
                    } while (true)
                }
            }

            return if (instances!!.size > slotId) instances!![slotId] else null
        }

        const val SIZE_MAX = 0x100

        const val SW_INTERNAL_EXCEPTION = Iso7816.SW1_INTERNAL_EXCEPTION shl 8
        val DATA_NONE = ByteArray(0)

        private const val CLOSING_TIMER_MILLIS = 500L
    }

    suspend fun initialize(): Boolean {
        _iccId = null

        if (openChannel()) {
            val selectResponse = select(FileId.PATH_MF, FileId.EF_ICCID, fcpRequest = true)
            if (selectResponse.isOk) {
                val readResponse = readBinary()
                startClosingTimer()
                if (readResponse.isOk && (readResponse.data.size == 10)) {
                    // Refer to the clause 13.2 of ETSI TS 102 221 for the format of ICCID
                    val builder = StringBuilder()
                    for (byte in readResponse.data) {
                        val hexString = byteToHexString(byte.toInt())
                        withContext(Dispatchers.IO) {
                            builder.append(hexString[1]).append(hexString[0])
                        }
                    }
                    _iccId = builder.toString()
                    Log.d(tag, "ICCID: $_iccId")

                    if (cacheIo.get(_iccId!!, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) == null) {
                        cacheIo.insert(SelectResponse(_iccId!!, FileId.AID_NONE, FileId.PATH_MF,
                            FileId.EF_ICCID, selectResponse.data, selectResponse.sw))
                    }

                    Log.i(tag, "The card associated with the slot is accessible")
                    return true
                }
            }
        }

        Log.i(tag, "The card associated with the slot is not accessible")
        return false
    }

    suspend fun cacheFileControlParameters(
        fileId: FileId
    ): Boolean {
        if (!isAccessible || !openChannel(fileId.aid)) {
            return false // means that the caching process cannot be continued.
        }
        if (cacheIo.get(_iccId!!, fileId.aid, fileId.path, fileId.fileId) != null) {
            return true // No need to cache it again
        }

        val response = select(fileId.path, fileId.fileId, fcpRequest = true)
        startClosingTimer()

        cacheIo.insert(SelectResponse(_iccId!!, fileId.aid, fileId.path, fileId.fileId,
                response.data, response.sw))

        return true // means that the caching process can be continued.
    }

    suspend fun queryFileControlParameters(
        fileId: FileId
    ): List<Result> {
        if (!isAccessible) return emptyList()
        return if (fileId.fileId == FileId.FID_ALMIGHTY) {
            Result.listFrom(cacheIo.getAll(_iccId!!, fileId.aid, fileId.path))
        } else {
            Result.from(cacheIo.get(_iccId!!, fileId.aid, fileId.path, fileId.fileId))
        }
    }

    suspend fun readBinary(
        params: ReadBinaryParams
    ): Result {
        if (!isAccessible
                || !openChannel(params.fileId.aid)
                || !select(params.fileId.path, params.fileId.fileId).isOk) {
            startClosingTimer()
            return Result.Builder(params.fileId.fileId)
                    .data(DATA_NONE)
                    .sw(SW_INTERNAL_EXCEPTION)
                    .build()
        }

        val response = readBinary(params.offset, params.size)
        startClosingTimer()

        return Result.Builder(params.fileId.fileId)
                .data(response.data)
                .sw(response.sw)
                .build()
    }

    suspend fun readRecord(
        params: ReadRecordParams
    ): Result {
        if (!isAccessible
                || !openChannel(params.fileId.aid)
                || !select(params.fileId.path, params.fileId.fileId).isOk) {
            startClosingTimer()
            return Result.Builder(params.fileId.fileId)
                    .data(DATA_NONE)
                    .sw(SW_INTERNAL_EXCEPTION)
                    .build()
        }

        val command = Command.Builder(Iso7816.INS_READ_RECORD)
                .p1(params.recordNo)
                .p2(0x04 /* Absolute/current mode, the record number is given in P1 */)
                .le(params.recordSize)
                .build()
        val response = cardIo.transmit(command)
        startClosingTimer()

        return Result.Builder(params.fileId.fileId)
                .data(response.data)
                .sw(response.sw)
                .build()
    }

    suspend fun readAllRecords(
        params: ReadAllRecordsParams
    ): List<Result> {
        if (!isAccessible
                || (params.numberOfRecords < 1) || (params.numberOfRecords > 254)
                || !openChannel(params.fileId.aid)
                || !select(params.fileId.path, params.fileId.fileId).isOk) {
            startClosingTimer()
            val result = Result.Builder(params.fileId.fileId)
                    .data(DATA_NONE)
                    .sw(SW_INTERNAL_EXCEPTION)
                    .build()
            return mutableListOf(result)
        }

        val records: MutableList<Result> = mutableListOf()
        for (recordNo in IntRange(1, params.numberOfRecords)) {
            val command = Command.Builder(Iso7816.INS_READ_RECORD)
                    .p1(recordNo)
                    .p2(0x04 /* Absolute/current mode, the record number is given in P1 */)
                    .le(params.recordSize)
                    .build()
            val response = cardIo.transmit(command)
            val result = Result.Builder(params.fileId.fileId)
                    .data(response.data)
                    .sw(response.sw)
                    .build()
            records.add(result)
        }
        startClosingTimer()
        return records
    }

    private suspend fun openChannel(
        aid: String = FileId.AID_NONE
    ): Boolean {
        cancelClosingTimer()
        return cardIo.openChannel(hexStringToByteArray(aid)) == Interface.OpenChannelResult.SUCCESS
    }

    private fun select(
        path: String = FileId.PATH_MF,
        fileId: String,
        fcpRequest: Boolean = false
    ): Response {
        val command = Command.Builder(Iso7816.INS_SELECT_FILE)
                .p1(0x08 /* Select by path from MF */)
                .p2(if (fcpRequest) 0x04 /* Return FCP template */ else 0x0C /* No data returned */)
                .data(hexStringToByteArray(path + fileId))
                .build()
        return cardIo.transmit(command)
    }

    private fun readBinary(
        offset: Int = 0,
        size: Int = SIZE_MAX
    ): Response {
        val command = Command.Builder(Iso7816.INS_READ_BINARY)
                .p1(offset.and(0x7F00).shr(8))
                .p2(offset.and(0x00FF))
                .le(size)
                .build()
        return cardIo.transmit(command)
    }

    private fun startClosingTimer() {
        synchronized(this) {
            closingJob = GlobalScope.launch {
                delay(CLOSING_TIMER_MILLIS)
                cardIo.closeRemainingChannel()
                closingJob = null
            }
            Log.d(tag, "Started a job for releasing the logical channel")
        }
    }

    private suspend fun cancelClosingTimer() {
        var controller: Job? = null
        synchronized(this) {
            if (closingJob != null) {
                closingJob!!.cancel()
                controller = closingJob
                closingJob = null
                Log.d(tag, "Canceled the launched job")
            }
        }
        controller?.join()
    }

    suspend fun dispose() {
        cancelClosingTimer()
        _iccId = null
        cardIo.dispose()
        Log.d(tag, "Disposed")
    }
}
