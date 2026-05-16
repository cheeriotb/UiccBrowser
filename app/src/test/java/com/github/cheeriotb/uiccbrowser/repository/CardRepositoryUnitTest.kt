/*
 *  Copyright (C) 2020-2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.repository

import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
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
import org.junit.Assert.assertThrows
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

    private lateinit var repository: CardRepository

    companion object {
        private const val FCP ="621E8202412183022FE2A506C00100CA01808A01058B032F06048002000A8800"
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
        private const val PIN_4_BYTES = "31323334"
        private const val PIN_8_BYTES = "3132333435363738"

        private val FILE_ID_DIR = FileId(AID, FileId.PATH_MF, FID_DIR)
        private val FILE_ID_MF = FileId(FileId.AID_NONE, FileId.PATH_MF, FileId.MF)
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
                hexStringToByteArray(FileId.EF_ICCID))) } returns
                Response(hexStringToByteArray(RESPONSE_FCP))
        every { cardIoMock.transmit(Command(Iso7816.INS_READ_BINARY, 0x00, 0x00, 0x100)) } returns
                Response(hexStringToByteArray(RESPONSE_ICCID))
        every { cardIoMock.closeRemainingChannel() } answers { nothing }
        every { cardIoMock.dispose() } answers { nothing }

        val target = CardRepository.from(ApplicationProvider.getApplicationContext(), 0)
        assertThat(target).isNotNull()
        repository = target!!
        ReflectionHelpers.setField(repository, "cardIo", cardIoMock)
        ReflectionHelpers.setField(repository, "cacheIo", cacheIoMock)
        repository.isProModeEnabled = false
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
    fun isProModeEnabled_defaultsToFalseAndCanBeUpdated() {
        assertThat(repository.isProModeEnabled).isFalse()

        repository.isProModeEnabled = true

        assertThat(repository.isProModeEnabled).isTrue()
    }

    @Test
    fun verifyPinKeyReference_values() {
        assertThat(KeyReference.APPLICATION_PIN1.value).isEqualTo(0x01)
        assertThat(KeyReference.APPLICATION_PIN8.value).isEqualTo(0x08)
        assertThat(KeyReference.ADM1.value).isEqualTo(0x0A)
        assertThat(KeyReference.ADM5.value).isEqualTo(0x0E)
        assertThat(KeyReference.UNIVERSAL_PIN.value).isEqualTo(0x11)
        assertThat(KeyReference.LOCAL_PIN1.value).isEqualTo(0x81)
        assertThat(KeyReference.LOCAL_PIN8.value).isEqualTo(0x88)
        assertThat(KeyReference.ADM6.value).isEqualTo(0x8A)
        assertThat(KeyReference.ADM10.value).isEqualTo(0x8E)
    }

    @Test
    fun initialize_failure_openChannel() = runBlocking {
        every { cardIoMock.openChannel(Interface.NO_AID_SPECIFIED) } returns
                Interface.OpenChannelResult.GENERIC_FAILURE

        assertThat(repository.initialize()).isFalse()
        assertThat(repository.isAccessible).isFalse()

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

        assertThat(repository.queryVerifyPinRetries(KeyReference.ADM1).sw)
                .isEqualTo(CardRepository.SW_INTERNAL_EXCEPTION)
        assertThat(repository.verifyPin(KeyReference.ADM1, PIN_4_BYTES).sw)
                .isEqualTo(CardRepository.SW_INTERNAL_EXCEPTION)
    }

    @Test
    fun initialize_failure_selectIccId() = runBlocking {
        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
                0x08 /* Select by path from MF */, 0x04 /* Return FCP template */,
                hexStringToByteArray(FileId.EF_ICCID))) } returns
                Response(hexStringToByteArray("6F00"))

        assertThat(repository.initialize()).isFalse()
        assertThat(repository.isAccessible).isFalse()

        delay(1000)

        verify { cardIoMock.closeRemainingChannel() }
    }

    @Test
    fun initialize_failure_readIccId() = runBlocking {
        every { cardIoMock.transmit(Command(Iso7816.INS_READ_BINARY, 0x00, 0x00, 0x100))
                } returns Response(hexStringToByteArray("6F00"))

        assertThat(repository.initialize()).isFalse()
        assertThat(repository.isAccessible).isFalse()
    }

    @Test
    fun initialize_notCached() = runBlocking {
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }

        assertThat(repository.initialize()).isTrue()
        assertThat(repository.isAccessible).isTrue()

        coVerify {
            cacheIoMock.insert(SelectResponse(ICCID, FileId.AID_NONE,
                    FileId.PATH_MF, FileId.EF_ICCID,
                    hexStringToByteArray(FCP), Result.SW_NORMAL))
        }
    }

    @Test
    fun initialize_alreadyCached() = runBlocking {
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns
                SelectResponse(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID,
                        hexStringToByteArray(FCP), Result.SW_NORMAL)

        assertThat(repository.initialize()).isTrue()
        assertThat(repository.isAccessible).isTrue()

        // The used logical channel must be closed in a certain amount of time.
        delay(1000)

        coVerify(inverse = true) { cacheIoMock.insert(any()) }
        verify { cardIoMock.closeRemainingChannel() }
    }

    @Test
    fun cacheFileControlParameters_alreadyCached() = runBlocking {
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }

        repository.initialize()

        coEvery { cacheIoMock.get(ICCID, AID, PATH_ADF, FID_AD) } returns
                SelectResponse(ICCID, AID, PATH_ADF, FID_AD,
                hexStringToByteArray(FCP), Result.SW_NORMAL)

        // Skip already cached one and move to the next
        assertThat(repository.cacheFileControlParameters(FILE_ID_AD)).isTrue()

        verify(inverse = true) { cardIoMock.openChannel(hexStringToByteArray(AID)) }
    }

    @Test
    fun cacheFileControlParameters_openChannelError() = runBlocking {
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        coEvery { cacheIoMock.get(ICCID, AID, PATH_ADF, FID_AD) } returns null
        every { cardIoMock.openChannel(hexStringToByteArray(AID)) } returns
                Interface.OpenChannelResult.GENERIC_FAILURE

        assertThat(repository.cacheFileControlParameters(FILE_ID_AD)).isFalse()
    }

    @Test
    fun cacheFileControlParameters_success() = runBlocking {
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns null
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
    fun cacheFileControlParameters_mfSelectsMfByFileId() = runBlocking {
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
                } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }

        repository.initialize()

        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.MF)
                } returns null
        every {
            cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
                    0x00 /* Select by file ID */, 0x04 /* Return FCP template */,
                    hexStringToByteArray("3F00")))
        } returns Response(hexStringToByteArray(RESPONSE_FCP))

        assertThat(repository.cacheFileControlParameters(FILE_ID_MF)).isTrue()

        verify {
            cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
                    0x00, 0x04, hexStringToByteArray("3F00")))
        }
        coVerify {
            cacheIoMock.insert(SelectResponse(ICCID, FileId.AID_NONE,
                    FileId.PATH_MF, FileId.MF, hexStringToByteArray(FCP), Result.SW_NORMAL))
        }
    }

    @Test
    fun readDirectoryFileControlParameters_adfSelectsDirectoryOnly() = runBlocking {
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
                } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x04,
                hexStringToByteArray(PATH_ADF))) } returns
                Response(hexStringToByteArray(RESPONSE_FCP))

        val result = repository.readDirectoryFileControlParameters(LEVEL_ADF)

        assertThat(result.isOk).isTrue()
        assertThat(result.data).isEqualTo(hexStringToByteArray(FCP))
        verify {
            cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x04,
                    hexStringToByteArray(PATH_ADF)))
        }
    }

    @Test
    fun readDirectoryFileControlParameters_efFileIdRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.readDirectoryFileControlParameters(FILE_ID_AD) }
        }
    }

    @Test
    fun queryFileControlParameters_success() = runBlocking {
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
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
    fun readBinary_openChannelError() = runBlocking {
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
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
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
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
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
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
    fun readBinary_insufficientSecurity_forgetsOnlyTrustedVerifiedPins() = runBlocking {
        coEvery {
            cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        val paddedPin = hexStringToByteArray(PIN_4_BYTES + "FFFFFFFF")
        every { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.ADM1.value, paddedPin)) } returns
                Response(hexStringToByteArray(RESPONSE_NORMAL))
        every { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.ADM2.value, paddedPin)) } returns
                Response(hexStringToByteArray(RESPONSE_NORMAL))
        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                hexStringToByteArray(PATH_ADF + FID_FPLMN))) } returns
                Response(hexStringToByteArray(FCP + RESPONSE_NORMAL))
        every {
            cardIoMock.transmit(Command(Iso7816.INS_READ_BINARY, 0x00, 0x00, 0x100))
        } returns Response(hexStringToByteArray(
                "%04X".format(Result.SW_INSUFFICIENT_SECURITY)
        ))

        repository.verifyPin(KeyReference.ADM1, PIN_4_BYTES, fileId = FILE_ID_FPLMN)
        repository.verifyPin(KeyReference.ADM2, PIN_4_BYTES, fileId = FILE_ID_FPLMN)
        repository.markVerifiedPinsTrustedForNextAccess(
                listOf(KeyReference.ADM1),
                FILE_ID_FPLMN
        )
        repository.readBinary(ReadBinaryParams.Builder(FILE_ID_FPLMN).build())

        assertThat(repository.isPinVerified(KeyReference.ADM1, FILE_ID_FPLMN)).isFalse()
        assertThat(repository.isPinVerified(KeyReference.ADM2, FILE_ID_FPLMN)).isTrue()
    }

    @Test
    fun updateBinary_openChannelError() = runBlocking {
        coEvery {
            cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        every { cardIoMock.openChannel(hexStringToByteArray(AID)) } returns
                Interface.OpenChannelResult.GENERIC_FAILURE

        val data = hexStringToByteArray(DATA1)
        val params = UpdateBinaryParams.Builder(FILE_ID_FPLMN)
                .data(data)
                .build()
        val result = Result.Builder(FID_FPLMN)
                .sw(CardRepository.SW_INTERNAL_EXCEPTION)
                .build()
        assertThat(repository.updateBinary(params)).isEqualTo(result)

        verify { cardIoMock.openChannel(hexStringToByteArray(AID)) }
        verify(inverse = true) { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
                0x08, 0x0C, hexStringToByteArray(PATH_ADF + FID_FPLMN))) }
    }

    @Test
    fun updateBinary_selectError() = runBlocking {
        coEvery {
            cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                hexStringToByteArray(PATH_ADF + FID_FPLMN))) } returns
                Response(hexStringToByteArray(RESPONSE_NOT_FOUND))

        val data = hexStringToByteArray(DATA1)
        val params = UpdateBinaryParams.Builder(FILE_ID_FPLMN)
                .data(data)
                .build()
        val result = Result.Builder(FID_FPLMN)
                .sw(CardRepository.SW_INTERNAL_EXCEPTION)
                .build()
        assertThat(repository.updateBinary(params)).isEqualTo(result)

        verifyOrder {
            cardIoMock.openChannel(hexStringToByteArray(AID))
            cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
                    0x08, 0x0C, hexStringToByteArray(PATH_ADF + FID_FPLMN)))
        }
    }

    @Test
    fun updateBinary_success() = runBlocking {
        coEvery {
            cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                hexStringToByteArray(PATH_ADF + FID_FPLMN))) } returns
                Response(hexStringToByteArray(FCP + RESPONSE_NORMAL))
        val data = hexStringToByteArray(DATA1)
        every {
            cardIoMock.transmit(Command(Iso7816.INS_UPDATE_BINARY, 0x01, 0x23, data))
        } returns
                Response(hexStringToByteArray(RESPONSE_NORMAL))

        val params = UpdateBinaryParams.Builder(FILE_ID_FPLMN)
                .offset(0x0123)
                .data(data)
                .build()
        val result = Result.Builder(FID_FPLMN)
                .sw(Result.SW_NORMAL)
                .build()
        assertThat(repository.updateBinary(params)).isEqualTo(result)

        delay(1000)

        verifyOrder {
            cardIoMock.openChannel(hexStringToByteArray(AID))
            cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                    hexStringToByteArray(PATH_ADF + FID_FPLMN)))
            cardIoMock.transmit(Command(Iso7816.INS_UPDATE_BINARY, 0x01, 0x23, data))
            cardIoMock.closeRemainingChannel()
        }
    }

    @Test
    fun updateBinary_insufficientSecurity_forgetsOnlyTrustedVerifiedPins() = runBlocking {
        coEvery {
            cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        val paddedPin = hexStringToByteArray(PIN_4_BYTES + "FFFFFFFF")
        every { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.ADM1.value, paddedPin)) } returns
                Response(hexStringToByteArray(RESPONSE_NORMAL))
        every { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.ADM2.value, paddedPin)) } returns
                Response(hexStringToByteArray(RESPONSE_NORMAL))
        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                hexStringToByteArray(PATH_ADF + FID_FPLMN))) } returns
                Response(hexStringToByteArray(FCP + RESPONSE_NORMAL))
        val data = hexStringToByteArray(DATA1)
        every { cardIoMock.transmit(Command(Iso7816.INS_UPDATE_BINARY, 0x00, 0x00, data)) } returns
                Response(hexStringToByteArray(
                        "%04X".format(Result.SW_INSUFFICIENT_SECURITY)
                ))

        repository.verifyPin(KeyReference.ADM1, PIN_4_BYTES, fileId = FILE_ID_FPLMN)
        repository.verifyPin(KeyReference.ADM2, PIN_4_BYTES, fileId = FILE_ID_FPLMN)
        repository.markVerifiedPinsTrustedForNextAccess(
                listOf(KeyReference.ADM1),
                FILE_ID_FPLMN
        )
        repository.updateBinary(UpdateBinaryParams.Builder(FILE_ID_FPLMN).data(data).build())

        assertThat(repository.isPinVerified(KeyReference.ADM1, FILE_ID_FPLMN)).isFalse()
        assertThat(repository.isPinVerified(KeyReference.ADM2, FILE_ID_FPLMN)).isTrue()
    }

    @Test
    fun readRecord_openChannelError() = runBlocking {
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
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
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
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
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
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
    fun updateRecord_openChannelError() = runBlocking {
        coEvery {
            cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        every { cardIoMock.openChannel(hexStringToByteArray(AID)) } returns
                Interface.OpenChannelResult.GENERIC_FAILURE

        val data = hexStringToByteArray(DATA1)
        val params = UpdateRecordParams.Builder(FILE_ID_DIR)
                .recordNo(1)
                .data(data)
                .build()
        val result = Result.Builder(FID_DIR)
                .sw(CardRepository.SW_INTERNAL_EXCEPTION)
                .build()
        assertThat(repository.updateRecord(params)).isEqualTo(result)

        verify { cardIoMock.openChannel(hexStringToByteArray(AID)) }
        verify(inverse = true) { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
                0x08, 0x0C, hexStringToByteArray(FID_DIR))) }
    }

    @Test
    fun updateRecord_selectError() = runBlocking {
        coEvery {
            cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                hexStringToByteArray(FID_DIR))) } returns
                Response(hexStringToByteArray(RESPONSE_NOT_FOUND))

        val data = hexStringToByteArray(DATA1)
        val params = UpdateRecordParams.Builder(FILE_ID_DIR)
                .recordNo(1)
                .data(data)
                .build()
        val result = Result.Builder(FID_DIR)
                .sw(CardRepository.SW_INTERNAL_EXCEPTION)
                .build()
        assertThat(repository.updateRecord(params)).isEqualTo(result)

        verifyOrder {
            cardIoMock.openChannel(hexStringToByteArray(AID))
            cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
                    0x08, 0x0C, hexStringToByteArray(FID_DIR)))
        }
    }

    @Test
    fun updateRecord_success() = runBlocking {
        coEvery {
            cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                hexStringToByteArray(FID_DIR))) } returns
                Response(hexStringToByteArray(FCP + RESPONSE_NORMAL))
        val data = hexStringToByteArray(DATA1)
        every {
            cardIoMock.transmit(Command(Iso7816.INS_UPDATE_RECORD, 0x02, 0x04, data))
        } returns
                Response(hexStringToByteArray(RESPONSE_NORMAL))

        val params = UpdateRecordParams.Builder(FILE_ID_DIR)
                .recordNo(2)
                .data(data)
                .build()
        val result = Result.Builder(FID_DIR)
                .sw(Result.SW_NORMAL)
                .build()
        assertThat(repository.updateRecord(params)).isEqualTo(result)

        delay(1000)

        verifyOrder {
            cardIoMock.openChannel(hexStringToByteArray(AID))
            cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                    hexStringToByteArray(FID_DIR)))
            cardIoMock.transmit(Command(Iso7816.INS_UPDATE_RECORD, 0x02, 0x04, data))
            cardIoMock.closeRemainingChannel()
        }
    }

    @Test
    fun readAllRecords_openChannelError() = runBlocking {
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
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
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
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
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
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

    @Test
    fun queryVerifyPinRetries_success() = runBlocking {
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        every { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.ADM1.value)) } returns Response(hexStringToByteArray("63C3"))

        val response = repository.queryVerifyPinRetries(KeyReference.ADM1)

        assertThat(response.sw).isEqualTo(0x63C3)
        verify { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.ADM1.value)) }
    }

    @Test
    fun verifyPin_success_padsShortCode() = runBlocking {
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        val paddedPin = hexStringToByteArray(PIN_4_BYTES + "FFFFFFFF")
        every { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.ADM1.value, paddedPin)) } returns
                Response(hexStringToByteArray(RESPONSE_NORMAL))

        val response = repository.verifyPin(KeyReference.ADM1, PIN_4_BYTES)

        assertThat(response.sw).isEqualTo(Result.SW_NORMAL)
        verify { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.ADM1.value, paddedPin)) }
    }

    @Test
    fun verifyPin_success_remembersVerifiedPin() = runBlocking {
        coEvery {
            cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        val paddedPin = hexStringToByteArray(PIN_4_BYTES + "FFFFFFFF")
        every { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.APPLICATION_PIN1.value, paddedPin)) } returns
                Response(hexStringToByteArray(RESPONSE_NORMAL))

        repository.verifyPin(KeyReference.APPLICATION_PIN1, PIN_4_BYTES)

        assertThat(repository.isPinVerified(KeyReference.APPLICATION_PIN1)).isTrue()
        assertThat(repository.isPinVerified(KeyReference.APPLICATION_PIN2)).isFalse()
    }

    @Test
    fun verifyPin_refDataInvalidated_remembersVerifiedPin() = runBlocking {
        coEvery {
            cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        val paddedPin = hexStringToByteArray(PIN_4_BYTES + "FFFFFFFF")
        every { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.APPLICATION_PIN1.value, paddedPin)) } returns
                Response(hexStringToByteArray("6984"))

        val response = repository.verifyPin(KeyReference.APPLICATION_PIN1, PIN_4_BYTES)

        assertThat(response.sw).isEqualTo(Result.SW_REF_DATA_INVALIDATED)
        assertThat(repository.isPinVerified(KeyReference.APPLICATION_PIN1)).isTrue()
    }

    @Test
    fun queryVerifyPinRetries_success_doesNotRememberVerifiedPin() = runBlocking {
        coEvery {
            cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        every { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.ADM1.value)) } returns
                Response(hexStringToByteArray(RESPONSE_NORMAL))

        repository.queryVerifyPinRetries(KeyReference.ADM1)

        assertThat(repository.isPinVerified(KeyReference.ADM1)).isFalse()
    }

    @Test
    fun verifyPin_success_remembersLocalPinForDirectoryOnly() = runBlocking {
        coEvery {
            cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        val anotherFileId = FileId(AID, PATH_ADF + "5FC0", FID_FPLMN)
        val paddedPin = hexStringToByteArray(PIN_4_BYTES + "FFFFFFFF")
        every { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.LOCAL_PIN1.value, paddedPin)) } returns
                Response(hexStringToByteArray(RESPONSE_NORMAL))

        repository.verifyPin(KeyReference.LOCAL_PIN1, PIN_4_BYTES, AID, FILE_ID_FPLMN)

        assertThat(repository.isPinVerified(KeyReference.LOCAL_PIN1, FILE_ID_FPLMN)).isTrue()
        assertThat(repository.isPinVerified(KeyReference.LOCAL_PIN1, anotherFileId)).isFalse()
    }

    @Test
    fun updateCurrentDirectoryContext_clearsLocalPinAndAdmButKeepsApplicationPin() = runBlocking {
        coEvery {
            cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        val paddedPin = hexStringToByteArray(PIN_4_BYTES + "FFFFFFFF")
        every { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.APPLICATION_PIN1.value, paddedPin)) } returns
                Response(hexStringToByteArray(RESPONSE_NORMAL))
        every { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.LOCAL_PIN1.value, paddedPin)) } returns
                Response(hexStringToByteArray(RESPONSE_NORMAL))
        every { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.ADM1.value, paddedPin)) } returns
                Response(hexStringToByteArray(RESPONSE_NORMAL))

        repository.updateCurrentDirectoryContext(AID, PATH_ADF)
        repository.verifyPin(KeyReference.APPLICATION_PIN1, PIN_4_BYTES)
        repository.verifyPin(KeyReference.LOCAL_PIN1, PIN_4_BYTES, AID, FILE_ID_FPLMN)
        repository.verifyPin(KeyReference.ADM1, PIN_4_BYTES, AID, FILE_ID_FPLMN)
        repository.updateCurrentDirectoryContext(AID, PATH_ADF + "5FC0")

        assertThat(repository.isPinVerified(KeyReference.APPLICATION_PIN1)).isTrue()
        assertThat(repository.isPinVerified(KeyReference.LOCAL_PIN1, FILE_ID_FPLMN)).isFalse()
        assertThat(repository.isPinVerified(KeyReference.ADM1, FILE_ID_FPLMN)).isFalse()
    }

    @Test
    fun verifyPin_success_keepsEightByteCode() = runBlocking {
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        val pin = hexStringToByteArray(PIN_8_BYTES)
        every { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.ADM10.value, pin)) } returns
                Response(hexStringToByteArray(RESPONSE_NORMAL))

        val response = repository.verifyPin(KeyReference.ADM10, PIN_8_BYTES)

        assertThat(response.sw).isEqualTo(Result.SW_NORMAL)
        verify { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.ADM10.value, pin)) }
    }

    @Test
    fun verifyPin_success_retainsLogicalChannel() = runBlocking {
        coEvery {
            cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        val paddedPin = hexStringToByteArray(PIN_4_BYTES + "FFFFFFFF")
        every { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.ADM1.value, paddedPin)) } returns
                Response(hexStringToByteArray(RESPONSE_NORMAL))

        val response = repository.verifyPin(KeyReference.ADM1, PIN_4_BYTES)

        assertThat(response.sw).isEqualTo(Result.SW_NORMAL)
        delay(1000)

        verify(exactly = 0) { cardIoMock.closeRemainingChannel() }
    }

    @Test
    fun verifyPin_failure_closesLogicalChannelByTimer() = runBlocking {
        coEvery {
            cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        val paddedPin = hexStringToByteArray(PIN_4_BYTES + "FFFFFFFF")
        every { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.ADM1.value, paddedPin)) } returns
                Response(hexStringToByteArray("63C3"))

        val response = repository.verifyPin(KeyReference.ADM1, PIN_4_BYTES)

        assertThat(response.sw).isEqualTo(0x63C3)
        delay(1000)

        verify { cardIoMock.closeRemainingChannel() }
    }

    @Test
    fun readBinary_afterVerifyPinSuccess_keepsLogicalChannel() = runBlocking {
        coEvery {
            cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        val paddedPin = hexStringToByteArray(PIN_4_BYTES + "FFFFFFFF")
        every { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.ADM1.value, paddedPin)) } returns
                Response(hexStringToByteArray(RESPONSE_NORMAL))
        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                hexStringToByteArray(PATH_ADF + FID_FPLMN))) } returns
                Response(hexStringToByteArray(FCP + RESPONSE_NORMAL))
        every {
            cardIoMock.transmit(Command(Iso7816.INS_READ_BINARY, 0x00, 0x00, 0x100))
        } returns
                Response(hexStringToByteArray(DATA1 + RESPONSE_NORMAL))

        repository.verifyPin(KeyReference.ADM1, PIN_4_BYTES)
        val params = ReadBinaryParams.Builder(FILE_ID_FPLMN).build()
        assertThat(repository.readBinary(params).sw).isEqualTo(Result.SW_NORMAL)
        delay(1000)

        verify(exactly = 0) { cardIoMock.closeRemainingChannel() }
    }

    @Test
    fun releaseLogicalChannel_closesRetainedChannelAndRestoresTimer() = runBlocking {
        coEvery {
            cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        val paddedPin = hexStringToByteArray(PIN_4_BYTES + "FFFFFFFF")
        every { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.ADM1.value, paddedPin)) } returns
                Response(hexStringToByteArray(RESPONSE_NORMAL))
        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                hexStringToByteArray(PATH_ADF + FID_FPLMN))) } returns
                Response(hexStringToByteArray(FCP + RESPONSE_NORMAL))
        every {
            cardIoMock.transmit(Command(Iso7816.INS_READ_BINARY, 0x00, 0x00, 0x100))
        } returns
                Response(hexStringToByteArray(DATA1 + RESPONSE_NORMAL))

        repository.verifyPin(KeyReference.ADM1, PIN_4_BYTES)
        repository.releaseLogicalChannel()
        val params = ReadBinaryParams.Builder(FILE_ID_FPLMN).build()
        assertThat(repository.readBinary(params).sw).isEqualTo(Result.SW_NORMAL)
        delay(1000)

        verify(atLeast = 2) { cardIoMock.closeRemainingChannel() }
    }

    @Test
    fun releaseLogicalChannel_keepsApplicationPinAndClearsAdm() = runBlocking {
        coEvery {
            cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)
        } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        val paddedPin = hexStringToByteArray(PIN_4_BYTES + "FFFFFFFF")
        every { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.APPLICATION_PIN1.value, paddedPin)) } returns
                Response(hexStringToByteArray(RESPONSE_NORMAL))
        every { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.ADM1.value, paddedPin)) } returns
                Response(hexStringToByteArray(RESPONSE_NORMAL))

        repository.verifyPin(KeyReference.APPLICATION_PIN1, PIN_4_BYTES)
        repository.verifyPin(KeyReference.ADM1, PIN_4_BYTES, AID, FILE_ID_FPLMN)
        repository.releaseLogicalChannel()

        assertThat(repository.isPinVerified(KeyReference.APPLICATION_PIN1)).isTrue()
        assertThat(repository.isPinVerified(KeyReference.ADM1, FILE_ID_FPLMN)).isFalse()
    }

    @Test
    fun verifyPin_openChannelError() = runBlocking {
        coEvery { cacheIoMock.get(ICCID, FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID) } returns null
        coEvery { cacheIoMock.insert(any()) } answers { nothing }
        repository.initialize()

        every { cardIoMock.openChannel(hexStringToByteArray(AID)) } returns
                Interface.OpenChannelResult.GENERIC_FAILURE

        val response = repository.verifyPin(KeyReference.LOCAL_PIN1, PIN_4_BYTES, AID)

        assertThat(response.sw).isEqualTo(CardRepository.SW_INTERNAL_EXCEPTION)
        verify(inverse = true) { cardIoMock.transmit(Command(Iso7816.INS_VERIFY_PIN, 0x00,
                KeyReference.LOCAL_PIN1.value, hexStringToByteArray(PIN_4_BYTES + "FFFFFFFF"))) }
    }

    @Test
    fun verifyPin_invalidCode_throws() {
        assertThat(assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.verifyPin(KeyReference.ADM1, "") }
        }).hasMessageThat().isEqualTo("PIN/ADM code must be between 1 and 8 bytes")

        assertThat(assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.verifyPin(KeyReference.ADM1, "123") }
        }).hasMessageThat().isEqualTo("PIN/ADM code must contain an even number of hex digits")

        assertThat(assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.verifyPin(KeyReference.ADM1, "3132333X") }
        }).hasMessageThat().isEqualTo("PIN/ADM code must be a hex string")

        assertThat(assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.verifyPin(KeyReference.ADM1, "313233343536373839") }
        }).hasMessageThat().isEqualTo("PIN/ADM code must be between 1 and 8 bytes")
    }
}
