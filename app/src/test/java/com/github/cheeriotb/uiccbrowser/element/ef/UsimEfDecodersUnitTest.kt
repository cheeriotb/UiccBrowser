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
        private const val IMSI = "0899100700002028F1"
        private const val KEYS =
                "07" +
                "00112233445566778899AABBCCDDEEFF" +
                "FFEEDDCCBBAA99887766554433221100"
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
        assertThat(element.subElements[0].label)
                .isEqualTo(resources.getString(R.string.language_code_label))
        assertThat(element.subElements[0].toString()).isEqualTo("656E (en)")
        assertThat(element.subElements[1].toString()).isEqualTo("FFFF (Unused)")
    }

    @Test
    fun decodeLi_oddSize_returnsNull() {
        val element = UsimEfDecoders.decodeLi(resources, hexStringToByteArray("656EFF"))

        assertThat(element).isNull()
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
                .isEqualTo("99100700002028F1 (90170000002821)")
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
        assertThat(element.subElements[0].toString()).isEqualTo("1")
        assertThat(element.subElements[0].subElements[0].toString())
                .isEqualTo("130062 (MCC 310, MNC 260)")
        assertThat(element.subElements[0].subElements[1].toString())
                .isEqualTo("0321 (E-UTRAN, UTRAN, GSM)")
        assertThat(element.subElements[1].subElements[0].toString()).isEqualTo("FFFFFF (Unused)")
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
    fun efDecoderRegistry_usimEfDecodersAreRegistered() {
        val registered = listOf(
                FileId.EF_USIM_LI,
                FileId.EF_USIM_IMSI,
                FileId.EF_USIM_KEYS,
                FileId.EF_USIM_KEYS_PS,
                FileId.EF_USIM_HPPLMN,
                FileId.EF_USIM_ACM_MAX,
                FileId.EF_USIM_PLMN_W_ACT
        )

        registered.forEach { fileId ->
            assertThat(EfDecoderRegistry.has(AID_USIM, FileId.PATH_ADF + fileId)).isTrue()
            assertThat(EfDecoderRegistry.find(AID_USIM, FileId.PATH_ADF + fileId)).isNotNull()
        }
    }
}
