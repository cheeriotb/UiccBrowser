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
import com.github.cheeriotb.uiccbrowser.R
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
class GetFileListUseCaseUnitTest {

    @Rule @JvmField
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.READ_PHONE_STATE)

    private val cardIoMock = mockk<Interface>()
    private val cacheIoMock = mockk<SelectResponseDataSource>()

    private lateinit var repository: CardRepository
    private lateinit var useCase: GetFileListUseCase

    companion object {
        private const val FCP_ICCID =
            "621E8202412183022FE2A506C00100CA01808A01058B032F06048002000A8800"
        private const val ICCID_RAW = "988812010000202801F6"
        private const val ICCID = "8988211000000282106F"

        private val FCP_OK = hexStringToByteArray("6200") + byteArrayOf(
            (Result.SW_NORMAL shr 8).toByte(),
            (Result.SW_NORMAL and 0xFF).toByte()
        )
        private val FCP_NOT_FOUND = ByteArray(0)
    }

    @Before
    fun setUp() {
        every { cardIoMock.isAvailable } returns true
        every { cardIoMock.openChannel(Interface.NO_AID_SPECIFIED) } returns
                Interface.OpenChannelResult.SUCCESS
        every {
            cardIoMock.transmit(
                Command(Iso7816.INS_SELECT_FILE, 0x08, 0x04, hexStringToByteArray(FileId.EF_ICCID))
            )
        } returns Response(hexStringToByteArray(FCP_ICCID + "%04X".format(Result.SW_NORMAL)))
        every {
            cardIoMock.transmit(Command(Iso7816.INS_READ_BINARY, 0x00, 0x00, 0x100))
        } returns Response(hexStringToByteArray(ICCID_RAW + "%04X".format(Result.SW_NORMAL)))
        every { cardIoMock.closeRemainingChannel() } answers { nothing }
        every { cardIoMock.dispose() } answers { nothing }

        val target = CardRepository.from(ApplicationProvider.getApplicationContext(), 0)
        assertThat(target).isNotNull()
        repository = target!!
        ReflectionHelpers.setField(repository, "cardIo", cardIoMock)
        ReflectionHelpers.setField(repository, "cacheIo", cacheIoMock)

        useCase = GetFileListUseCase(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() = runBlocking {
        if (::repository.isInitialized) repository.dispose()
    }

    private suspend fun initializeRepo() {
        coEvery {
            cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()
    }

    private fun okResponse(aid: String, path: String, fileId: String) =
        SelectResponse(ICCID, aid, path, fileId, FCP_OK, Result.SW_NORMAL)

    private fun notFoundResponse(aid: String, path: String, fileId: String) =
        SelectResponse(ICCID, aid, path, fileId, FCP_NOT_FOUND, Result.SW_NOT_FOUND)

    @Test
    fun execute_mfRoot_returnsOnlyExistingFiles() = runBlocking {
        initializeRepo()
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, "2F00") } returns
                okResponse(FileId.AID_NONE, FileId.PATH_MF, "2F00")
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, "2F05") } returns
                notFoundResponse(FileId.AID_NONE, FileId.PATH_MF, "2F05")
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, "2F06") } returns
                okResponse(FileId.AID_NONE, FileId.PATH_MF, "2F06")
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, "2F08") } returns
                notFoundResponse(FileId.AID_NONE, FileId.PATH_MF, "2F08")
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, "2FE2") } returns
                notFoundResponse(FileId.AID_NONE, FileId.PATH_MF, "2FE2")

        val entries = useCase.execute(R.raw.level_mf, 0, FileId.AID_NONE, FileId.PATH_MF)

        assertThat(entries).hasSize(2)
        assertThat(entries[0].id).isEqualTo("2F00")
        assertThat(entries[1].id).isEqualTo("2F06")
    }

    @Test
    fun execute_allNotFound_returnsEmpty() = runBlocking {
        initializeRepo()
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, any()) } returns null

        val entries = useCase.execute(R.raw.level_mf, 0, FileId.AID_NONE, FileId.PATH_MF)

        assertThat(entries).isEmpty()
    }

    @Test
    fun execute_efNoChildren_markedAsFile() = runBlocking {
        initializeRepo()
        // Register catch-all first; specific overrides take priority (LIFO in MockK)
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, any()) } returns null
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, "2F00") } returns
                okResponse(FileId.AID_NONE, FileId.PATH_MF, "2F00")

        val entries = useCase.execute(R.raw.level_mf, 0, FileId.AID_NONE, FileId.PATH_MF)

        assertThat(entries).hasSize(1)
        assertThat(entries[0].isDirectory).isFalse()
    }

    @Test
    fun execute_dfHasChildren_markedAsDirectory() = runBlocking {
        val usimAid = "A0000000871002FFFFFFFFFFFFFFFF"
        initializeRepo()
        coEvery { cacheIoMock.get(ICCID, usimAid, FileId.PATH_ADF, any()) } returns null
        coEvery {
            cacheIoMock.get(ICCID, usimAid, FileId.PATH_ADF, "5FC0")
        } returns okResponse(usimAid, FileId.PATH_ADF, "5FC0")

        val entries = useCase.execute(R.raw.level_adf_usim, 0, usimAid, FileId.PATH_ADF)

        val df5gs = entries.find { it.id == "5FC0" }
        assertThat(df5gs).isNotNull()
        assertThat(df5gs!!.isDirectory).isTrue()
    }

    @Test
    fun execute_usimRoot_returnsCorrectNames() = runBlocking {
        val usimAid = "A0000000871002FFFFFFFFFFFFFFFF"
        initializeRepo()
        coEvery { cacheIoMock.get(ICCID, usimAid, FileId.PATH_ADF, any()) } returns null
        coEvery { cacheIoMock.get(ICCID, usimAid, FileId.PATH_ADF, "6F07") } returns
                okResponse(usimAid, FileId.PATH_ADF, "6F07")

        val entries = useCase.execute(R.raw.level_adf_usim, 0, usimAid, FileId.PATH_ADF)

        assertThat(entries).hasSize(1)
        assertThat(entries[0].name).isEqualTo("IMSI")
        assertThat(entries[0].description).isEqualTo("International Mobile Subscriber Identity")
    }

    @Test
    fun execute_nestedDfPath_returnsChildren() = runBlocking {
        val usimAid = "A0000000871002FFFFFFFFFFFFFFFF"
        val gsPath = FileId.PATH_ADF + "5FC0"  // "7FFF5FC0"
        initializeRepo()
        coEvery { cacheIoMock.get(ICCID, usimAid, gsPath, "4F07") } returns
                okResponse(usimAid, gsPath, "4F07")
        coEvery { cacheIoMock.get(ICCID, usimAid, gsPath, "4F08") } returns null

        val entries = useCase.execute(R.raw.level_adf_usim, 0, usimAid, gsPath)

        assertThat(entries).hasSize(1)
        assertThat(entries[0].id).isEqualTo("4F07")
        assertThat(entries[0].name).isEqualTo("SUCI_Calc_Info")
    }

    @Test
    fun parseRootMeta_mf_returnsNameWithEmptyId() {
        val (name, id) = useCase.parseRootMeta(R.raw.level_mf)

        assertThat(name).isEqualTo("MF")
        assertThat(id).isEmpty()
    }

    @Test
    fun parseRootMeta_usim_returnsNameAndId() {
        val (name, id) = useCase.parseRootMeta(R.raw.level_adf_usim)

        assertThat(name).isEqualTo("ADF USIM")
        assertThat(id).isEqualTo("7FFF")
    }
}
