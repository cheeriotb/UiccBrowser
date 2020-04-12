/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.repository

import android.content.Context
import android.util.Log
import com.github.cheeriotb.uiccbrowser.cacheio.CachedSubscription
import com.github.cheeriotb.uiccbrowser.cacheio.CachedSubscriptionDataSource
import com.github.cheeriotb.uiccbrowser.cacheio.CachedSubscriptionDataSourceImpl
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
    private val slotId: Int,
    private val cardIo: Interface,
    private val cacheIo: SelectResponseDataSource,
    private val subscriptionIo: CachedSubscriptionDataSource
) {
    private val tag = TelephonyInterface::class.java.simpleName + slotId

    private var lastSelectedAdf: String? = null
    private var iccId: String? = null

    private var closingJob: Job? = null

    val isAccessible: Boolean
        get() = (iccId != null)

    var isCached: Boolean = false

    companion object {
        @Volatile
        private var instances: ArrayList<CardRepository>? = null

        fun from(context: Context, slotId: Int): CardRepository? {
            synchronized(this) {
                if (instances == null) {
                    instances = ArrayList()
                    val cacheIo = SelectResponseDataSourceImpl.from(context)
                    val subscriptionIo = CachedSubscriptionDataSourceImpl.from(context)
                    var testSlotId = 0
                    do {
                        val cardIo: Interface = TelephonyInterface.from(context, testSlotId)
                        if (!cardIo.isAvailable) break
                        instances!!.add(CardRepository(slotId, cardIo, cacheIo, subscriptionIo))
                        testSlotId++
                    } while (true)
                }
            }

            return if (instances!!.size > slotId) instances!![slotId] else null
        }

        const val AID_NONE = ""
        const val LEVEL_MF = ""
        const val SIZE_MAX = 0x100

        const val EF_ICCID = "2FE2"
        const val SW_INTERNAL_EXCEPTION = Iso7816.SW1_INTERNAL_EXCEPTION shl 8
    }

    suspend fun initialize(): Boolean {
        if (openChannel()) {
            val selectResponse = select(LEVEL_MF, EF_ICCID, fcpRequest = true)
            if (selectResponse.isOk) {
                val readResponse = readBinary()
                if (readResponse.isOk && (readResponse.data.size == 10)) {
                    // Refer to the clause 13.2 of ETSI TS 102 221 for the format of ICCID
                    val builder = StringBuilder()
                    for (byte in readResponse.data) {
                        val hexString = byteToHexString(byte.toInt())
                        withContext(Dispatchers.IO) {
                            builder.append(hexString[1]).append(hexString[0])
                        }
                    }
                    iccId = builder.toString()
                    Log.d(tag, "ICCID: $iccId")

                    val subscription: CachedSubscription? = subscriptionIo.get(iccId!!)
                    isCached = (subscription != null)
                    if (!isCached) {
                        // Delete all incomplete cache for this ICCID, if exists
                        cacheIo.delete(iccId!!)
                        // Cache the FCP template for this ICCID
                        cacheIo.insert(SelectResponse(iccId!!, AID_NONE, LEVEL_MF, EF_ICCID,
                                selectResponse.data, selectResponse.sw))
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
        aid: String = AID_NONE,
        path: String = LEVEL_MF,
        fileId: String
    ): Boolean {
        if (!isAccessible || isCached || !openChannel(aid)) {
            return false // means that the caching process cannot be continued.
        }

        val response = select(path, fileId, fcpRequest = true)
        startClosingTimer()

        if (response.data.isNotEmpty()) {
            cacheIo.insert(SelectResponse(iccId!!, aid, path, fileId, response.data, response.sw))
        }

        return true // means that the caching process can be continued.
    }

    suspend fun finalizeCache(name: String) {
        if (isAccessible && !isCached) {
            subscriptionIo.insert(CachedSubscription(iccId!!, name))
            isCached = true
            Log.i(tag, "The cache for $iccId was finalized")
        }
    }

    suspend fun queryFileControlParameters(
        aid: String = AID_NONE,
        path: String = LEVEL_MF
    ): List<Result> {
        if (!isAccessible || !isCached) return emptyList()
        return Result.listFrom(cacheIo.getAll(iccId!!, aid, path))
    }

    suspend fun readBinary(
        aid: String = AID_NONE,
        path: String = LEVEL_MF,
        fileId: String,
        offset: Int = 0,
        size: Int = SIZE_MAX
    ): Result {
        if (!isAccessible || !openChannel(aid) || !select(path, fileId).isOk) {
            startClosingTimer()
            return Result(fileId, ByteArray(0), SW_INTERNAL_EXCEPTION)
        }

        val response = readBinary(offset, size)
        startClosingTimer()

        return Result(fileId, response.data, response.sw)
    }

    suspend fun readRecord(
        aid: String = AID_NONE,
        path: String = LEVEL_MF,
        fileId: String,
        recordNo: Int,
        size: Int = SIZE_MAX
    ): Result {
        if (!isAccessible || !openChannel(aid) || !select(path, fileId).isOk) {
            startClosingTimer()
            return Result(fileId, ByteArray(0), SW_INTERNAL_EXCEPTION)
        }

        val response = cardIo.transmit(Command(Iso7816.INS_READ_RECORD, recordNo,
                0x04 /* Absolute/current mode, the record number is given in P1 */, size))
        startClosingTimer()

        return Result(fileId, response.data, response.sw)
    }

    private suspend fun openChannel(aid: String = AID_NONE): Boolean {
        val aidArray = if (aid.isNotEmpty()) hexStringToByteArray(aid)
                else hexStringToByteArray(lastSelectedAdf ?: AID_NONE)

        cancelClosingTimer()

        return when (cardIo.openChannel(aidArray)) {
            Interface.OpenChannelResult.MISSING_RESOURCE -> {
                lastSelectedAdf = null
                cardIo.transmit(Command(Iso7816.INS_SELECT_FILE, 0x04 /* Selection by DF name */,
                        0x0C /* No data returned*/, hexStringToByteArray(aid))).isOk
            }
            Interface.OpenChannelResult.SUCCESS -> {
                if (aid.isNotEmpty()) lastSelectedAdf = aid
                true
            }
            else -> false
        }
    }

    private fun select(
        path: String = LEVEL_MF,
        fileId: String,
        fcpRequest: Boolean = false
    ): Response {
        val p2 = if (fcpRequest) 0x04 /* Return FCP template */ else 0x0C /* No data returned */
        return cardIo.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08 /* Select by path from MF */,
                p2, hexStringToByteArray(path + fileId)))
    }

    private fun readBinary(offset: Int = 0, size: Int = SIZE_MAX): Response {
        val p1 = offset.and(0x7F00).shr(8)
        val p2 = offset.and(0x00FF)
        return cardIo.transmit(Command(Iso7816.INS_READ_BINARY, p1, p2, size))
    }

    private suspend fun startClosingTimer() {
        if (lastSelectedAdf != null) {
            synchronized(this) {
                closingJob = GlobalScope.launch {
                    delay(500)
                    cardIo.closeRemainingChannel()
                }
                Log.d(tag, "Started a job for releasing the logical channel")
            }
        }
    }

    private suspend fun cancelClosingTimer() {
        var controller: Job? = null
        synchronized(this) {
            if (closingJob != null) {
                closingJob!!.cancel()
                controller = closingJob
                closingJob = null
                Log.d(tag, "Canceled a job for releasing the logical channel")
            }
        }
        controller?.join()
    }

    fun dispose() {
        cardIo.dispose()
        Log.d(tag, "Disposed")
    }
}
