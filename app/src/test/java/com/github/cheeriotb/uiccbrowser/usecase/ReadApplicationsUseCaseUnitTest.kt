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
class ReadApplicationsUseCaseUnitTest {

    @Rule @JvmField
    val grantPermissionRule: GrantPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.READ_PHONE_STATE)

    private val cardIoMock = mockk<Interface>()
    private val cacheIoMock = mockk<SelectResponseDataSource>()

    private lateinit var repository: CardRepository
    private lateinit var useCase: ReadApplicationsUseCase

    companion object {
        // FCP for EF DIR: linear fixed EF, record size 0x1B=27, 2 records
        //   62 0B 82 05 42 21 00 1B 02 83 02 2F 00
        private const val FCP_DIR = "620B8205422100" + "1B02" + "83022F00"

        // FCP for EF ICCID (transparent EF) — re-used for the initialize() call
        private const val FCP_ICCID = "621E8202412183022FE2A506C00100CA01808A01058B032F06048002000A8800"

        private const val ICCID_RAW = "988812010000202801F6"
        private const val ICCID = "8988211000000282106F"

        // USIM AID: A0 00 00 00 87 10 02 FF FF FF FF FF FF FF FF FF (16 bytes)
        private const val USIM_AID = "A0000000871002FFFFFFFFFFFFFFFF"
        // ISIM AID: A0 00 00 00 87 10 04 FF FF FF (10 bytes)
        private const val ISIM_AID = "A0000000871004FFFFFFFFFF"

        // Build a 27-byte DIR record containing a USIM Application template + 0xFF padding
        // 61 12 4F 10 <USIM_AID>  → 20 bytes + 7 bytes of FF = 27 bytes
        private val RECORD_USIM: ByteArray = run {
            val aid = hexStringToByteArray(USIM_AID)
            val value = byteArrayOf(0x4F, aid.size.toByte()) + aid
            byteArrayOf(0x61, value.size.toByte()) + value +
                    ByteArray(27 - 2 - value.size) { 0xFF.toByte() }
        }

        // Build a 27-byte DIR record containing an ISIM Application template + 0xFF padding
        // 61 0C 4F 0A <ISIM_AID>  → 14 bytes + 13 bytes of FF = 27 bytes
        private val RECORD_ISIM: ByteArray = run {
            val aid = hexStringToByteArray(ISIM_AID)
            val value = byteArrayOf(0x4F, aid.size.toByte()) + aid
            byteArrayOf(0x61, value.size.toByte()) + value +
                    ByteArray(27 - 2 - value.size) { 0xFF.toByte() }
        }

        // All-0xFF record (unused slot)
        private val RECORD_UNUSED = ByteArray(27) { 0xFF.toByte() }
    }

    @Before
    fun setUp() {
        every { cardIoMock.isAvailable } returns true
        every { cardIoMock.openChannel(Interface.NO_AID_SPECIFIED) } returns
                Interface.OpenChannelResult.SUCCESS
        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
                0x08, 0x04, hexStringToByteArray(FileId.EF_ICCID))) } returns
                Response(hexStringToByteArray(FCP_ICCID + "%04X".format(Result.SW_NORMAL)))
        every { cardIoMock.transmit(Command(Iso7816.INS_READ_BINARY, 0x00, 0x00, 0x100)) } returns
                Response(hexStringToByteArray(ICCID_RAW + "%04X".format(Result.SW_NORMAL)))
        every { cardIoMock.closeRemainingChannel() } answers { nothing }
        every { cardIoMock.dispose() } answers { nothing }

        val target = CardRepository.from(ApplicationProvider.getApplicationContext(), 0)
        assertThat(target).isNotNull()
        repository = target!!
        ReflectionHelpers.setField(repository, "cardIo", cardIoMock)
        ReflectionHelpers.setField(repository, "cacheIo", cacheIoMock)

        useCase = ReadApplicationsUseCase(ApplicationProvider.getApplicationContext())
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
    fun execute_twoApplications_returnsBothAids() = runBlocking {
        initializeRepo()

        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_DIR) } returns
                SelectResponse(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_DIR,
                        hexStringToByteArray(FCP_DIR), Result.SW_NORMAL)

        every { cardIoMock.openChannel(Interface.NO_AID_SPECIFIED) } returns
                Interface.OpenChannelResult.SUCCESS
        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
                0x08, 0x0C, hexStringToByteArray(FileId.EF_DIR))) } returns
                Response(hexStringToByteArray("%04X".format(Result.SW_NORMAL)))
        every { cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x01, 0x04, 0x1B)) } returns
                Response(RECORD_USIM + hexStringToByteArray("%04X".format(Result.SW_NORMAL)))
        every { cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x02, 0x04, 0x1B)) } returns
                Response(RECORD_ISIM + hexStringToByteArray("%04X".format(Result.SW_NORMAL)))

        val aids = useCase.execute(0)

        assertThat(aids).hasSize(2)
        assertThat(aids[0]).isEqualTo(USIM_AID)
        assertThat(aids[1]).isEqualTo(ISIM_AID)
    }

    @Test
    fun execute_dirNotCached_returnsEmpty() = runBlocking {
        initializeRepo()

        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_DIR) } returns
                SelectResponse(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_DIR,
                        ByteArray(0), Result.SW_NOT_FOUND)

        val aids = useCase.execute(0)

        assertThat(aids).isEmpty()
    }

    @Test
    fun execute_unusedRecord_skipped() = runBlocking {
        initializeRepo()

        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_DIR) } returns
                SelectResponse(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_DIR,
                        hexStringToByteArray(FCP_DIR), Result.SW_NORMAL)

        every { cardIoMock.openChannel(Interface.NO_AID_SPECIFIED) } returns
                Interface.OpenChannelResult.SUCCESS
        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
                0x08, 0x0C, hexStringToByteArray(FileId.EF_DIR))) } returns
                Response(hexStringToByteArray("%04X".format(Result.SW_NORMAL)))
        every { cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x01, 0x04, 0x1B)) } returns
                Response(RECORD_USIM + hexStringToByteArray("%04X".format(Result.SW_NORMAL)))
        every { cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x02, 0x04, 0x1B)) } returns
                Response(RECORD_UNUSED + hexStringToByteArray("%04X".format(Result.SW_NORMAL)))

        val aids = useCase.execute(0)

        assertThat(aids).hasSize(1)
        assertThat(aids[0]).isEqualTo(USIM_AID)
    }
}
