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
import com.github.cheeriotb.uiccbrowser.cacheio.SelectResponseDataSource
import com.github.cheeriotb.uiccbrowser.cardio.Command
import com.github.cheeriotb.uiccbrowser.cardio.Interface
import com.github.cheeriotb.uiccbrowser.cardio.Iso7816
import com.github.cheeriotb.uiccbrowser.cardio.Response
import com.github.cheeriotb.uiccbrowser.repository.CardRepository
import com.github.cheeriotb.uiccbrowser.repository.FileId
import com.github.cheeriotb.uiccbrowser.util.hexStringToByteArray
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class CurrentDirectoryFcpUseCaseUnitTest {
    @Rule @JvmField
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.READ_PHONE_STATE)

    private val cardIoMock = mockk<Interface>()
    private val cacheIoMock = mockk<SelectResponseDataSource>()
    private lateinit var repository: CardRepository
    private lateinit var useCase: CurrentDirectoryFcpUseCase

    companion object {
        private const val ICCID = "8988211000000282106F"
        private const val AID_USIM = "A0000000871001FFFFFFFFFFFFFFFFFF"
        private const val AID_ISIM = "A0000000871004FFFFFFFFFFFFFFFFFF"
        private const val PATH_ADF = "7FFF"
        private const val PATH_DF_5GS = "7FFF5FC0"
        private const val FID_AD = "6FAD"
        private const val FID_OPL5G = "4F08"
        private const val FCP_ADF_USIM =
                "62128410A0000000871001FFFFFFFFFFFFFFFFFF"
        private const val FCP_ADF_ISIM =
                "62128410A0000000871004FFFFFFFFFFFFFFFFFF"
        private const val FCP_DF_5GS = "620483025FC0"
        private const val FCP_WRONG_DIRECTORY = "620483027F20"
    }

    @Before
    fun setUp() {
        ReflectionHelpers.setStaticField(CardRepository::class.java, "instances", null)
        CurrentDirectoryFcpUseCase.clearCache()
        every { cardIoMock.openChannel(hexStringToByteArray(AID_USIM)) } returns
                Interface.OpenChannelResult.SUCCESS
        every { cardIoMock.openChannel(hexStringToByteArray(AID_ISIM)) } returns
                Interface.OpenChannelResult.SUCCESS
        every { cardIoMock.closeRemainingChannel() } answers { nothing }
        every { cardIoMock.dispose() } answers { nothing }

        repository = CardRepository.from(ApplicationProvider.getApplicationContext(), 0)!!
        ReflectionHelpers.setField(repository, "cardIo", cardIoMock)
        ReflectionHelpers.setField(repository, "cacheIo", cacheIoMock)
        ReflectionHelpers.setField(repository, "_iccId", ICCID)
        useCase = CurrentDirectoryFcpUseCase(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        CurrentDirectoryFcpUseCase.clearCache()
        ReflectionHelpers.setStaticField(CardRepository::class.java, "instances", null)
    }

    @Test
    fun prepareForEf_cachesValidatedDirectoryFcp() = runBlocking {
        val fileId = FileId(AID_USIM, PATH_DF_5GS, FID_OPL5G)
        selectDirectoryReturns(PATH_DF_5GS, FCP_DF_5GS)

        useCase.prepareForEf(0, fileId)

        val cached = useCase.queryForEf(fileId)
        assertThat(cached).isNotNull()
        assertThat(cached!!.data).isEqualTo(hexStringToByteArray(FCP_DF_5GS))
    }

    @Test
    fun prepareForEf_sameDirectoryAlreadyCached_doesNotSelectAgain() = runBlocking {
        val fileId = FileId(AID_USIM, PATH_ADF, FID_AD)
        selectDirectoryReturns(PATH_ADF, FCP_ADF_USIM)

        useCase.prepareForEf(0, fileId)
        useCase.prepareForEf(0, fileId)

        verify(exactly = 1) {
            cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x04,
                    hexStringToByteArray(PATH_ADF)))
        }
    }

    @Test
    fun prepareForEf_samePathDifferentAid_replacesCache() = runBlocking {
        val usimFileId = FileId(AID_USIM, PATH_ADF, FID_AD)
        val isimFileId = FileId(AID_ISIM, PATH_ADF, FID_AD)
        selectDirectoryReturnsMany(PATH_ADF, listOf(FCP_ADF_USIM, FCP_ADF_ISIM))

        useCase.prepareForEf(0, usimFileId)
        useCase.prepareForEf(0, isimFileId)

        assertThat(useCase.queryForEf(usimFileId)).isNull()
        assertThat(useCase.queryForEf(isimFileId)).isNotNull()
    }

    @Test
    fun prepareForEf_invalidDirectoryFcpClearsOldCache() = runBlocking {
        val oldFileId = FileId(AID_USIM, PATH_ADF, FID_AD)
        val newFileId = FileId(AID_USIM, PATH_DF_5GS, FID_OPL5G)
        selectDirectoryReturns(PATH_ADF, FCP_ADF_USIM)
        selectDirectoryReturns(PATH_DF_5GS, FCP_WRONG_DIRECTORY)

        useCase.prepareForEf(0, oldFileId)
        useCase.prepareForEf(0, newFileId)

        assertThat(useCase.queryForEf(oldFileId)).isNull()
        assertThat(useCase.queryForEf(newFileId)).isNull()
    }

    private fun selectDirectoryReturns(path: String, fcp: String) {
        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x04,
                hexStringToByteArray(path))) } returns Response(hexStringToByteArray(fcp + "9000"))
    }

    private fun selectDirectoryReturnsMany(path: String, fcps: List<String>) {
        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x04,
                hexStringToByteArray(path))) } returnsMany
                fcps.map { Response(hexStringToByteArray(it + "9000")) }
    }
}
