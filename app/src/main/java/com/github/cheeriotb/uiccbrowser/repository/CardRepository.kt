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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CardRepository private constructor (
    slotId: Int,
    private val cardIo: Interface,
    private val cacheIo: SelectResponseDataSource
) {
    private val tag = TelephonyInterface::class.java.simpleName + slotId
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var _iccId: String? = null
    private var closingJob: Job? = null
    private var isLogicalChannelRetained = false
    private val verifiedPinKeyReferences = mutableSetOf<KeyReference>()
    private val trustedPinKeyReferencesForNextAccess = mutableSetOf<KeyReference>()

    val iccId: String? get() = _iccId
    var isProModeEnabled: Boolean = false

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

        private fun clearInstances() {
            synchronized(this) {
                instances = null
            }
        }

        const val SIZE_MAX = 0x100

        const val SW_INTERNAL_EXCEPTION = Iso7816.SW1_INTERNAL_EXCEPTION shl 8
        val DATA_NONE = ByteArray(0)

        private const val CLOSING_TIMER_MILLIS = 500L
        private const val VERIFY_PIN_CODE_SIZE = 8
        private const val FID_MF = "3F00"
    }

    suspend fun initialize(): Boolean {
        _iccId = null
        isLogicalChannelRetained = false
        clearVerifiedPins()

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
            } else {
                startClosingTimer()
            }
        }

        Log.i(tag, "The card associated with the slot is not accessible")
        return false
    }

    suspend fun cacheFileControlParameters(
        fileId: FileId
    ): Boolean {
        if (!isAccessible) {
            return false // means that the caching process cannot be continued.
        }
        if (cacheIo.get(_iccId!!, fileId.aid, fileId.path, fileId.fileId) != null) {
            return true // No need to cache it again
        }
        if (!openChannel(fileId.aid)) {
            return false // means that the caching process cannot be continued.
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

    /**
     * Selects an MF, DF, or ADF by MF-relative path and returns its FCP template.
     *
     * This directory-only API is separate from EF FCP caching. [directory] must not identify an EF.
     */
    suspend fun readDirectoryFileControlParameters(directory: FileId): Result {
        require(directory.fileId == FileId.FID_ALMIGHTY) {
            "Directory FCP requests must not specify an EF file ID"
        }
        if (!isAccessible || !openChannel(directory.aid)) {
            startClosingTimer()
            return Result.Builder(directory.path)
                    .data(DATA_NONE)
                    .sw(SW_INTERNAL_EXCEPTION)
                    .build()
        }

        val command = Command.Builder(Iso7816.INS_SELECT_FILE)
                .p1(if (directory.path == FileId.PATH_MF) 0x00 else 0x08)
                .p2(0x04 /* Return FCP template */)
                .data(hexStringToByteArray(
                        if (directory.path == FileId.PATH_MF) FID_MF else directory.path))
                .build()
        val response = cardIo.transmit(command)
        startClosingTimer()
        return Result.Builder(directory.path)
                .data(response.data)
                .sw(response.sw)
                .build()
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

        val command = Command.Builder(Iso7816.INS_READ_BINARY)
                .p1(params.offset.and(0x7F00).shr(8))
                .p2(params.offset.and(0x00FF))
                .le(params.size)
                .build()
        val response = transmitReadWriteCommand(command)
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
        val response = transmitReadWriteCommand(command)
        startClosingTimer()

        return Result.Builder(params.fileId.fileId)
                .data(response.data)
                .sw(response.sw)
                .build()
    }

    suspend fun updateBinary(
        params: UpdateBinaryParams
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

        val command = Command.Builder(Iso7816.INS_UPDATE_BINARY)
                .p1(params.offset.and(0x7F00).shr(8))
                .p2(params.offset.and(0x00FF))
                .data(params.data)
                .build()
        val response = transmitReadWriteCommand(command)
        startClosingTimer()

        return Result.Builder(params.fileId.fileId)
                .data(response.data)
                .sw(response.sw)
                .build()
    }

    suspend fun updateRecord(
        params: UpdateRecordParams
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

        val command = Command.Builder(Iso7816.INS_UPDATE_RECORD)
                .p1(params.recordNo)
                .p2(0x04 /* Absolute/current mode, the record number is given in P1 */)
                .data(params.data)
                .build()
        val response = transmitReadWriteCommand(command)
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
            val response = transmitReadWriteCommand(command)
            val result = Result.Builder(params.fileId.fileId)
                    .data(response.data)
                    .sw(response.sw)
                    .build()
            records.add(result)
        }
        startClosingTimer()
        return records
    }

    suspend fun queryVerifyPinRetries(
        keyReference: KeyReference,
        aid: String = FileId.AID_NONE
    ): Response {
        if (!isAccessible || !openChannel(aid)) {
            startClosingTimer()
            return internalExceptionResponse()
        }

        val response = verifyPin(keyReference)
        startClosingTimer()

        return response
    }

    suspend fun verifyPin(
        keyReference: KeyReference,
        code: String,
        aid: String = FileId.AID_NONE
    ): Response {
        val paddedCode = padVerifyPinCode(code)
        if (!isAccessible || !openChannel(aid)) {
            startClosingTimer()
            return internalExceptionResponse()
        }

        val response = verifyPin(keyReference, paddedCode)
        if (response.isOk) {
            rememberVerifiedPin(keyReference)
            retainLogicalChannel()
        } else {
            startClosingTimer()
        }

        return response
    }

    fun releaseLogicalChannel() {
        synchronized(this) {
            isLogicalChannelRetained = false
            clearVerifiedPins()
        }
        cancelClosingTimerJob()
        cardIo.closeRemainingChannel()
    }

    /** Returns true when this repository has observed a successful VERIFY for [keyReference]. */
    fun isPinVerified(keyReference: KeyReference): Boolean =
        synchronized(this) { keyReference in verifiedPinKeyReferences }

    /**
     * Marks remembered PINs that were trusted instead of VERIFY for the next READ/UPDATE command.
     *
     * If that command fails with SW6982, only these PINs are removed from the remembered set.
     */
    fun markVerifiedPinsTrustedForNextAccess(keyReferences: Collection<KeyReference>) {
        synchronized(this) {
            trustedPinKeyReferencesForNextAccess.clear()
            trustedPinKeyReferencesForNextAccess.addAll(keyReferences)
        }
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
        val isMf = path == FileId.PATH_MF && fileId == FileId.MF
        val command = Command.Builder(Iso7816.INS_SELECT_FILE)
                .p1(if (isMf) 0x00 /* Select by file ID */ else 0x08)
                .p2(if (fcpRequest) 0x04 /* Return FCP template */ else 0x0C /* No data returned */)
                .data(hexStringToByteArray(if (isMf) FID_MF else path + fileId))
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

    private fun transmitReadWriteCommand(command: Command): Response {
        val response = cardIo.transmit(command)
        updateVerifiedPinsForReadWriteResponse(response.sw)
        return response
    }

    private fun verifyPin(
        keyReference: KeyReference,
        data: ByteArray = DATA_NONE
    ): Response {
        val command = Command.Builder(Iso7816.INS_VERIFY_PIN)
                .p1(0x00)
                .p2(keyReference.value)
                .data(data)
                .build()
        return cardIo.transmit(command)
    }

    private fun padVerifyPinCode(code: String): ByteArray {
        require(code.length % 2 == 0) { "PIN/ADM code must contain an even number of hex digits" }
        require(code.matches(Regex("[0-9A-Fa-f]*"))) { "PIN/ADM code must be a hex string" }

        val codeLength = code.length / 2
        require(codeLength in 1..VERIFY_PIN_CODE_SIZE) {
            "PIN/ADM code must be between 1 and 8 bytes"
        }

        val paddedCode = ByteArray(VERIFY_PIN_CODE_SIZE) { 0xFF.toByte() }
        hexStringToByteArray(code).copyInto(paddedCode)
        return paddedCode
    }

    private fun internalExceptionResponse() =
            Response(byteArrayOf(
                    SW_INTERNAL_EXCEPTION.shr(8).toByte(),
                    SW_INTERNAL_EXCEPTION.toByte()))

    private fun startClosingTimer() {
        synchronized(this) {
            if (isLogicalChannelRetained) {
                Log.d(tag, "Skipped releasing the retained logical channel")
                return
            }
            closingJob = repositoryScope.launch {
                delay(CLOSING_TIMER_MILLIS)
                cardIo.closeRemainingChannel()
                closingJob = null
            }
            Log.d(tag, "Started a job for releasing the logical channel")
        }
    }

    private fun retainLogicalChannel() {
        synchronized(this) {
            isLogicalChannelRetained = true
            Log.d(tag, "Retained the logical channel")
        }
    }

    private fun rememberVerifiedPin(keyReference: KeyReference) {
        synchronized(this) {
            verifiedPinKeyReferences.add(keyReference)
            Log.d(tag, "Remembered a successful VERIFY result")
        }
    }

    private fun clearVerifiedPins() {
        synchronized(this) {
            verifiedPinKeyReferences.clear()
            trustedPinKeyReferencesForNextAccess.clear()
        }
    }

    private fun updateVerifiedPinsForReadWriteResponse(sw: Int) {
        synchronized(this) {
            if (sw == Result.SW_INSUFFICIENT_SECURITY) {
                verifiedPinKeyReferences.removeAll(trustedPinKeyReferencesForNextAccess)
            }
            trustedPinKeyReferencesForNextAccess.clear()
        }
    }

    private fun cancelClosingTimerJob(): Job? {
        synchronized(this) {
            val controller = closingJob ?: return null
            controller.cancel()
            closingJob = null
            Log.d(tag, "Canceled the launched job")
            return controller
        }
    }

    private suspend fun cancelClosingTimer() {
        val controller = cancelClosingTimerJob()
        controller?.join()
    }

    suspend fun dispose() {
        isLogicalChannelRetained = false
        clearVerifiedPins()
        cancelClosingTimer()
        repositoryScope.cancel()
        _iccId = null
        cardIo.dispose()
        clearInstances()
        Log.d(tag, "Disposed")
    }
}
