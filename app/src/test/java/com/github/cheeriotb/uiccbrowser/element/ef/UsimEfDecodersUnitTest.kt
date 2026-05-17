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
    fun decodeFixedLengthFiles_invalidSize_returnsNull() {
        assertThat(UsimEfDecoders.decodeSpn(resources, hexStringToByteArray("00"))).isNull()
        assertThat(UsimEfDecoders.decodePuct(resources, hexStringToByteArray("555344"))).isNull()
        assertThat(UsimEfDecoders.decodeAcc(resources, hexStringToByteArray("80"))).isNull()
        assertThat(UsimEfDecoders.decodeLoci(resources, hexStringToByteArray("0102"))).isNull()
        assertThat(UsimEfDecoders.decodeAd(resources, hexStringToByteArray("0102"))).isNull()
    }

    @Test
    fun efDecoderRegistry_usimEfDecodersAreRegistered() {
        val registered = listOf(
                FileId.EF_USIM_LI,
                FileId.EF_USIM_IMSI,
                FileId.EF_USIM_KEYS,
                FileId.EF_USIM_KEYS_PS,
                FileId.EF_USIM_HPPLMN,
                FileId.EF_USIM_ACM_MAX,
                FileId.EF_USIM_UST,
                FileId.EF_USIM_GID1,
                FileId.EF_USIM_GID2,
                FileId.EF_USIM_SPN,
                FileId.EF_USIM_PUCT,
                FileId.EF_USIM_CBMI,
                FileId.EF_USIM_PLMN_W_ACT,
                FileId.EF_USIM_ACC,
                FileId.EF_USIM_FPLMN,
                FileId.EF_USIM_LOCI,
                FileId.EF_USIM_AD
        )

        registered.forEach { fileId ->
            assertThat(EfDecoderRegistry.has(AID_USIM, FileId.PATH_ADF + fileId)).isTrue()
            assertThat(EfDecoderRegistry.find(AID_USIM, FileId.PATH_ADF + fileId)).isNotNull()
        }
    }
}
