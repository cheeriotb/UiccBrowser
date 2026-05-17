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
import com.github.cheeriotb.uiccbrowser.repository.KeyReference
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
class EditAccessUseCaseUnitTest {
    @Rule @JvmField
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.READ_PHONE_STATE)

    private val cardIoMock = mockk<Interface>()
    private val cacheIoMock = mockk<SelectResponseDataSource>()
    private lateinit var repository: CardRepository
    private lateinit var useCase: EditAccessUseCase

    companion object {
        private const val ICCID = "8988211000000282106F"
        private const val AID = "A0000000871001FFFFFFFFFFFFFFFFFF"
        private const val PATH_ADF = "7FFF"
        private const val FID_AD = "6FAD"
        private const val FID_ARR = "2F06"
        private const val FCP_COMPACT_ADM1_UPDATE_READ_ALWAYS = "62058C03030A00"
        private const val FCP_EXPANDED_ADM1_READ_UPDATE = "620AAB08800103A40383010A"
        private const val FCP_EXPANDED_PIN1_READ_ADM1_UPDATE =
                "6212AB10800101A403830101800102A40383010A"
        private const val FCP_EXPANDED_PIN1_READ_UPDATE = "620AAB08800103A403830101"
        private const val FCP_EXPANDED_PIN1_OR_ADM1_UPDATE_READ_ALWAYS =
                "6211AB0F800103A00AA403830101A40383010A"
        private const val FCP_EXPANDED_ADM1_OR_UNIVERSAL_PIN_UPDATE_READ_ALWAYS =
                "6211AB0F800103A00AA40383010AA403830111"
        private const val FCP_EXPANDED_NOT_ADM1_READ_UPDATE = "620CAB0A800103A705A40383010A"
        private const val FCP_EXPANDED_COMMAND_DESCRIPTION_ADM1 = "620AAB088401D4A40383010A"
        private const val FCP_ARR_REF_RECORD4 = "62058B032F0604"
        private const val FCP_ARR_REF_SE_RECORDS = "62088B062F0601040205"
        private const val FCP_ARR_REF_SE00_SE01_RECORDS = "62088B062F0600040105"
        private const val FCP_ARR_REF_SE01_RECORD12_SE00_RECORD0 =
                "62088B062F06010C0000"
        private const val FCP_ARR_REF_DUPLICATED_SE00_WITH_RECORD0 =
                "62088B062F0600000004"
        private const val FCP_ADF_PIN_STATUS_ADM1_ADM2_LOCAL_PIN1 =
                "62238410A0000000871001FFFFFFFFFFFFFFFFFFC60F90017083010183010A83010B830181"
        private const val FCP_ADF_PIN_STATUS_APPLICATION_PIN1 =
                "621A8410A0000000871001FFFFFFFFFFFFFFFFFFC606900180830101"
        private const val FCP_ADF_PIN_STATUS_APPLICATION_PIN1_ADM1 =
                "621D8410A0000000871001FFFFFFFFFFFFFFFFFFC6099001C083010183010A"
        private const val FCP_ADF_PIN_STATUS_ADM1_ONLY =
                "621D8410A0000000871001FFFFFFFFFFFFFFFFFFC60990018083010A830101"
        private const val FCP_ADF_PIN_STATUS_UNIVERSAL_PIN_USE =
                "621D8410A0000000871001FFFFFFFFFFFFFFFFFFC609900180950108830111"
        private const val FCP_ADF_PIN_STATUS_UNIVERSAL_PIN_USE_ADM1 =
                "62208410A0000000871001FFFFFFFFFFFFFFFFFFC60C9001C095010883011183010A"
        private const val FCP_ADF_PIN_STATUS_UNIVERSAL_PIN_WITHOUT_USAGE =
                "621A8410A0000000871001FFFFFFFFFFFFFFFFFFC606900180830111"
        private const val FCP_ADF_PIN_STATUS_USAGE_QUALIFIERS =
                "62268410A0000000871001FFFFFFFFFFFFFFFFFFC6129001C0" +
                        "95010183010195010283010A83010B"
        private const val FCP_ADF_PIN_STATUS_MULTI_BYTE_PS =
                "62338410A0000000871001FFFFFFFFFFFFFFFFFFC61F90028080" +
                        "830101830102830103830104830105830106830107830108830111"
        private const val FCP_ADF_NO_PIN_STATUS =
                "62128410A0000000871001FFFFFFFFFFFFFFFFFF"
        private const val ARR_RECORD_ADM1_READ_UPDATE = "800103A40383010A"
        private const val ARR_RECORD_PIN1_READ_ADM1_UPDATE =
                "800101A403830101800102A40383010A"
        private const val ARR_RECORD_READ_ALWAYS_UPDATE_LOCAL_PIN1 =
                "8001019000800102A403830181"
        private const val ARR_RECORD_COMMAND_DESCRIPTION_ADM1 = "8401D4A40383010A"

        private val FILE_ID = FileId(AID, PATH_ADF, FID_AD)
    }

    @Before
    fun setUp() {
        ReflectionHelpers.setStaticField(CardRepository::class.java, "instances", null)
        every { cardIoMock.openChannel(hexStringToByteArray(AID)) } returns
                Interface.OpenChannelResult.SUCCESS
        every { cardIoMock.openChannel(hexStringToByteArray(FileId.AID_NONE)) } returns
                Interface.OpenChannelResult.SUCCESS
        every { cardIoMock.closeRemainingChannel() } answers { nothing }
        every { cardIoMock.dispose() } answers { nothing }

        repository = CardRepository.from(ApplicationProvider.getApplicationContext(), 0)!!
        ReflectionHelpers.setField(repository, "cardIo", cardIoMock)
        ReflectionHelpers.setField(repository, "cacheIo", cacheIoMock)
        ReflectionHelpers.setField(repository, "_iccId", ICCID)
        CurrentDirectoryFcpUseCase.clearCache()

        useCase = EditAccessUseCase(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        CurrentDirectoryFcpUseCase.clearCache()
        ReflectionHelpers.setStaticField(CardRepository::class.java, "instances", null)
    }

    @Test
    fun execute_compactSecurityAttributes_returnsRequiredKeyReference() {
        runBlocking {
            cacheFcp(FCP_COMPACT_ADM1_UPDATE_READ_ALWAYS)

            val outcome = useCase.execute(0, FILE_ID)

            assertThat(outcome.failure).isNull()
            assertThat(outcome.keyReferences).containsExactly(KeyReference.ADM1)
        }
    }

    @Test
    fun execute_expandedSecurityAttributes_returnsRequiredKeyReference() {
        runBlocking {
            cacheFcp(FCP_EXPANDED_ADM1_READ_UPDATE)

            val outcome = useCase.execute(0, FILE_ID)

            assertThat(outcome.failure).isNull()
            assertThat(outcome.keyReferences).containsExactly(KeyReference.ADM1)
        }
    }

    @Test
    fun execute_expandedSecurityAttributesWithoutReadAccess_returnsUpdateKeyReference() {
        runBlocking {
            cacheFcp(FCP_EXPANDED_PIN1_READ_ADM1_UPDATE)

            val outcome = useCase.execute(0, FILE_ID, requireReadAccess = false)

            assertThat(outcome.failure).isNull()
            assertThat(outcome.keyReferences).containsExactly(KeyReference.ADM1)
        }
    }

    @Test
    fun execute_readAccess_returnsReadKeyReferenceOnly() {
        runBlocking {
            cacheFcp(FCP_EXPANDED_PIN1_READ_ADM1_UPDATE)

            val outcome = useCase.execute(0, FILE_ID, EditAccessUseCase.RequiredAccess.READ)

            assertThat(outcome.failure).isNull()
            assertThat(outcome.keyReferences).containsExactly(KeyReference.APPLICATION_PIN1)
        }
    }

    @Test
    fun execute_expandedSecurityAttributesWithReadAccess_returnsReadAndUpdateKeyReferences() {
        runBlocking {
            cacheFcp(FCP_EXPANDED_PIN1_READ_ADM1_UPDATE)

            val outcome = useCase.execute(0, FILE_ID, requireReadAccess = true)

            assertThat(outcome.failure).isNull()
            assertThat(outcome.keyReferences).containsExactly(
                    KeyReference.APPLICATION_PIN1,
                    KeyReference.ADM1
            )
        }
    }

    @Test
    fun execute_disabledKeyReference_filtersVerifyOptions() {
        runBlocking {
            cacheFcp(FCP_EXPANDED_PIN1_READ_ADM1_UPDATE)
            prepareCurrentDirectoryFcp(FCP_ADF_PIN_STATUS_ADM1_ONLY)

            val outcome = useCase.execute(0, FILE_ID, requireReadAccess = true)

            assertThat(outcome.failure).isNull()
            assertThat(outcome.keyReferences).containsExactly(KeyReference.ADM1)
        }
    }

    @Test
    fun execute_onlyDisabledKeyReference_returnsNoVerifyOption() {
        runBlocking {
            cacheFcp(FCP_EXPANDED_PIN1_READ_UPDATE)
            prepareCurrentDirectoryFcp(FCP_ADF_PIN_STATUS_ADM1_ONLY)

            val outcome = useCase.execute(0, FILE_ID, requireReadAccess = true)

            assertThat(outcome.failure).isNull()
            assertThat(outcome.keyReferences).isEmpty()
        }
    }

    @Test
    fun execute_expandedOrSecurityAttributes_returnsKeyReferenceOptions() {
        runBlocking {
            cacheFcp(FCP_EXPANDED_PIN1_OR_ADM1_UPDATE_READ_ALWAYS)

            val outcome = useCase.execute(0, FILE_ID)

            assertThat(outcome.failure).isNull()
            assertThat(outcome.keyReferenceOptions).containsExactly(
                    listOf(KeyReference.APPLICATION_PIN1),
                    listOf(KeyReference.ADM1)
            ).inOrder()
            assertThat(outcome.keyReferences).containsExactly(KeyReference.APPLICATION_PIN1)
        }
    }

    @Test
    fun execute_expandedOrSecurityAttributes_returnsUniversalPinOption() {
        runBlocking {
            cacheFcp(FCP_EXPANDED_ADM1_OR_UNIVERSAL_PIN_UPDATE_READ_ALWAYS)

            val outcome = useCase.execute(0, FILE_ID)

            assertThat(outcome.failure).isNull()
            assertThat(outcome.keyReferenceOptions).containsExactly(
                    listOf(KeyReference.ADM1),
                    listOf(KeyReference.UNIVERSAL_PIN)
            ).inOrder()
        }
    }

    @Test
    fun execute_expandedNotSecurityCondition_returnsUnsupported() {
        runBlocking {
            cacheFcp(FCP_EXPANDED_NOT_ADM1_READ_UPDATE)

            val outcome = useCase.execute(0, FILE_ID)

            assertThat(outcome.failure)
                    .isEqualTo(EditAccessUseCase.Failure.SECURITY_CONDITION_UNSUPPORTED)
        }
    }

    @Test
    fun execute_expandedCommandDescriptionAmDo_returnsUnsupported() {
        runBlocking {
            cacheFcp(FCP_EXPANDED_COMMAND_DESCRIPTION_ADM1)

            val outcome = useCase.execute(0, FILE_ID)

            assertThat(outcome.failure)
                    .isEqualTo(EditAccessUseCase.Failure.SECURITY_CONDITION_UNSUPPORTED)
        }
    }

    @Test
    fun execute_arrReference_readsArrRecord() {
        runBlocking {
            cacheFcp(FCP_ARR_REF_RECORD4)
            every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                    hexStringToByteArray(PATH_ADF + FID_ARR))) } returns
                    Response(hexStringToByteArray("9000"))
            every { cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x04, 0x04, 0x100))
                    } returns Response(hexStringToByteArray(ARR_RECORD_ADM1_READ_UPDATE + "9000"))

            val outcome = useCase.execute(0, FILE_ID)

            assertThat(outcome.failure).isNull()
            assertThat(outcome.keyReferences).containsExactly(KeyReference.ADM1)
        }
    }

    @Test
    fun execute_arrReferenceWithoutReadAccess_returnsUpdateKeyReference() {
        runBlocking {
            cacheFcp(FCP_ARR_REF_RECORD4)
            every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                    hexStringToByteArray(PATH_ADF + FID_ARR))) } returns
                    Response(hexStringToByteArray("9000"))
            every { cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x04, 0x04, 0x100))
                    } returns
                    Response(hexStringToByteArray(ARR_RECORD_PIN1_READ_ADM1_UPDATE + "9000"))

            val outcome = useCase.execute(0, FILE_ID, requireReadAccess = false)

            assertThat(outcome.failure).isNull()
            assertThat(outcome.keyReferences).containsExactly(KeyReference.ADM1)
        }
    }

    @Test
    fun execute_arrReferenceReadAlwaysUpdateLocalPin_returnsUpdateKeyReference() {
        runBlocking {
            cacheFcp(FCP_ARR_REF_RECORD4)
            every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                    hexStringToByteArray(PATH_ADF + FID_ARR))) } returns
                    Response(hexStringToByteArray("9000"))
            every { cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x04, 0x04, 0x100))
                    } returns Response(
                    hexStringToByteArray(ARR_RECORD_READ_ALWAYS_UPDATE_LOCAL_PIN1 + "9000")
            )

            val outcome = useCase.execute(0, FILE_ID, requireReadAccess = true)

            assertThat(outcome.failure).isNull()
            assertThat(outcome.keyReferences).containsExactly(KeyReference.LOCAL_PIN1)
        }
    }

    @Test
    fun execute_arrReferenceCommandDescriptionAmDo_returnsUnsupported() {
        runBlocking {
            cacheFcp(FCP_ARR_REF_RECORD4)
            every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                    hexStringToByteArray(PATH_ADF + FID_ARR))) } returns
                    Response(hexStringToByteArray("9000"))
            every { cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x04, 0x04, 0x100))
                    } returns Response(
                    hexStringToByteArray(ARR_RECORD_COMMAND_DESCRIPTION_ADM1 + "9000")
            )

            val outcome = useCase.execute(0, FILE_ID)

            assertThat(outcome.failure)
                    .isEqualTo(EditAccessUseCase.Failure.SECURITY_CONDITION_UNSUPPORTED)
        }
    }

    @Test
    fun execute_arrReferenceWithMultipleSeRecords_returnsUnsupported() {
        runBlocking {
            cacheFcp(FCP_ARR_REF_SE_RECORDS)

            val outcome = useCase.execute(0, FILE_ID)

            assertThat(outcome.failure)
                    .isEqualTo(EditAccessUseCase.Failure.SECURITY_CONDITION_UNSUPPORTED)
        }
    }

    @Test
    fun execute_arrReferenceWithSeRecords_selectsSe01Record() {
        runBlocking {
            cacheFcp(FCP_ARR_REF_SE_RECORDS)
            prepareCurrentDirectoryFcp(FCP_ADF_PIN_STATUS_APPLICATION_PIN1_ADM1)
            every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                    hexStringToByteArray(PATH_ADF + FID_ARR))) } returns
                    Response(hexStringToByteArray("9000"))
            every { cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x04, 0x04, 0x100))
                    } returns Response(hexStringToByteArray(ARR_RECORD_ADM1_READ_UPDATE + "9000"))

            val outcome = useCase.execute(0, FILE_ID)

            assertThat(outcome.failure).isNull()
            assertThat(outcome.keyReferences).containsExactly(KeyReference.ADM1)
        }
    }

    @Test
    fun execute_arrReferenceWithSeRecords_selectsSe00Record() {
        runBlocking {
            cacheFcp(FCP_ARR_REF_SE00_SE01_RECORDS)
            prepareCurrentDirectoryFcp(FCP_ADF_PIN_STATUS_UNIVERSAL_PIN_USE_ADM1)
            every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                    hexStringToByteArray(PATH_ADF + FID_ARR))) } returns
                    Response(hexStringToByteArray("9000"))
            every { cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x04, 0x04, 0x100))
                    } returns Response(hexStringToByteArray(ARR_RECORD_ADM1_READ_UPDATE + "9000"))

            val outcome = useCase.execute(0, FILE_ID)

            assertThat(outcome.failure).isNull()
            assertThat(outcome.keyReferences).containsExactly(KeyReference.ADM1)
        }
    }

    @Test
    fun execute_arrReferenceWithSeRecord0_returnsUnsupported() {
        runBlocking {
            cacheFcp(FCP_ARR_REF_SE01_RECORD12_SE00_RECORD0)
            prepareCurrentDirectoryFcp(FCP_ADF_PIN_STATUS_UNIVERSAL_PIN_USE)

            val outcome = useCase.execute(0, FILE_ID)

            assertThat(outcome.failure)
                    .isEqualTo(EditAccessUseCase.Failure.SECURITY_CONDITION_UNSUPPORTED)
        }
    }

    @Test
    fun execute_arrReferenceWithDuplicatedSeIncludingRecord0_returnsUnsupported() {
        runBlocking {
            cacheFcp(FCP_ARR_REF_DUPLICATED_SE00_WITH_RECORD0)
            prepareCurrentDirectoryFcp(FCP_ADF_PIN_STATUS_UNIVERSAL_PIN_USE)

            val outcome = useCase.execute(0, FILE_ID)

            assertThat(outcome.failure)
                    .isEqualTo(EditAccessUseCase.Failure.SECURITY_CONDITION_UNSUPPORTED)
        }
    }

    @Test
    fun execute_arrReferenceWithSeRecordsWithoutMatchingSe_returnsUnsupported() {
        runBlocking {
            cacheFcp(FCP_ARR_REF_SE_RECORDS)
            prepareCurrentDirectoryFcp(FCP_ADF_PIN_STATUS_UNIVERSAL_PIN_USE)

            val outcome = useCase.execute(0, FILE_ID)

            assertThat(outcome.failure)
                    .isEqualTo(EditAccessUseCase.Failure.SECURITY_CONDITION_UNSUPPORTED)
        }
    }

    @Test
    fun execute_arrReferenceWithSeRecordsWithoutDecidableSe_returnsUnsupported() {
        runBlocking {
            cacheFcp(FCP_ARR_REF_SE00_SE01_RECORDS)
            prepareCurrentDirectoryFcp(FCP_ADF_PIN_STATUS_UNIVERSAL_PIN_WITHOUT_USAGE)

            val outcome = useCase.execute(0, FILE_ID)

            assertThat(outcome.failure)
                    .isEqualTo(EditAccessUseCase.Failure.SECURITY_CONDITION_UNSUPPORTED)
        }
    }

    @Test
    fun execute_arrReferenceReadFailure_returnsArrReadFailed() {
        runBlocking {
            cacheFcp(FCP_ARR_REF_RECORD4)
            every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                    hexStringToByteArray(PATH_ADF + FID_ARR))) } returns
                    Response(hexStringToByteArray("9000"))
            every { cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x04, 0x04, 0x100))
                    } returns Response(hexStringToByteArray("6A82"))

            val outcome = useCase.execute(0, FILE_ID)

            assertThat(outcome.failure).isEqualTo(EditAccessUseCase.Failure.ARR_READ_FAILED)
        }
    }

    @Test
    fun execute_arrReferenceInsufficientSecurity_returnsExploreKeyReferenceOptions() {
        runBlocking {
            cacheFcp(FCP_ARR_REF_RECORD4)
            prepareCurrentDirectoryFcp(FCP_ADF_PIN_STATUS_ADM1_ADM2_LOCAL_PIN1)
            every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                    hexStringToByteArray(PATH_ADF + FID_ARR))) } returns
                    Response(hexStringToByteArray("9000"))
            every { cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x04, 0x04, 0x100))
                    } returns Response(hexStringToByteArray("6982"))

            val outcome = useCase.execute(0, FILE_ID)

            assertThat(outcome.failure).isNull()
            assertThat(outcome.exploreKeyReferenceOptions).containsExactly(
                    KeyReference.ADM1,
                    KeyReference.ADM2,
                    KeyReference.LOCAL_PIN1
            ).inOrder()
        }
    }

    @Test
    fun execute_arrReferenceInsufficientSecurity_usesPinStatusTemplateEntries() {
        runBlocking {
            cacheFcp(FCP_ARR_REF_RECORD4)
            prepareCurrentDirectoryFcp(FCP_ADF_PIN_STATUS_USAGE_QUALIFIERS)
            every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                    hexStringToByteArray(PATH_ADF + FID_ARR))) } returns
                    Response(hexStringToByteArray("9000"))
            every { cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x04, 0x04, 0x100))
                    } returns Response(hexStringToByteArray("6982"))

            val outcome = useCase.execute(0, FILE_ID)

            assertThat(outcome.failure).isNull()
            assertThat(outcome.exploreKeyReferenceOptions).containsExactly(
                    KeyReference.APPLICATION_PIN1,
                    KeyReference.ADM1
            ).inOrder()
        }
    }

    @Test
    fun execute_arrReferenceInsufficientSecurity_usesMultiBytePsDo() {
        runBlocking {
            cacheFcp(FCP_ARR_REF_RECORD4)
            prepareCurrentDirectoryFcp(FCP_ADF_PIN_STATUS_MULTI_BYTE_PS)
            every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                    hexStringToByteArray(PATH_ADF + FID_ARR))) } returns
                    Response(hexStringToByteArray("9000"))
            every { cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x04, 0x04, 0x100))
                    } returns Response(hexStringToByteArray("6982"))

            val outcome = useCase.execute(0, FILE_ID)

            assertThat(outcome.failure).isNull()
            assertThat(outcome.exploreKeyReferenceOptions).containsExactly(
                    KeyReference.APPLICATION_PIN1,
                    KeyReference.UNIVERSAL_PIN
            ).inOrder()
        }
    }

    @Test
    fun execute_arrReferenceInsufficientSecurityWithoutC6_returnsAccessKeysUnavailable() {
        runBlocking {
            cacheFcp(FCP_ARR_REF_RECORD4)
            prepareCurrentDirectoryFcp(FCP_ADF_NO_PIN_STATUS)
            every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x0C,
                    hexStringToByteArray(PATH_ADF + FID_ARR))) } returns
                    Response(hexStringToByteArray("9000"))
            every { cardIoMock.transmit(Command(Iso7816.INS_READ_RECORD, 0x04, 0x04, 0x100))
                    } returns Response(hexStringToByteArray("6982"))

            val outcome = useCase.execute(0, FILE_ID)

            assertThat(outcome.failure)
                    .isEqualTo(EditAccessUseCase.Failure.ARR_ACCESS_KEYS_UNAVAILABLE)
        }
    }

    @Test
    fun execute_noSecurityAttributes_returnsUnavailable() {
        runBlocking {
            cacheFcp("62028200")

            val outcome = useCase.execute(0, FILE_ID)

            assertThat(outcome.failure)
                    .isEqualTo(EditAccessUseCase.Failure.SECURITY_ATTRIBUTES_UNAVAILABLE)
        }
    }

    private fun cacheFcp(fcp: String) {
        coEvery { cacheIoMock.get(ICCID, AID, PATH_ADF, FID_AD) } returns
                SelectResponse(
                        ICCID,
                        AID,
                        PATH_ADF,
                        FID_AD,
                        hexStringToByteArray(fcp),
                        Result.SW_NORMAL
                )
    }

    private suspend fun prepareCurrentDirectoryFcp(fcp: String) {
        every { cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE, 0x08, 0x04,
                hexStringToByteArray(PATH_ADF))) } returns
                Response(hexStringToByteArray(fcp + "9000"))
        CurrentDirectoryFcpUseCase(ApplicationProvider.getApplicationContext())
            .prepareForDirectory(0, FILE_ID.aid, FILE_ID.path)
    }
}
