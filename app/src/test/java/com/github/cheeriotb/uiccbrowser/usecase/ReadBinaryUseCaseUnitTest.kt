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
class ReadBinaryUseCaseUnitTest {

    @Rule @JvmField
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.READ_PHONE_STATE)

    private val cardIoMock = mockk<Interface>()
    private val cacheIoMock = mockk<SelectResponseDataSource>()

    private lateinit var repository: CardRepository
    private lateinit var useCase: ReadBinaryUseCase

    companion object {
        // Transparent EF (EF ICCID): FD byte = 0x41 (bits 2-0 = 001 = Transparent), file size = 10
        private const val FCP_TRANSPARENT =
            "621E8202412183022FE2A506C00100CA01808A01058B032F06048002000A8800"
        private const val ICCID_RAW = "988812010000202801F6"
        private const val ICCID = "8988211000000282106F"

        // Linear Fixed EF: FD byte = 0x42 (bits 2-0 = 010 = Linear Fixed), 2 records of 10 bytes
        private const val FCP_LINEAR_FIXED = "620F82054221000A028002001483022F02"

        // Transparent EF with file size = 260 (0x0104) for multi-chunk read test
        // 62 0C 82 02 41 21 80 02 01 04 83 02 2F E3
        private const val FCP_TRANSPARENT_260 = "620C820241218002010483022FE3"
        private const val EF_260 = "2FE3"

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

        useCase = ReadBinaryUseCase(ApplicationProvider.getApplicationContext())
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
    fun execute_transparentEf_returnsData() = runBlocking {
        initializeRepo()

        val fileId = FileId(FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns
            SelectResponse(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID,
                hexStringToByteArray(FCP_TRANSPARENT), Result.SW_NORMAL)

        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
            0x08, 0x0C, hexStringToByteArray(FileId.EF_ICCID))) } returns
            Response(hexStringToByteArray(SW_OK))
        every { cardIoMock.transmit(Command(Iso7816.INS_READ_BINARY, 0x00, 0x00, 0x0A)) } returns
            Response(hexStringToByteArray(ICCID_RAW + SW_OK))

        val result = useCase.execute(0, fileId)

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(hexStringToByteArray(ICCID_RAW))
    }

    @Test
    fun execute_transparentEf_multipleChunks_returnsAllData() = runBlocking {
        initializeRepo()

        val fileId = FileId(FileId.AID_NONE, FileId.PATH_MF, EF_260)
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, EF_260) } returns
            SelectResponse(ICCID, FileId.AID_NONE, FileId.PATH_MF, EF_260,
                hexStringToByteArray(FCP_TRANSPARENT_260), Result.SW_NORMAL)

        val chunk1 = ByteArray(256) { it.toByte() }
        val chunk2 = ByteArray(4) { (it + 256).toByte() }

        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
            0x08, 0x0C, hexStringToByteArray(EF_260))) } returns
            Response(hexStringToByteArray(SW_OK))
        every { cardIoMock.transmit(Command(Iso7816.INS_READ_BINARY, 0x00, 0x00, 0x100)) } returns
            Response(chunk1 + hexStringToByteArray(SW_OK))
        every { cardIoMock.transmit(Command(Iso7816.INS_READ_BINARY, 0x01, 0x00, 0x04)) } returns
            Response(chunk2 + hexStringToByteArray(SW_OK))

        val result = useCase.execute(0, fileId)

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(chunk1 + chunk2)
    }

    @Test
    fun execute_linearFixedEf_returnsNull() = runBlocking {
        initializeRepo()

        val fileId = FileId(FileId.AID_NONE, FileId.PATH_MF, "2F02")
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, "2F02") } returns
            SelectResponse(ICCID, FileId.AID_NONE, FileId.PATH_MF, "2F02",
                hexStringToByteArray(FCP_LINEAR_FIXED), Result.SW_NORMAL)

        val result = useCase.execute(0, fileId)

        assertThat(result).isNull()
    }

    @Test
    fun execute_fcpNotCached_returnsNull() = runBlocking {
        initializeRepo()

        val fileId = FileId(FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns
            SelectResponse(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID,
                ByteArray(0), Result.SW_NOT_FOUND)

        val result = useCase.execute(0, fileId)

        assertThat(result).isNull()
    }

    @Test
    fun execute_readBinaryFails_returnsNull() = runBlocking {
        initializeRepo()

        val fileId = FileId(FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns
            SelectResponse(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID,
                hexStringToByteArray(FCP_TRANSPARENT), Result.SW_NORMAL)

        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
            0x08, 0x0C, hexStringToByteArray(FileId.EF_ICCID))) } returns
            Response(hexStringToByteArray(SW_OK))
        every { cardIoMock.transmit(Command(Iso7816.INS_READ_BINARY, 0x00, 0x00, 0x0A)) } returns
            Response(hexStringToByteArray(SW_FAIL))

        val result = useCase.execute(0, fileId)

        assertThat(result).isNull()
    }
}
