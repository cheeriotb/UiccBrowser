/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.element.ef

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.github.cheeriotb.uiccbrowser.R
import com.github.cheeriotb.uiccbrowser.element.EfDecoderRegistry
import com.github.cheeriotb.uiccbrowser.element.PrimitiveElement
import com.github.cheeriotb.uiccbrowser.repository.FileId
import com.github.cheeriotb.uiccbrowser.util.hexStringToByteArray
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UsimEfDecodersUnitTest {

    private lateinit var resources: Resources

    companion object {
        private const val AID_USIM = AppTemplate.RID + AppTemplate.APP_USIM
        private const val IMSI = "0819100700002028F1"
        private const val KEYS =
                "07" +
                "00112233445566778899AABBCCDDEEFF" +
                "FFEEDDCCBBAA99887766554433221100"
        private const val SPN = "03" + "54657374204F70657261746F72" + "FFFFFF"
        private const val PLMN_W_ACT =
                "1300620321" +
                "FFFFFF0000" +
                "FFFFFF0000" +
                "FFFFFF0000" +
                "FFFFFF0000" +
                "FFFFFF0000" +
                "FFFFFF0000" +
                "FFFFFF0000"
        private const val DIALING_RECORD =
                "54657374FFFFFFFFFFFF" +
                "05" +
                "81" +
                "2143FFFFFFFFFFFFFFFF" +
                "02" +
                "03"
        private const val SMSP_RECORD =
                "54657374FFFFFFFFFFFF" +
                "FF" +
                "FFFFFFFFFFFFFFFFFFFFFFFF" +
                "FFFFFFFFFFFFFFFFFFFFFFFF" +
                "00" +
                "00" +
                "AA"
        private val smsRecord = "01" + "00".repeat(175)
        private val smsrRecord = "02" + "00".repeat(29)
    }

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    }

    @Test
    fun decodeLi_languageEntries_returnsElement() {
        val element = UsimEfDecoders.decodeLi(resources, hexStringToByteArray("656EFFFF"))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_li_label))
        assertThat(element.subElements).hasSize(2)
        assertThat(element.subElements[0].label).isEqualTo("Language code 1")
        assertThat(element.subElements[0].toString()).isEqualTo("656E (en)")
        assertThat(element.subElements[1].label).isEqualTo("Language code 2")
        assertThat(element.subElements[1].toString()).isEqualTo("FFFF (Unused)")
    }

    @Test
    fun decodeLi_oddSize_returnsNull() {
        val element = UsimEfDecoders.decodeLi(resources, hexStringToByteArray("656EFF"))

        assertThat(element).isNull()
    }

    @Test
    fun defaultStringInterpreter_emptyDecodedText_omitsParentheses() {
        val interpretation = PrimitiveElement.defaultStringInterpreter(
                resources,
                hexStringToByteArray("FFFFFF")
        )

        assertThat(interpretation).isEqualTo("FFFFFF")
    }

    @Test
    fun decodeImsi_validData_interpretsImsiDigits() {
        val element = UsimEfDecoders.decodeImsi(resources, hexStringToByteArray(IMSI))

        assertThat(element).isNotNull()
        assertThat(element!!.subElements).hasSize(2)
        assertThat(element.subElements[0].label)
                .isEqualTo(resources.getString(R.string.length_of_imsi_label))
        assertThat(element.subElements[0].toString()).isEqualTo("08")
        assertThat(element.subElements[1].label).isEqualTo(resources.getString(R.string.imsi_label))
        assertThat(element.subElements[1].toString())
                .isEqualTo("19100700002028F1 (10170000002821)")
    }

    @Test
    fun decodeImsi_invalidSize_returnsNull() {
        val element = UsimEfDecoders.decodeImsi(resources, hexStringToByteArray("089910"))

        assertThat(element).isNull()
    }

    @Test
    fun decodeKeys_validData_returnsKsiCkIk() {
        val element = UsimEfDecoders.decodeKeys(resources, hexStringToByteArray(KEYS))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_keys_label))
        assertThat(element.subElements.map { it.label }).containsExactly(
                resources.getString(R.string.key_set_identifier_label),
                resources.getString(R.string.ciphering_key_label),
                resources.getString(R.string.integrity_key_label)
        ).inOrder()
        assertThat(element.subElements[0].toString()).isEqualTo("07 (7)")
        assertThat(element.subElements[1].data).hasLength(16)
        assertThat(element.subElements[2].data).hasLength(16)
    }

    @Test
    fun decodeKeysPs_validData_returnsPsLabels() {
        val element = UsimEfDecoders.decodeKeysPs(resources, hexStringToByteArray(KEYS))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_keys_ps_label))
        assertThat(element.subElements.map { it.label }).containsExactly(
                resources.getString(R.string.key_set_identifier_ps_label),
                resources.getString(R.string.ciphering_key_ps_label),
                resources.getString(R.string.integrity_key_ps_label)
        ).inOrder()
    }

    @Test
    fun decodeKeys_invalidSize_returnsNull() {
        val element = UsimEfDecoders.decodeKeys(resources, hexStringToByteArray("07"))

        assertThat(element).isNull()
    }

    @Test
    fun decodePlmnWAct_validData_interpretsPlmnAndAccessTechnology() {
        val element = UsimEfDecoders.decodePlmnWAct(resources, hexStringToByteArray(PLMN_W_ACT))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_plmn_w_act_label))
        assertThat(element.subElements).hasSize(8)
        assertThat(element.subElements[0].label).isEqualTo("PLMN with Access Technology 1")
        assertThat(element.subElements[0].toString()).isEqualTo("1")
        assertThat(element.subElements[0].subElements[0].toString())
                .isEqualTo("130062 (MCC 310, MNC 260)")
        assertThat(element.subElements[0].subElements[1].toString())
                .isEqualTo("0321 (E-UTRAN, UTRAN, GSM)")
        assertThat(element.subElements[1].label).isEqualTo("PLMN with Access Technology 2")
        assertThat(element.subElements[1].subElements[0].toString()).isEqualTo("FFFFFF (Unused)")
        assertThat(element.subElements[1].subElements[1].toString()).isEqualTo("0000")
    }

    @Test
    fun decodePlmnWAct_shortData_returnsNull() {
        val element = UsimEfDecoders.decodePlmnWAct(resources, hexStringToByteArray("1300620321"))

        assertThat(element).isNull()
    }

    @Test
    fun decodeHpplmn_zeroValue_interpretsAsNoSearchAttempts() {
        val element = UsimEfDecoders.decodeHpplmn(resources, hexStringToByteArray("00"))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_hpplmn_label))
        assertThat(element.subElements).hasSize(1)
        assertThat(element.subElements[0].label)
                .isEqualTo(resources.getString(R.string.time_interval_label))
        assertThat(element.subElements[0].toString())
                .isEqualTo("00 (No higher priority PLMN search attempts)")
    }

    @Test
    fun decodeHpplmn_nonZeroValue_interpretsTimer() {
        val element = UsimEfDecoders.decodeHpplmn(resources, hexStringToByteArray("02"))

        assertThat(element).isNotNull()
        assertThat(element!!.subElements[0].toString())
                .isEqualTo("02 (2n minutes; 4 hours for NB-IoT, EC-GSM-IoT, or Category M1)")
    }

    @Test
    fun decodeAcmMax_validData_interpretsInteger() {
        val element = UsimEfDecoders.decodeAcmMax(resources, hexStringToByteArray("000030"))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_acm_max_label))
        assertThat(element.subElements).hasSize(1)
        assertThat(element.subElements[0].label)
                .isEqualTo(resources.getString(R.string.maximum_value_label))
        assertThat(element.subElements[0].toString()).isEqualTo("000030 (48)")
    }

    @Test
    fun decodeAcm_validData_interpretsAccumulatedUnits() {
        val element = UsimEfDecoders.decodeAcm(resources, hexStringToByteArray("000030"))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_acm_label))
        assertThat(element.subElements.single().toString()).isEqualTo("000030 (48)")
    }

    @Test
    fun decodeCallTimers_validData_interpretSeconds() {
        val ict = UsimEfDecoders.decodeIct(resources, hexStringToByteArray("000030"))
        val oct = UsimEfDecoders.decodeOct(resources, hexStringToByteArray("00003C"))

        assertThat(ict!!.subElements.single().toString()).isEqualTo("000030 (48 seconds)")
        assertThat(oct!!.subElements.single().toString()).isEqualTo("00003C (60 seconds)")
    }

    @Test
    fun decodeIci_validData_interpretsCallInformation() {
        val record =
                "03" + "81" + "2143" + "FF".repeat(8) + "FF" + "FF" +
                "62105121436500" + "000030" + "00" + "000102"

        val element = UsimEfDecoders.decodeIci(resources, hexStringToByteArray(record))

        assertThat(element).isNotNull()
        assertThat(element!!.subElements).hasSize(10)
        assertThat(element.subElements[6].toString())
                .isEqualTo("62105121436500 (26-01-15 12:34:56 +00:00)")
        assertThat(element.subElements[7].toString()).isEqualTo("000030 (48 seconds)")
        assertThat(element.subElements[8].toString()).isEqualTo("00 (Answered)")
        assertThat(element.subElements[9].toString())
                .isEqualTo("000102 (Global phone book: PBR #1, ADN #2)")
    }

    @Test
    fun decodeOci_validData_interpretsCallInformation() {
        val record =
                "03" + "81" + "2143" + "FF".repeat(8) + "FF" + "FF" +
                "62105121436500" + "00003C" + "010203"

        val element = UsimEfDecoders.decodeOci(resources, hexStringToByteArray(record))

        assertThat(element).isNotNull()
        assertThat(element!!.subElements).hasSize(9)
        assertThat(element.subElements[7].toString()).isEqualTo("00003C (60 seconds)")
        assertThat(element.subElements[8].toString())
                .isEqualTo("010203 (Local phone book: PBR #2, ADN #3)")
    }

    @Test
    fun decodeIps_validData_interpretsPairingStatusAndRecordLink() {
        val element = UsimEfDecoders.decodeIps(resources, hexStringToByteArray("4F4B02FF"))

        assertThat(element).isNotNull()
        assertThat(element!!.subElements[0].toString()).isEqualTo("4F4B (Pairing successful)")
        assertThat(element.subElements[1].toString()).isEqualTo("02 (Record #2)")
    }

    @Test
    fun decodeUst_validData_returnsServiceBits() {
        val element = UsimEfDecoders.decodeUst(resources, hexStringToByteArray("0504"))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_ust_label))
        assertThat(element.data).isEqualTo(hexStringToByteArray("0504"))
        assertThat(element.subElements).hasSize(2)
        assertThat(element.subElements[0].label).isEqualTo("Byte 1")
        assertThat(element.subElements[0].subElements).hasSize(8)
        assertThat(element.subElements[0].subElements[0].label)
                .isEqualTo("n°1: Local Phone Book")
        assertThat(element.subElements[0].subElements[0].toString())
                .isEqualTo("Available")
        assertThat(element.subElements[0].subElements[1].label)
                .isEqualTo("n°2: Fixed Dialling Numbers (FDN)")
        assertThat(element.subElements[0].subElements[1].toString())
                .isEqualTo("Not available")
        assertThat(element.subElements[1].label).isEqualTo("Byte 2")
        assertThat(element.subElements[1].subElements[2].label)
                .isEqualTo("n°11: Short Message Status Reports (SMSR)")
        assertThat(element.subElements[1].subElements[2].toString())
                .isEqualTo("Available")
    }

    @Test
    fun decodeGid1_singleInformationElement_wrapsPrimitiveInConstructedElement() {
        val element = UsimEfDecoders.decodeGid1(resources, hexStringToByteArray("0102"))

        assertThat(element).isNotNull()
        assertThat(element!!.primitive).isFalse()
        assertThat(element.label).isEqualTo(resources.getString(R.string.ef_gid1_label))
        assertThat(element.subElements).hasSize(1)
        assertThat(element.subElements[0].label)
                .isEqualTo(resources.getString(R.string.usim_group_identifiers_label))
        assertThat(element.subElements[0].toString()).isEqualTo("0102")
    }

    @Test
    fun decodeGid2_emptyData_returnsNull() {
        val element = UsimEfDecoders.decodeGid2(resources, byteArrayOf())

        assertThat(element).isNull()
    }

    @Test
    fun decodeSpn_validData_returnsDisplayConditionAndName() {
        val element = UsimEfDecoders.decodeSpn(resources, hexStringToByteArray(SPN))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_spn_label))
        assertThat(element.subElements.map { it.label }).containsExactly(
                resources.getString(R.string.display_condition_label),
                resources.getString(R.string.service_provider_name_label)
        ).inOrder()
        assertThat(element.subElements[0].toString()).contains("Registered PLMN name required")
        assertThat(element.subElements[1].toString())
                .isEqualTo("54657374204F70657261746F72FFFFFF (Test Operator)")
    }

    @Test
    fun decodePuct_validData_returnsCurrencyAndPrice() {
        val element = UsimEfDecoders.decodePuct(resources, hexStringToByteArray("5553440121"))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_puct_label))
        assertThat(element.subElements[0].toString()).isEqualTo("555344 (USD)")
        assertThat(element.subElements[1].toString())
                .isEqualTo("0121 (EPPU 17, EX 1, price 170.0)")
    }

    @Test
    fun decodeCbmi_validData_returnsMessageIdentifiers() {
        val element = UsimEfDecoders.decodeCbmi(resources, hexStringToByteArray("1000FFFF"))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_cbmi_label))
        assertThat(element.subElements).hasSize(2)
        assertThat(element.subElements[0].toString()).isEqualTo("1000 (4096)")
        assertThat(element.subElements[1].toString()).isEqualTo("FFFF (Unused)")
    }

    @Test
    fun decodeAcc_validData_interpretsAllocatedClasses() {
        val element = UsimEfDecoders.decodeAcc(resources, hexStringToByteArray("8081"))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_acc_label))
        assertThat(element.subElements).hasSize(1)
        assertThat(element.subElements[0].label)
                .isEqualTo(resources.getString(R.string.access_control_classes_label))
        assertThat(element.subElements[0].toString()).isEqualTo("8081 (00, 07, 15)")
    }

    @Test
    fun decodeFplmn_validData_interpretsPlmns() {
        val element = UsimEfDecoders.decodeFplmn(
                resources,
                hexStringToByteArray("130062FFFFFF42F618FFFFFF")
        )

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_fplmn_label))
        assertThat(element.subElements).hasSize(4)
        assertThat(element.subElements[0].label).isEqualTo("PLMN 1")
        assertThat(element.subElements[0].toString()).isEqualTo("130062 (MCC 310, MNC 260)")
        assertThat(element.subElements[1].label).isEqualTo("PLMN 2")
        assertThat(element.subElements[1].toString()).isEqualTo("FFFFFF (Unused)")
        assertThat(element.subElements[2].label).isEqualTo("PLMN 3")
        assertThat(element.subElements[2].toString()).isEqualTo("42F618 (MCC 246, MNC 81)")
    }

    @Test
    fun decodeLoci_validData_returnsFields() {
        val element = UsimEfDecoders.decodeLoci(
                resources,
                hexStringToByteArray("010203041300620001FF01")
        )

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_loci_label))
        assertThat(element.subElements.map { it.label }).containsExactly(
                resources.getString(R.string.tmsi_label),
                resources.getString(R.string.location_area_information_label),
                resources.getString(R.string.rfu_label),
                resources.getString(R.string.location_update_status_label)
        ).inOrder()
        assertThat(element.subElements[1].toString())
                .isEqualTo("1300620001 (130062 (MCC 310, MNC 260), LAC 0001)")
        assertThat(element.subElements[3].toString()).isEqualTo("01 (Not updated)")
    }

    @Test
    fun decodeAd_validData_returnsFields() {
        val element = UsimEfDecoders.decodeAd(resources, hexStringToByteArray("01001102FF"))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_ad_label))
        assertThat(element.subElements).hasSize(4)
        assertThat(element.subElements[0].toString())
                .isEqualTo("01 (Normal operation + specific facilities)")
        assertThat(element.subElements[1].toString())
                .isEqualTo("0011 (Ciphering indicator, 5G ProSe)")
        assertThat(element.subElements[2].toString()).isEqualTo("02 (2 digits)")
        assertThat(element.subElements[3].label).isEqualTo(resources.getString(R.string.rfu_label))
    }

    @Test
    fun decodeCbmid_validData_returnsMessageIdentifiers() {
        val element = UsimEfDecoders.decodeCbmid(resources, hexStringToByteArray("1000FFFF"))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_cbmid_label))
        assertThat(element.subElements).hasSize(2)
        assertThat(element.subElements[0].toString()).isEqualTo("1000 (4096)")
        assertThat(element.subElements[1].toString()).isEqualTo("FFFF (Unused)")
    }

    @Test
    fun decodeEcc_validData_returnsEmergencyCallCodeAlphaIdentifierAndServiceCategory() {
        val element = UsimEfDecoders.decodeEcc(
                resources,
                hexStringToByteArray("112FFF54657374FFFF01")
        )

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_ecc_label))
        assertThat(element.subElements).hasSize(3)
        assertThat(element.subElements[0].toString()).isEqualTo("112FFF (112)")
        assertThat(element.subElements[1].label).isEqualTo("Alpha Identifier")
        assertThat(element.subElements[1].toString()).isEqualTo("54657374FFFF (Test)")
        assertThat(element.subElements[2].label).isEqualTo("Service Category")
    }

    @Test
    fun decodeEcc_emptyAlternativeRepresentations_omitParentheses() {
        val element = UsimEfDecoders.decodeEcc(
                resources,
                hexStringToByteArray("FFFFFFFFFFFFFFFFFFFF01")
        )

        assertThat(element).isNotNull()
        assertThat(element!!.subElements[0].toString()).isEqualTo("FFFFFF")
        assertThat(element.subElements[1].toString()).isEqualTo("FFFFFFFFFFFFFF")
    }

    @Test
    fun decodeCbmir_validData_returnsIdentifierRanges() {
        val element = UsimEfDecoders.decodeCbmir(resources, hexStringToByteArray("100010FF"))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_cbmir_label))
        assertThat(element.subElements).hasSize(1)
        assertThat(element.subElements[0].label).isEqualTo("CB Message Identifier range 1")
        assertThat(element.subElements[0].subElements[0].toString()).isEqualTo("1000 (4096)")
        assertThat(element.subElements[0].subElements[1].toString()).isEqualTo("10FF (4351)")
    }

    @Test
    fun decodePsloci_validData_returnsPacketSwitchedLocation() {
        val element = UsimEfDecoders.decodePsloci(
                resources,
                hexStringToByteArray("0102030405060713006200010201")
        )

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_psloci_label))
        assertThat(element.subElements.map { it.label }).containsExactly(
                resources.getString(R.string.p_tmsi_label),
                resources.getString(R.string.p_tmsi_signature_value_label),
                resources.getString(R.string.routing_area_information_label),
                resources.getString(R.string.routing_area_update_status_label)
        ).inOrder()
        assertThat(element.subElements[3].toString()).isEqualTo("01 (Not updated)")
    }

    @Test
    fun decodeDiallingNumberFiles_validData_returnCommonFields() {
        val fdn = UsimEfDecoders.decodeFdn(resources, hexStringToByteArray(DIALING_RECORD))
        val msisdn = UsimEfDecoders.decodeMsisdn(resources, hexStringToByteArray(DIALING_RECORD))
        val sdn = UsimEfDecoders.decodeSdn(resources, hexStringToByteArray(DIALING_RECORD))

        listOf(fdn, msisdn, sdn).forEach { element ->
            assertThat(element).isNotNull()
            assertThat(element!!.subElements).hasSize(6)
            assertThat(element.subElements[0].toString()).isEqualTo("54657374FFFFFFFFFFFF (Test)")
            assertThat(element.subElements[3].toString()).isEqualTo("2143FFFFFFFFFFFFFFFF (1234)")
        }
        assertThat(fdn!!.subElements[5].label).isEqualTo("Extension2 Record Identifier")
        assertThat(msisdn!!.subElements[5].label).isEqualTo("Extension5 Record Identifier")
        assertThat(sdn!!.subElements[5].label).isEqualTo("Extension3 Record Identifier")
    }

    @Test
    fun decodeSms_validData_returnsStatusAndTpdu() {
        val element = UsimEfDecoders.decodeSms(resources, hexStringToByteArray(smsRecord))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_sms_label))
        assertThat(element.subElements).hasSize(2)
        assertThat(element.subElements[0].toString()).isEqualTo("01 (Received message, read)")
        assertThat(element.subElements[1].data).hasLength(175)
    }

    @Test
    fun decodeSmsp_validData_returnsSmsParameters() {
        val element = UsimEfDecoders.decodeSmsp(resources, hexStringToByteArray(SMSP_RECORD))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_smsp_label))
        assertThat(element.subElements).hasSize(7)
        assertThat(element.subElements[0].toString()).isEqualTo("54657374FFFFFFFFFFFF (Test)")
        assertThat(element.subElements[6].label).isEqualTo("Validity period")
    }

    @Test
    fun decodeSmss_validData_returnsMessageReferenceAndMemoryFlag() {
        val element = UsimEfDecoders.decodeSmss(resources, hexStringToByteArray("0501"))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_smss_label))
        assertThat(element.subElements[0].toString()).isEqualTo("05 (5)")
        assertThat(element.subElements[1].toString()).isEqualTo("01 (Memory capacity exceeded)")
    }

    @Test
    fun decodeExtensionRecords_validData_returnRecordFields() {
        val bytes = hexStringToByteArray("020102030405060708090A0B0C")

        listOf(
                UsimEfDecoders.decodeExt2(resources, bytes),
                UsimEfDecoders.decodeExt3(resources, bytes),
                UsimEfDecoders.decodeExt5(resources, bytes)
        ).forEach { element ->
            assertThat(element).isNotNull()
            assertThat(element!!.subElements.map { it.label }).containsExactly(
                    resources.getString(R.string.record_type_label),
                    resources.getString(R.string.extension_data_label),
                    resources.getString(R.string.identifier_label)
            ).inOrder()
        }
    }

    @Test
    fun decodeSmsr_validData_returnsRecordIdentifierAndStatusReport() {
        val element = UsimEfDecoders.decodeSmsr(resources, hexStringToByteArray(smsrRecord))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_smsr_label))
        assertThat(element.subElements[0].toString()).isEqualTo("02 (Record #2)")
        assertThat(element.subElements[1].data).hasLength(29)
    }

    @Test
    fun decodeCcp2_singleInformationElement_wrapsPrimitiveInConstructedElement() {
        val element = UsimEfDecoders.decodeCcp2(resources, hexStringToByteArray("00".repeat(15)))

        assertThat(element).isNotNull()
        assertThat(element!!.primitive).isFalse()
        assertThat(element.label).isEqualTo(resources.getString(R.string.ef_ccp2_label))
        assertThat(element.subElements).hasSize(1)
        assertThat(element.subElements[0].label)
                .isEqualTo(
                        resources.getString(R.string.bearer_capability_information_element_label))
    }

    @Test
    fun decodeAdditionalUsimEf_validData_returnsExpectedChildren() {
        val emlpp = UsimEfDecoders.decodeEmlpp(resources, hexStringToByteArray("1C0C"))
        assertThat(emlpp).isNotNull()
        assertThat(emlpp!!.subElements[0].toString())
                .isEqualTo("1C (Priority levels 0, 1, 2)")

        val bdn = UsimEfDecoders.decodeBdn(resources, hexStringToByteArray(DIALING_RECORD + "04"))
        assertThat(bdn).isNotNull()
        assertThat(bdn!!.subElements).hasSize(7)
        assertThat(bdn.subElements[6].toString()).isEqualTo("04 (Record #4)")

        val mwis = UsimEfDecoders.decodeMwis(resources, hexStringToByteArray("0502010000"))
        assertThat(mwis).isNotNull()
        assertThat(mwis!!.subElements[0].toString())
                .isEqualTo("05 (Voicemail, Electronic Mail)")

        val opl = UsimEfDecoders.decodeOpl(resources, hexStringToByteArray("1300620000FFFE01"))
        assertThat(opl).isNotNull()
        assertThat(opl!!.subElements[0].subElements[0].toString())
                .isEqualTo("130062 (MCC 310, MNC 260)")
    }

    @Test
    fun decodePnn_validData_labelsNetworkNameTags() {
        val element = UsimEfDecoders.decodePnn(
                resources,
                hexStringToByteArray("4305843350900AFFFFFFFFFFFF")
        )

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_pnn_label))
        assertThat(element.subElements).hasSize(1)
        assertThat(element.subElements[0].label).isEqualTo("Full name for network '43'")
        assertThat(element.subElements[0].toString()).contains("GSM default alphabet")
    }

    @Test
    fun decodeSpdi_validData_labelsPlmnList() {
        val element = UsimEfDecoders.decodeSpdi(
                resources,
                hexStringToByteArray("A307800544F001FFFF")
        )

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_spdi_label))
        assertThat(element.subElements).hasSize(1)
        assertThat(element.subElements[0].label)
                .isEqualTo("Service Provider Display Information 'A3'")
        assertThat(element.subElements[0].subElements[0].label)
                .isEqualTo("Service Provider PLMN List '80'")
        assertThat(element.subElements[0].subElements[0].subElements[0].label)
                .isEqualTo("Service Provider PLMN 1")
        assertThat(element.subElements[0].subElements[0].subElements[0].toString())
                .isEqualTo("44F001 (MCC 440, MNC 10)")
    }

    @Test
    fun decodeNetpar_validData_labelsFddCellInformation() {
        val element = UsimEfDecoders.decodeNetpar(
                resources,
                hexStringToByteArray("A1058003042A01" + "FF".repeat(39))
        )

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_netpar_label))
        assertThat(element.subElements).hasSize(1)
        assertThat(element.subElements[0].label).isEqualTo("FDD Cell Information 'A1'")
        assertThat(element.subElements[0].subElements[0].label)
                .isEqualTo("Intra Frequency Information '80'")
        assertThat(element.subElements[0].subElements[0].toString()).isEqualTo("042A01")
    }

    @Test
    fun decodeTask044TransparentFiles_validData_returnsElements() {
        val ehplmn = UsimEfDecoders.decodeEhplmn(resources, hexStringToByteArray("44F001"))
        val epsloci = UsimEfDecoders.decodeEpsloci(
                resources,
                hexStringToByteArray("00".repeat(12) + "44F0010001" + "02")
        )
        val ncpIp = UsimEfDecoders.decodeNcpIp(
                resources,
                hexStringToByteArray("80076D6D732E61706E830621200A000000")
        )

        assertThat(ehplmn).isNotNull()
        assertThat(ehplmn!!.subElements[0].label).isEqualTo("EHPLMN 1")
        assertThat(ehplmn.subElements[0].toString()).isEqualTo("44F001 (MCC 440, MNC 10)")
        assertThat(epsloci).isNotNull()
        assertThat(epsloci!!.subElements[1].label)
                .isEqualTo(resources.getString(R.string.last_visited_registered_tai_label))
        assertThat(epsloci.subElements[2].toString()).isEqualTo("02 (Roaming not allowed)")
        assertThat(ncpIp).isNotNull()
        assertThat(ncpIp!!.subElements[0].label).isEqualTo("Access Point Name '80'")
        assertThat(ncpIp.subElements[1].subElements[0].toString())
                .isEqualTo("21 (IPv4 address range)")
    }

    @Test
    fun decodeTask044TlvFiles_validData_returnsDedicatedLabels() {
        val mmsicp = UsimEfDecoders.decodeMmsicp(
                resources,
                hexStringToByteArray("AB0A8001018105687474703A")
        )
        val mmsup = UsimEfDecoders.decodeMmsup(
                resources,
                hexStringToByteArray("A00B8001018104546573748200")
        )
        val epsnsc = UsimEfDecoders.decodeEpsnsc(
                resources,
                hexStringToByteArray("A0348001038120" + "00".repeat(32) +
                        "820400000001830400000002840100")
        )

        assertThat(mmsicp).isNotNull()
        assertThat(mmsicp!!.subElements[0].label)
                .isEqualTo("MMS Connectivity Parameters 'AB'")
        assertThat(mmsicp.subElements[0].subElements[0].label)
                .isEqualTo("MMS Implementation '80'")
        assertThat(mmsup).isNotNull()
        assertThat(mmsup!!.subElements[0].subElements[1].toString())
                .isEqualTo("54657374 (Test)")
        assertThat(epsnsc).isNotNull()
        assertThat(epsnsc!!.subElements[0].label).isEqualTo("EPS NAS Security Context 'A0'")
        assertThat(epsnsc.subElements[0].subElements[0].toString()).isEqualTo("03 (3)")
    }

    @Test
    fun decodeTask044LinearFixedFiles_validData_returnsElements() {
        val ext8 = UsimEfDecoders.decodeExt8(resources, hexStringToByteArray("020548656C6C6FFF"))
        val nia = UsimEfDecoders.decodeNia(resources, hexStringToByteArray("0154657374FFFF"))
        val vgcs = UsimEfDecoders.decodeVgcs(resources, hexStringToByteArray("2143FFFF"))
        val vgcsca = UsimEfDecoders.decodeVgcsca(resources, hexStringToByteArray("0003"))
        val gbabp = UsimEfDecoders.decodeGbabp(
                resources,
                hexStringToByteArray("04AABBCCDD03627469083230323630353233")
        )

        assertThat(ext8).isNotNull()
        assertThat(ext8!!.subElements[2].toString()).isEqualTo("FF (Unused)")
        assertThat(nia).isNotNull()
        assertThat(nia!!.subElements[1].toString()).isEqualTo("54657374FFFF (Test)")
        assertThat(vgcs).isNotNull()
        assertThat(vgcs!!.subElements[0].toString()).isEqualTo("2143FFFF (1234)")
        assertThat(vgcsca).isNotNull()
        assertThat(vgcsca!!.subElements[1].toString()).isEqualTo("03 (GSM A5/3)")
        assertThat(gbabp).isNotNull()
        assertThat(gbabp!!.subElements[3].toString()).isEqualTo("627469 (bti)")
    }

    @Test
    fun decodeTask045Nasconfig_validData_labelsKnownTags() {
        val element = UsimEfDecoders.decodeNasconfig(
                resources,
                hexStringToByteArray("8001018101008201019B0101")
        )

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_nasconfig_label))
        assertThat(element.subElements.map { it.label }).containsExactly(
                "NAS signalling priority configuration '80'",
                "NMO I Behaviour '81'",
                "Attach with IMSI '82'",
                "MUSIM paging restriction support '9B'"
        ).inOrder()
        assertThat(element.subElements[0].toString()).isEqualTo("01 (Enabled)")
        assertThat(element.subElements[1].toString()).isEqualTo("00 (Disabled)")
    }

    @Test
    fun decodeTask045UriAndIariFiles_validData_returnsDecodedStrings() {
        val uicciari = UsimEfDecoders.decodeUicciari(
                resources,
                hexStringToByteArray("800D75726E3A746573743A69617269")
        )
        val fdnuri = UsimEfDecoders.decodeFdnuri(
                resources,
                hexStringToByteArray("800E7369703A6131406578616D706C65" + "54657374FFFF")
        )

        assertThat(uicciari).isNotNull()
        assertThat(uicciari!!.subElements[0].label).isEqualTo("UICC IARI '80'")
        assertThat(uicciari.subElements[0].toString()).contains("urn:test:iari")
        assertThat(fdnuri).isNotNull()
        assertThat(fdnuri!!.subElements[0].subElements[0].label).isEqualTo("URI '80'")
        assertThat(fdnuri.subElements[1].toString()).isEqualTo("54657374FFFF (Test)")
    }

    @Test
    fun decodeTask045StructuredFiles_validData_returnsFields() {
        val pws = UsimEfDecoders.decodePws(resources, hexStringToByteArray("03FFFF"))
        val ial = UsimEfDecoders.decodeIal(resources, hexStringToByteArray("3510203040506070"))
        val epdgId = UsimEfDecoders.decodeEpdgid(
                resources,
                hexStringToByteArray("800D00657064672E6578616D706C65")
        )
        val epdgSelection = UsimEfDecoders.decodeEpdgselection(
                resources,
                hexStringToByteArray("44F00100010102")
        )
        val tvconfig = UsimEfDecoders.decodeTvconfig(
                resources,
                hexStringToByteArray("44F001A0090102030405066FAD01A1040000FC18")
        )
        val psDataOff = UsimEfDecoders.decodeThreeGppPsDataOff(
                resources,
                hexStringToByteArray("03010000")
        )
        val serviceList = UsimEfDecoders.decodeThreeGppPsDataOffServiceList(
                resources,
                hexStringToByteArray("800A75726E3A696373693A31")
        )
        val earfcn = UsimEfDecoders.decodeEarfcnList(
                resources,
                hexStringToByteArray(
                        "A01A" +
                        "80040000FC18" +
                        "8112000001000002000003000004000005000006"
                )
        )
        val eaka = UsimEfDecoders.decodeEaka(resources, hexStringToByteArray("01FFFF"))
        val ocst = UsimEfDecoders.decodeOcst(resources, hexStringToByteArray("018003020169"))
        val gba = UsimEfDecoders.decodeAcGbauapi(
                resources,
                hexStringToByteArray("800F05A000000087086E61662E74657374")
        )
        val imsdci = UsimEfDecoders.decodeImsdci(resources, hexStringToByteArray("02"))
        val lsp = UsimEfDecoders.decodeOplmnWActLsp(
                resources,
                hexStringToByteArray("0144F001020101")
        )

        assertThat(pws).isNotNull()
        assertThat(pws!!.subElements[0].label).isEqualTo("PWS configuration")
        assertThat(pws.subElements[0].toString()).contains("HPLMN")
        assertThat(ial).isNotNull()
        assertThat(ial!!.subElements[0].label).isEqualTo("IMEI(SV) 1")
        assertThat(epdgId).isNotNull()
        assertThat(epdgId!!.subElements[0].label).isEqualTo("ePDG identifier '80'")
        assertThat(epdgId.subElements[0].subElements[0].toString()).isEqualTo("00 (FQDN)")
        assertThat(epdgId.subElements[0].subElements[1].toString())
                .isEqualTo("657064672E6578616D706C65 (epdg.example)")
        assertThat(epdgSelection).isNotNull()
        assertThat(epdgSelection!!.subElements[0].subElements[0].toString())
                .isEqualTo("44F001 (MCC 440, MNC 10)")
        assertThat(epdgSelection.subElements[0].subElements[2].toString())
                .isEqualTo("01 (Location based FQDN)")
        assertThat(tvconfig).isNotNull()
        assertThat(tvconfig!!.subElements[0].toString()).isEqualTo("44F001 (MCC 440, MNC 10)")
        assertThat(tvconfig.subElements[1].label).isEqualTo("TMGI list 'A0'")
        assertThat(tvconfig.subElements[1].subElements[0].subElements[1].toString())
                .isEqualTo("6FAD (Record #111)")
        assertThat(tvconfig.subElements[2].label).isEqualTo("TV EARFCN list 'A1'")
        assertThat(tvconfig.subElements[2].subElements[0].toString()).isEqualTo("0000FC18 (64536)")
        assertThat(psDataOff).isNotNull()
        assertThat(psDataOff!!.subElements[0].toString()).contains("USSI")
        assertThat(psDataOff.subElements[0].toString()).contains("MMTEL voice")
        assertThat(psDataOff.subElements[1].toString()).contains("USSI")
        assertThat(serviceList).isNotNull()
        assertThat(serviceList!!.subElements[0].label).isEqualTo("3GPP PS Data Off service 1 '80'")
        assertThat(serviceList.subElements[0].toString()).contains("urn:icsi:1")
        assertThat(earfcn).isNotNull()
        assertThat(earfcn!!.subElements[0].label).isEqualTo("EARFCN list entry 1 'A0'")
        assertThat(earfcn.subElements[0].subElements[0].toString()).isEqualTo("0000FC18 (64536)")
        assertThat(earfcn.subElements[0].subElements[1].label)
                .isEqualTo("Geographical Area - Polygon '81'")
        assertThat(earfcn.subElements[0].subElements[1].subElements[0].subElements[0].label)
                .isEqualTo("Latitude")
        assertThat(eaka).isNotNull()
        assertThat(eaka!!.subElements[0].toString()).contains("Enhanced SQN calculation supported")
        assertThat(eaka.subElements[1].label).isEqualTo(resources.getString(R.string.rfu_label))
        assertThat(ocst).isNotNull()
        assertThat(ocst!!.subElements[0].toString()).isEqualTo("01 (Enabled)")
        assertThat(ocst.subElements[1].label)
                .isEqualTo("Operator controlled signal threshold parameters '80'")
        assertThat(ocst.subElements[1].subElements[0].subElements[0].toString())
                .isEqualTo("0201 (E-UTRAN, GSM)")
        assertThat(ocst.subElements[1].subElements[0].subElements[1].toString())
                .isEqualTo("69 (-105 dBm)")
        assertThat(gba).isNotNull()
        assertThat(gba!!.subElements[0].label).isEqualTo("Applet NAF Access Control '80'")
        assertThat(gba.subElements[0].subElements[1].toString()).isEqualTo("A000000087")
        assertThat(gba.subElements[0].subElements[3].toString())
                .isEqualTo("6E61662E74657374 (naf.test)")
        assertThat(imsdci).isNotNull()
        assertThat(imsdci!!.subElements[0].toString()).contains("simultaneous setup")
        assertThat(lsp).isNotNull()
        assertThat(lsp!!.subElements[0].label).isEqualTo("Priority")
        assertThat(lsp.subElements[1].subElements[0].toString())
                .isEqualTo("44F001 (MCC 440, MNC 10)")
        assertThat(lsp.subElements[1].subElements[2].label).isEqualTo("SelectedSATFeature")
    }

    @Test
    fun decodeFixedLengthFiles_invalidSize_returnsNull() {
        assertThat(UsimEfDecoders.decodeSpn(resources, hexStringToByteArray("00"))).isNull()
        assertThat(UsimEfDecoders.decodePuct(resources, hexStringToByteArray("555344"))).isNull()
        assertThat(UsimEfDecoders.decodeAcc(resources, hexStringToByteArray("80"))).isNull()
        assertThat(UsimEfDecoders.decodeLoci(resources, hexStringToByteArray("0102"))).isNull()
        assertThat(UsimEfDecoders.decodeAd(resources, hexStringToByteArray("0102"))).isNull()
        assertThat(UsimEfDecoders.decodeEcc(resources, hexStringToByteArray("112FFF"))).isNull()
        assertThat(UsimEfDecoders.decodeCbmid(resources, hexStringToByteArray("10"))).isNull()
        assertThat(UsimEfDecoders.decodeCbmir(resources, hexStringToByteArray("1000"))).isNull()
        assertThat(UsimEfDecoders.decodePsloci(resources, hexStringToByteArray("0102"))).isNull()
        assertThat(UsimEfDecoders.decodeSms(resources, hexStringToByteArray("00"))).isNull()
        assertThat(UsimEfDecoders.decodeSmss(resources, hexStringToByteArray("00"))).isNull()
        assertThat(UsimEfDecoders.decodeExt2(resources, hexStringToByteArray("00"))).isNull()
        assertThat(UsimEfDecoders.decodeCcp2(resources, hexStringToByteArray("00"))).isNull()
        assertThat(UsimEfDecoders.decodeEmlpp(resources, hexStringToByteArray("00"))).isNull()
        assertThat(UsimEfDecoders.decodeAaem(resources, hexStringToByteArray("0000"))).isNull()
        assertThat(UsimEfDecoders.decodeHiddenkey(resources, hexStringToByteArray("00"))).isNull()
        assertThat(UsimEfDecoders.decodeDck(resources, hexStringToByteArray("00"))).isNull()
        assertThat(UsimEfDecoders.decodeStartHfn(resources, hexStringToByteArray("00"))).isNull()
        assertThat(UsimEfDecoders.decodeThreshold(resources, hexStringToByteArray("00"))).isNull()
        assertThat(UsimEfDecoders.decodeCfis(resources, hexStringToByteArray("00"))).isNull()
        assertThat(UsimEfDecoders.decodeEpsloci(resources, hexStringToByteArray("00"))).isNull()
        assertThat(UsimEfDecoders.decodeVgcss(resources, hexStringToByteArray("00"))).isNull()
    }

    @Test
    fun efDecoderRegistry_usimEfDecodersAreRegistered() {
        val registered = listOf(
                FileId.EF_USIM_LI,
                FileId.EF_USIM_IMSI,
                FileId.EF_USIM_KEYS,
                FileId.EF_USIM_KEYS_PS,
                FileId.EF_USIM_DCK,
                FileId.EF_USIM_HPPLMN,
                FileId.EF_USIM_CNL,
                FileId.EF_USIM_ACM_MAX,
                FileId.EF_USIM_UST,
                FileId.EF_USIM_CBMID,
                FileId.EF_USIM_ECC,
                FileId.EF_USIM_CBMIR,
                FileId.EF_USIM_PSLOCI,
                FileId.EF_USIM_FDN,
                FileId.EF_USIM_SMS,
                FileId.EF_USIM_MSISDN,
                FileId.EF_USIM_SMSP,
                FileId.EF_USIM_SMSS,
                FileId.EF_USIM_SDN,
                FileId.EF_USIM_EXT2,
                FileId.EF_USIM_EXT3,
                FileId.EF_USIM_BDN,
                FileId.EF_USIM_SMSR,
                FileId.EF_USIM_EXT5,
                FileId.EF_USIM_CCP2,
                FileId.EF_USIM_EMLPP,
                FileId.EF_USIM_AAEM,
                FileId.EF_USIM_HIDDENKEY,
                FileId.EF_USIM_EXT4,
                FileId.EF_USIM_CMI,
                FileId.EF_USIM_EST,
                FileId.EF_USIM_ACL,
                FileId.EF_USIM_START_HFN,
                FileId.EF_USIM_THRESHOLD,
                FileId.EF_USIM_OPLMN_W_ACT,
                FileId.EF_USIM_HPLMN_W_ACT,
                FileId.EF_USIM_NETPAR,
                FileId.EF_USIM_PNN,
                FileId.EF_USIM_OPL,
                FileId.EF_USIM_MBDN,
                FileId.EF_USIM_EXT6,
                FileId.EF_USIM_MBI,
                FileId.EF_USIM_MWIS,
                FileId.EF_USIM_CFIS,
                FileId.EF_USIM_EXT7,
                FileId.EF_USIM_SPDI,
                FileId.EF_USIM_MMSN,
                FileId.EF_USIM_GID1,
                FileId.EF_USIM_GID2,
                FileId.EF_USIM_SPN,
                FileId.EF_USIM_PUCT,
                FileId.EF_USIM_CBMI,
                FileId.EF_USIM_PLMN_W_ACT,
                FileId.EF_USIM_ACC,
                FileId.EF_USIM_FPLMN,
                FileId.EF_USIM_LOCI,
                FileId.EF_USIM_AD,
                FileId.EF_USIM_VGCS,
                FileId.EF_USIM_VGCSS,
                FileId.EF_USIM_VBS,
                FileId.EF_USIM_VBSS,
                FileId.EF_USIM_EXT8,
                FileId.EF_USIM_MMSICP,
                FileId.EF_USIM_MMSUP,
                FileId.EF_USIM_MMSUCP,
                FileId.EF_USIM_NIA,
                FileId.EF_USIM_VGCSCA,
                FileId.EF_USIM_VBSCA,
                FileId.EF_USIM_GBABP,
                FileId.EF_USIM_MSK,
                FileId.EF_USIM_MUK,
                FileId.EF_USIM_EHPLMN,
                FileId.EF_USIM_GBANL,
                FileId.EF_USIM_EHPLMNPI,
                FileId.EF_USIM_LRPLMNSI,
                FileId.EF_USIM_NAFKCA,
                FileId.EF_USIM_SPNI,
                FileId.EF_USIM_PNNI,
                FileId.EF_USIM_NCP_IP,
                FileId.EF_USIM_EPSLOCI,
                FileId.EF_USIM_EPSNSC,
                FileId.EF_USIM_UFC,
                FileId.EF_USIM_UICCIARI,
                FileId.EF_USIM_NASCONFIG,
                FileId.EF_USIM_PWS,
                FileId.EF_USIM_FDNURI,
                FileId.EF_USIM_BDNURI,
                FileId.EF_USIM_SDNURI,
                FileId.EF_USIM_IAL,
                FileId.EF_USIM_IPD,
                FileId.EF_USIM_EPDGID,
                FileId.EF_USIM_EPDGSELECTION,
                FileId.EF_USIM_EPDGIDEM,
                FileId.EF_USIM_EPDGSELECTIONEM,
                FileId.EF_USIM_FROMPREFERRED,
                FileId.EF_USIM_3GPPPSDATAOFF,
                FileId.EF_USIM_3GPPPSDATAOFF_SERVICE_LIST,
                FileId.EF_USIM_TVCONFIG,
                FileId.EF_USIM_EARFCN_LIST,
                FileId.EF_USIM_EAKA,
                FileId.EF_USIM_OCST,
                FileId.EF_USIM_AC_GBAUAPI,
                FileId.EF_USIM_IMSDCI,
                FileId.EF_USIM_OPLMN_W_ACT_LSP,
                FileId.EF_USIM_LSPPLMN
        )

        registered.forEach { fileId ->
            assertThat(EfDecoderRegistry.has(AID_USIM, FileId.PATH_ADF + fileId)).isTrue()
            assertThat(EfDecoderRegistry.find(AID_USIM, FileId.PATH_ADF + fileId)).isNotNull()
        }
    }

    @Test
    fun efDecoderRegistry_cyclicUsimEfDecodersAreRegistered() {
        val registered = listOf(
                FileId.EF_USIM_ACM,
                FileId.EF_USIM_ICI,
                FileId.EF_USIM_OCI,
                FileId.EF_USIM_ICT,
                FileId.EF_USIM_OCT,
                FileId.EF_USIM_IPS
        )

        registered.forEach { fileId ->
            assertThat(EfDecoderRegistry.has(AID_USIM, FileId.PATH_ADF + fileId)).isTrue()
            assertThat(EfDecoderRegistry.find(AID_USIM, FileId.PATH_ADF + fileId)).isNotNull()
        }
    }

    @Test
    fun efDecoderRegistry_berTlvUsimEfDecodersAreNotRegistered() {
        val notRegistered = listOf(
                FileId.EF_USIM_IMS_CONFIG_DATA,
                FileId.EF_USIM_XCAP_CONFIG_DATA,
                FileId.EF_USIM_MUDMID_CONFIG_DATA
        )

        notRegistered.forEach { fileId ->
            assertThat(EfDecoderRegistry.has(AID_USIM, FileId.PATH_ADF + fileId)).isFalse()
            assertThat(EfDecoderRegistry.find(AID_USIM, FileId.PATH_ADF + fileId)).isNull()
        }
    }
}
