/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.usecase

import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import com.github.cheeriotb.uiccbrowser.cacheio.SelectResponse
import com.github.cheeriotb.uiccbrowser.cacheio.SelectResponseDataSource
import com.github.cheeriotb.uiccbrowser.cardio.Command
import com.github.cheeriotb.uiccbrowser.cardio.Interface
import com.github.cheeriotb.uiccbrowser.cardio.Iso7816
import com.github.cheeriotb.uiccbrowser.cardio.Response
import com.github.cheeriotb.uiccbrowser.repository.CardRepository
import com.github.cheeriotb.uiccbrowser.repository.FileId
import com.github.cheeriotb.uiccbrowser.repository.Result
import com.github.cheeriotb.uiccbrowser.util.hexStringToByteArray
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class ReadRecordUseCaseUnitTest {

    @Rule @JvmField
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.READ_PHONE_STATE)

    private val cardIoMock = mockk<Interface>()
    private val cacheIoMock = mockk<SelectResponseDataSource>()

    private lateinit var repository: CardRepository
    private lateinit var useCase: ReadRecordUseCase

    companion object {
        // Transparent EF (EF ICCID): used only for initialize()
        private const val FCP_TRANSPARENT =
            "621E8202412183022FE2A506C00100CA01808A01058B032F06048002000A8800"
        private const val ICCID_RAW = "988812010000202801F6"
        private const val ICCID = "8988211000000282106F"

        // Linear Fixed EF (2F02): FD byte = 0x42, 2 records of 10 bytes
        private const val FCP_LINEAR_FIXED = "620F82054221000A028002001483022F02"
        private const val EF_LF = "2F02"

        private val SW_OK = "%04X".format(Result.SW_NORMAL)
        private val SW_FAIL = "%04X".format(0x6A82)
    }

    @Before
    fun setUp() {
        every { cardIoMock.isAvailable } returns true
        every { cardIoMock.openChannel(Interface.NO_AID_SPECIFIED) } returns
            Interface.OpenChannelResult.SUCCESS
        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
            0x08, 0x04, hexStringToByteArray(FileId.EF_ICCID))) } returns
            Response(hexStringToByteArray(FCP_TRANSPARENT + SW_OK))
        every { cardIoMock.transmit(Command(Iso7816.INS_READ_BINARY, 0x00, 0x00, 0x100)) } returns
            Response(hexStringToByteArray(ICCID_RAW + SW_OK))
        every { cardIoMock.closeRemainingChannel() } answers { nothing }
        every { cardIoMock.dispose() } answers { nothing }

        val target = CardRepository.from(ApplicationProvider.getApplicationContext(), 0)
        assertThat(target).isNotNull()
        repository = target!!
        ReflectionHelpers.setField(repository, "cardIo", cardIoMock)
        ReflectionHelpers.setField(repository, "cacheIo", cacheIoMock)

        useCase = ReadRecordUseCase(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() = runBlocking {
        if (::repository.isInitialized) repository.dispose()
    }

    private suspend fun initializeRepo() {
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()
    }

    @Test
    fun getInfo_linearFixedEf_returnsCorrectInfo() = runBlocking {
        initializeRepo()

        val fileId = FileId(FileId.AID_NONE, FileId.PATH_MF, EF_LF)
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, EF_LF) } returns
            SelectResponse(ICCID, FileId.AID_NONE, FileId.PATH_MF, EF_LF,
                hexStringToByteArray(FCP_LINEAR_FIXED), Result.SW_NORMAL)

        val info = useCase.getInfo(0, fileId)

        assertThat(info).isNotNull()
        assertThat(info!!.recordLength).isEqualTo(10)
        assertThat(info.numberOfRecords).isEqualTo(2)
    }

    @Test
    fun execute_linearFixedEf_firstRecord_returnsData() = runBlocking {
        initializeRepo()

        val fileId = FileId(FileId.AID_NONE, FileId.PATH_MF, EF_LF)
        val record1Data = ByteArray(10) { it.toByte() }

        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
            0x08, 0x0C, hexStringToByteArray(EF_LF))) } returns
            Response(hexStringToByteArray(SW_OK))
        every { cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x01, 0x04, 0x0A)) } returns
            Response(record1Data + hexStringToByteArray(SW_OK))

        val result = useCase.execute(0, fileId, 1, 10)

        assertThat(result).isEqualTo(record1Data)
    }

    @Test
    fun execute_linearFixedEf_secondRecord_returnsData() = runBlocking {
        initializeRepo()

        val fileId = FileId(FileId.AID_NONE, FileId.PATH_MF, EF_LF)
        val record2Data = ByteArray(10) { (it + 10).toByte() }

        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
            0x08, 0x0C, hexStringToByteArray(EF_LF))) } returns
            Response(hexStringToByteArray(SW_OK))
        every { cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x02, 0x04, 0x0A)) } returns
            Response(record2Data + hexStringToByteArray(SW_OK))

        val result = useCase.execute(0, fileId, 2, 10)

        assertThat(result).isEqualTo(record2Data)
    }

    @Test
    fun getInfo_transparentEf_returnsNull() = runBlocking {
        initializeRepo()

        val fileId = FileId(FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns
            SelectResponse(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID,
                hexStringToByteArray(FCP_TRANSPARENT), Result.SW_NORMAL)

        val info = useCase.getInfo(0, fileId)

        assertThat(info).isNull()
    }

    @Test
    fun getInfo_fcpNotCached_returnsNull() = runBlocking {
        initializeRepo()

        val fileId = FileId(FileId.AID_NONE, FileId.PATH_MF, EF_LF)
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, EF_LF) } returns
            SelectResponse(ICCID, FileId.AID_NONE, FileId.PATH_MF, EF_LF,
                ByteArray(0), Result.SW_NOT_FOUND)

        val info = useCase.getInfo(0, fileId)

        assertThat(info).isNull()
    }

    @Test
    fun execute_readRecordFails_returnsNull() = runBlocking {
        initializeRepo()

        val fileId = FileId(FileId.AID_NONE, FileId.PATH_MF, EF_LF)

        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
            0x08, 0x0C, hexStringToByteArray(EF_LF))) } returns
            Response(hexStringToByteArray(SW_OK))
        every { cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x01, 0x04, 0x0A)) } returns
            Response(hexStringToByteArray(SW_FAIL))

        val result = useCase.execute(0, fileId, 1, 10)

        assertThat(result).isNull()
    }
}
