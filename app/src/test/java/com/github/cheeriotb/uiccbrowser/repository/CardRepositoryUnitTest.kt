/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.repository

import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import com.github.cheeriotb.uiccbrowser.cacheio.CachedSubscription
import com.github.cheeriotb.uiccbrowser.cacheio.CachedSubscriptionDataSource
import com.github.cheeriotb.uiccbrowser.cacheio.SelectResponse
import com.github.cheeriotb.uiccbrowser.cacheio.SelectResponseDataSource
import com.github.cheeriotb.uiccbrowser.cardio.Command
import com.github.cheeriotb.uiccbrowser.cardio.Interface
import com.github.cheeriotb.uiccbrowser.cardio.Iso7816
import com.github.cheeriotb.uiccbrowser.cardio.Response
import com.github.cheeriotb.uiccbrowser.util.hexStringToByteArray
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class CardRepositoryUnitTest {
    @Rule @JvmField
    val grantPermissionRule: GrantPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.READ_PHONE_STATE)

    private val cardIoMock = mockk<Interface>()
    private val cacheIoMock = mockk<SelectResponseDataSource>()
    private val subscriptionIoMock = mockk<CachedSubscriptionDataSource>()

    private lateinit var repository: CardRepository

    companion object {
        private const val PROFILE_NAME = "USIM"

        private const val FCP = "621E8202412183022FE2A506C00100CA01808A01058B032F06048002000A8800"
        private const val DATA1 = "30313233343536373839"
        private const val DATA2 = "30313233343536373839"
        private val RESPONSE_NOT_FOUND = "%04X".format(Result.SW_NOT_FOUND)
        private val RESPONSE_NORMAL = "%04X".format(Result.SW_NORMAL)
        private val RESPONSE_FCP = FCP + RESPONSE_NORMAL

        private const val ICCID = "8988211000000282106F"
        private const val ICCID_RAW = "988812010000202801F6"
        private val RESPONSE_ICCID = ICCID_RAW + RESPONSE_NORMAL

        private const val AID = "A0000000871001FFFFFFFFFFFFFFFFFF"
        private const val PATH_ADF = "7FFF"
        private const val FID_AD = "6FAD"
        private const val FID_FPLMN = "6F7B"

        private const val FID_DIR = "2F00"

        private val FILE_ID_DIR = FileId(AID, FileId.PATH_MF, FID_DIR)
        private val FILE_ID_AD = FileId(AID, PATH_ADF, FID_AD)
        private val FILE_ID_FPLMN = FileId(AID, PATH_ADF, FID_FPLMN)
        private val LEVEL_ADF = FileId(AID, PATH_ADF)
    }

    @Before
    fun setUp() {
        every { cardIoMock.openChannel(Interface.NO_AID_SPECIFIED) } returns
                Interface.OpenChannelResult.SUCCESS
        every { cardIoMock.openChannel(hexStringToByteArray(AID)) } returns
                Interface.OpenChannelResult.SUCCESS
        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
                0x08 /* Select by path from MF */, 0x04 /* Return FCP template */,
                hexStringToByteArray(CardRepository.EF_ICCID))) } returns
                Response(hexStringToByteArray(RESPONSE_FCP))
        every { cardIoMock.transmit(Command(Iso7816.INS_READ_BINARY, 0x00, 0x00, 0x100)) } returns
                Response(hexStringToByteArray(RESPONSE_ICCID))
        every { cardIoMock.closeRemainingChannel() } answers { nothing }
        every { cardIoMock.dispose() } answers { nothing }

        coEvery { subscriptionIoMock.get(ICCID) } returns CachedSubscription(ICCID, "USIM")

        val target = CardRepository.from(ApplicationProvider.getApplicationContext(), 0)
        assertThat(target).isNotNull()
        repository = target!!
        ReflectionHelpers.setField(repository, "cardIo", cardIoMock)
        ReflectionHelpers.setField(repository, "cacheIo", cacheIoMock)
        ReflectionHelpers.setField(repository, "subscriptionIo", subscriptionIoMock)
    }

    @After
    fun tearDown() = runBlocking {
        if (::repository.isInitialized) {
            repository.dispose()
        }
    }

    @Test
    fun from() {
        assertThat(CardRepository.from(ApplicationProvider.getApplicationContext(), 0))
                .isEqualTo(repository)
    }

    @Test
    fun initialize_failure_openChannel() = runBlocking {
        every { cardIoMock.openChannel(Interface.NO_AID_SPECIFIED) } returns
                Interface.OpenChannelResult.GENERIC_FAILURE

        assertThat(repository.initialize()).isFalse()
        assertThat(repository.isAccessible).isFalse()
        assertThat(repository.isCached).isFalse()

        assertThat(repository.cacheFileControlParameters(FILE_ID_AD)).isFalse()
        assertThat(repository.queryFileControlParameters(LEVEL_ADF)).isEmpty()

        val readBinaryParams = ReadBinaryParams.Builder(FILE_ID_FPLMN).build()
        assertThat(repository.readBinary(readBinaryParams).sw)
                .isEqualTo(CardRepository.SW_INTERNAL_EXCEPTION)

        val readRecordParams = ReadRecordParams.Builder(FILE_ID_DIR).build()
        assertThat(repository.readRecord(readRecordParams).sw)
                .isEqualTo(CardRepository.SW_INTERNAL_EXCEPTION)

        val readAllRecordParams = ReadAllRecordsParams.Builder(FILE_ID_DIR).build()
        assertThat(repository.readAllRecords(readAllRecordParams)[0].sw)
                .isEqualTo(CardRepository.SW_INTERNAL_EXCEPTION)

        repository.finalizeCache(PROFILE_NAME)
        assertThat(repository.isCached).isFalse()
        coVerify(inverse = true) {
            subscriptionIoMock.insert(CachedSubscription(ICCID, PROFILE_NAME))
        }
    }

    @Test
    fun initialize_failure_selectIccId() = runBlocking {
        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
                0x08 /* Select by path from MF */, 0x04 /* Return FCP template */,
                hexStringToByteArray(CardRepository.EF_ICCID))) } returns
                Response(hexStringToByteArray("6F00"))

        assertThat(repository.initialize()).isFalse()
        assertThat(repository.isAccessible).isFalse()
        assertThat(repository.isCached).isFalse()
    }

    @Test
    fun initialize_failure_readIccId() = runBlocking {
        every { cardIoMock.transmit(Command(Iso7816.INS_READ_BINARY, 0x00, 0x00, 0x100))
                } returns Response(hexStringToByteArray("6F00"))

        assertThat(repository.initialize()).isFalse()
        assertThat(repository.isAccessible).isFalse()
        assertThat(repository.isCached).isFalse()
    }

    @Test
    fun initialize_notCached() = runBlocking {
        coEvery { subscriptionIoMock.get(ICCID) } returns null
        coEvery { cacheIoMock.delete(any()) } answers { nothing }
        coEvery { cacheIoMock.insert(any()) } answers { nothing }

        assertThat(repository.initialize()).isTrue()
        assertThat(repository.isAccessible).isTrue()
        assertThat(repository.isCached).isFalse()

        // Query is not yet available before finishing the caching operation.
        assertThat(repository.queryFileControlParameters(LEVEL_ADF)).isEmpty()

        coVerifyOrder {
            cacheIoMock.delete(ICCID)
            cacheIoMock.insert(SelectResponse(ICCID, FileId.AID_NONE,
                    FileId.PATH_MF, CardRepository.EF_ICCID,
                    hexStringToByteArray(FCP), Result.SW_NORMAL))
        }
    }

    @Test
    fun initialize_cached() = runBlocking {
        assertThat(repository.initialize()).isTrue()
        assertThat(repository.isAccessible).isTrue()
        assertThat(repository.isCached).isTrue()

        // No need to cache the FCP templates again.
        assertThat(repository.cacheFileControlParameters(FILE_ID_AD)).isFalse()
        // Nothing shall happen as the cache has already been finalized.
        repository.finalizeCache(PROFILE_NAME)

        // The used logical channel must be closed in a certain amount of time.
        delay(1000)

        coVerify(inverse = true) { cacheIoMock.delete(any()) }
        coVerify(inverse = true) { cacheIoMock.insert(any()) }
        coVerify(inverse = true) {
            subscriptionIoMock.insert(CachedSubscription(ICCID, PROFILE_NAME))
        }
        verify() { cardIoMock.closeRemainingChannel() }
    }

    @Test
    fun cacheFileControlParameters_alreadyCached() = runBlocking {
        coEvery { subscriptionIoMock.get(ICCID) } returns null
        coEvery { cacheIoMock.delete(any()) } answers { nothing }
        coEvery { cacheIoMock.insert(any()) } answers { nothing }

        repository.initialize()

        coEvery { cacheIoMock.get(ICCID, AID, PATH_ADF, FID_AD) } returns
                SelectResponse(ICCID, AID, PATH_ADF, FID_AD,
                hexStringToByteArray(FCP), Result.SW_NORMAL)

        // Skip already cached one and move to the next
        assertThat(repository.cacheFileControlParameters(FILE_ID_AD)).isTrue()
    }

    @Test
    fun cacheFileControlParameters_openChannelError() = runBlocking {
        repository.initialize()

        every { cardIoMock.openChannel(hexStringToByteArray(AID)) } returns
                Interface.OpenChannelResult.GENERIC_FAILURE

        assertThat(repository.cacheFileControlParameters(FILE_ID_AD)).isFalse()
    }

    @Test
    fun cacheFileControlParameters_success() = runBlocking {
        coEvery { subscriptionIoMock.get(ICCID) } returns null
        coEvery { cacheIoMock.delete(any()) } answers { nothing }
        coEvery { cacheIoMock.insert(any()) } answers { nothing }

        repository.initialize()

        coEvery { cacheIoMock.get(ICCID, AID, PATH_ADF, FID_AD) } returns null
        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
                0x08 /* Select by path from MF */, 0x04 /* Return FCP template */,
                hexStringToByteArray(PATH_ADF + FID_AD))) } returns
                Response(hexStringToByteArray(RESPONSE_FCP))

        assertThat(repository.cacheFileControlParameters(FILE_ID_AD)).isTrue()

        // The used logical channel must be closed in a certain amount of time.
        delay(1000)

        verifyOrder {
            cardIoMock.openChannel(hexStringToByteArray(AID))
            cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
                    0x08, 0x04, hexStringToByteArray(PATH_ADF + FID_AD)))
            cardIoMock.closeRemainingChannel()
        }
        coVerify {
            cacheIoMock.insert(SelectResponse(ICCID, AID, PATH_ADF, FID_AD,
                    hexStringToByteArray(FCP), Result.SW_NORMAL))
        }
    }

    @Test
    fun queryFileControlParameters_success() = runBlocking {
        repository.initialize()

        val response1 = SelectResponse(ICCID, AID, PATH_ADF, FID_AD,
                hexStringToByteArray(FCP), Result.SW_NORMAL)
        val response2 = SelectResponse(ICCID, AID, PATH_ADF, FID_FPLMN,
                hexStringToByteArray(FCP), Result.SW_NORMAL)
        coEvery { cacheIoMock.getAll(ICCID, AID, PATH_ADF) } returns
                mutableListOf(response1, response2)

        val list = repository.queryFileControlParameters(LEVEL_ADF)
        assertThat(list.size).isEqualTo(2)
        assertThat(list[0].fileId).isEqualTo(FID_AD)
        assertThat(list[1].fileId).isEqualTo(FID_FPLMN)

        coVerify { cacheIoMock.getAll(ICCID, AID, PATH_ADF) }
    }

    @Test
    fun finalizeCache_success() = runBlocking {
        coEvery { subscriptionIoMock.get(ICCID) } returns null
        coEvery { subscriptionIoMock.insert(any()) } answers { nothing }
        coEvery { cacheIoMock.delete(any()) } answers { nothing }
        coEvery { cacheIoMock.insert(any()) } answers { nothing }

        repository.initialize()
        repository.finalizeCache(PROFILE_NAME)
        assertThat(repository.isCached).isTrue()

        coVerify { subscriptionIoMock.insert(CachedSubscription(ICCID, PROFILE_NAME)) }
    }

    @Test
    fun readBinary_openChannelError() = runBlocking {
        repository.initialize()

        every { cardIoMock.openChannel(hexStringToByteArray(AID)) } returns
                Interface.OpenChannelResult.GENERIC_FAILURE

        val params = ReadBinaryParams.Builder(FILE_ID_FPLMN).build()
        val result = Result.Builder(FID_FPLMN)
                .sw(CardRepository.SW_INTERNAL_EXCEPTION)
                .build()
        assertThat(repository.readBinary(params)).isEqualTo(result)

        verify { cardIoMock.openChannel(hexStringToByteArray(AID)) }
        verify(inverse = true) { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
                0x08, 0x0C, hexStringToByteArray(PATH_ADF + FID_FPLMN))) }
    }

    @Test
    fun readBinary_selectError() = runBlocking {
        repository.initialize()

        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                hexStringToByteArray(PATH_ADF + FID_FPLMN))) } returns
                Response(hexStringToByteArray(RESPONSE_NOT_FOUND))

        val params = ReadBinaryParams.Builder(FILE_ID_FPLMN).build()
        val result = Result.Builder(FID_FPLMN)
                .sw(CardRepository.SW_INTERNAL_EXCEPTION)
                .build()
        assertThat(repository.readBinary(params)).isEqualTo(result)

        verifyOrder {
            cardIoMock.openChannel(hexStringToByteArray(AID))
            cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
                    0x08, 0x0C, hexStringToByteArray(PATH_ADF + FID_FPLMN)))
        }
    }

    @Test
    fun readBinary_success() = runBlocking {
        repository.initialize()

        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                hexStringToByteArray(PATH_ADF + FID_FPLMN))) } returns
                Response(hexStringToByteArray(FCP + RESPONSE_NORMAL))
        every { cardIoMock.transmit(Command(Iso7816.INS_READ_BINARY, 0x00, 0x00, 0x100)) } returns
                Response(hexStringToByteArray(DATA1 + RESPONSE_NORMAL))

        val params = ReadBinaryParams.Builder(FILE_ID_FPLMN).build()
        val result = Result.Builder(FID_FPLMN)
                .data(hexStringToByteArray(DATA1))
                .sw(Result.SW_NORMAL)
                .build()
        assertThat(repository.readBinary(params)).isEqualTo(result)

        // The used logical channel must be closed in a certain amount of time.
        delay(1000)

        verifyOrder {
            cardIoMock.openChannel(hexStringToByteArray(AID))
            cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                    hexStringToByteArray(PATH_ADF + FID_FPLMN)))
            cardIoMock.transmit(Command(Iso7816.INS_READ_BINARY, 0x00, 0x00, 0x100))
            cardIoMock.closeRemainingChannel()
        }
    }

    @Test
    fun readRecord_openChannelError() = runBlocking {
        repository.initialize()

        every { cardIoMock.openChannel(hexStringToByteArray(AID)) } returns
                Interface.OpenChannelResult.GENERIC_FAILURE

        val params = ReadRecordParams.Builder(FILE_ID_DIR)
                .recordNo(1)
                .build()
        val result = Result.Builder(FID_DIR)
                .sw(CardRepository.SW_INTERNAL_EXCEPTION)
                .build()
        assertThat(repository.readRecord(params)).isEqualTo(result)

        verify { cardIoMock.openChannel(hexStringToByteArray(AID)) }
        verify(inverse = true) { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
                0x08, 0x0C, hexStringToByteArray(FID_DIR))) }
    }

    @Test
    fun readRecord_selectError() = runBlocking {
        repository.initialize()

        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                hexStringToByteArray(FID_DIR))) } returns
                Response(hexStringToByteArray(RESPONSE_NOT_FOUND))

        val params = ReadRecordParams.Builder(FILE_ID_DIR)
                .recordNo(1)
                .build()
        val result = Result.Builder(FID_DIR)
                .sw(CardRepository.SW_INTERNAL_EXCEPTION)
                .build()
        assertThat(repository.readRecord(params)).isEqualTo(result)

        verifyOrder {
            cardIoMock.openChannel(hexStringToByteArray(AID))
            cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                    hexStringToByteArray(FID_DIR)))
        }
    }

    @Test
    fun readRecord_success() = runBlocking {
        repository.initialize()

        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                hexStringToByteArray(FID_DIR))) } returns
                Response(hexStringToByteArray(FCP + RESPONSE_NORMAL))
        every { cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x01, 0x04, 0x100)) } returns
                Response(hexStringToByteArray(DATA1 + RESPONSE_NORMAL))

        val params = ReadRecordParams.Builder(FILE_ID_DIR)
                .recordNo(1)
                .build()
        val result = Result.Builder(FID_DIR)
                .data(hexStringToByteArray(DATA1))
                .sw(Result.SW_NORMAL)
                .build()
        assertThat(repository.readRecord(params)).isEqualTo(result)

        // The used logical channel must be closed in a certain amount of time.
        delay(1000)

        verifyOrder {
            cardIoMock.openChannel(hexStringToByteArray(AID))
            cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                    hexStringToByteArray(FID_DIR)))
            cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x01, 0x04, 0x100))
            cardIoMock.closeRemainingChannel()
        }
    }

    @Test
    fun readAllRecords_openChannelError() = runBlocking {
        repository.initialize()

        every { cardIoMock.openChannel(hexStringToByteArray(AID)) } returns
                Interface.OpenChannelResult.GENERIC_FAILURE

        val params = ReadAllRecordsParams.Builder(FILE_ID_DIR)
                .numberOfRecords(2)
                .build()
        val result = Result.Builder(FID_DIR)
                .sw(CardRepository.SW_INTERNAL_EXCEPTION)
                .build()
        val list = repository.readAllRecords(params)
        assertThat(list.size).isEqualTo(1)
        assertThat(list[0]).isEqualTo(result)

        verify { cardIoMock.openChannel(hexStringToByteArray(AID)) }
        verify(inverse = true) { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
                0x08, 0x0C, hexStringToByteArray(FID_DIR))) }
    }

    @Test
    fun readAllRecords_selectError() = runBlocking {
        repository.initialize()

        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                hexStringToByteArray(FID_DIR))) } returns
                Response(hexStringToByteArray(RESPONSE_NOT_FOUND))

        val params = ReadAllRecordsParams.Builder(FILE_ID_DIR)
                .numberOfRecords(2)
                .build()
        val result = Result.Builder(FID_DIR)
                .sw(CardRepository.SW_INTERNAL_EXCEPTION)
                .build()
        val list = repository.readAllRecords(params)
        assertThat(list.size).isEqualTo(1)
        assertThat(list[0]).isEqualTo(result)

        verifyOrder {
            cardIoMock.openChannel(hexStringToByteArray(AID))
            cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                    hexStringToByteArray(FID_DIR)))
        }
    }

    @Test
    fun readAllRecords_success() = runBlocking {
        repository.initialize()

        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                hexStringToByteArray(FID_DIR))) } returns
                Response(hexStringToByteArray(FCP + RESPONSE_NORMAL))
        every { cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x01, 0x04, 0x100)) } returns
                Response(hexStringToByteArray(DATA1 + RESPONSE_NORMAL))
        every { cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x02, 0x04, 0x100)) } returns
                Response(hexStringToByteArray(DATA2 + RESPONSE_NORMAL))

        val params = ReadAllRecordsParams.Builder(FILE_ID_DIR)
                .numberOfRecords(2)
                .build()
        val result1 = Result.Builder(FID_DIR)
                .data(hexStringToByteArray(DATA1))
                .sw(Result.SW_NORMAL)
                .build()
        val result2 = Result.Builder(FID_DIR)
                .data(hexStringToByteArray(DATA2))
                .sw(Result.SW_NORMAL)
                .build()
        val list = repository.readAllRecords(params)
        assertThat(list.size).isEqualTo(2)
        assertThat(list[0]).isEqualTo(result1)
        assertThat(list[1]).isEqualTo(result2)

        // The used logical channel must be closed in a certain amount of time.
        delay(1000)

        verifyOrder {
            cardIoMock.openChannel(hexStringToByteArray(AID))
            cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                    hexStringToByteArray(FID_DIR)))
            cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x01, 0x04, 0x100))
            cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x02, 0x04, 0x100))
            cardIoMock.closeRemainingChannel()
        }
    }
}
