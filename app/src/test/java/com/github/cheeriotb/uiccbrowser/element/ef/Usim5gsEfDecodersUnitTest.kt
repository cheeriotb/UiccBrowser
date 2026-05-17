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
class Usim5gsEfDecodersUnitTest {

    private lateinit var resources: Resources

    companion object {
        private const val AID_USIM = AppTemplate.RID + AppTemplate.APP_USIM
        private const val SUCI_CALC_INFO = "A00401000201A1088001018103010203"
        private const val OPL5G = "1300620000010000FF01"
    }

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    }

    @Test
    fun decodeSuciCalcInfo_validData_returnsProtectionSchemesAndPublicKeys() {
        val element = Usim5gsEfDecoders.decodeSuciCalcInfo(
                resources,
                hexStringToByteArray(SUCI_CALC_INFO)
        )

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_suci_calc_info_label))
        assertThat(element.subElements).hasSize(2)
        assertThat(element.subElements[0].label)
                .isEqualTo(resources.getString(R.string.protection_scheme_identifier_list_label))
        assertThat(element.subElements[0].subElements).hasSize(2)
        assertThat(element.subElements[0].subElements[0].label).isEqualTo("Protection scheme 1")
        assertThat(element.subElements[0].subElements[0].subElements[0].toString())
                .isEqualTo("01 (1)")
        assertThat(element.subElements[0].subElements[0].subElements[1].toString())
                .isEqualTo("00 (0)")
        assertThat(element.subElements[1].label)
                .isEqualTo(resources.getString(R.string.home_network_public_key_list_label))
        assertThat(element.subElements[1].subElements).hasSize(2)
        assertThat(element.subElements[1].subElements[0].label)
                .isEqualTo(resources.getString(R.string.home_network_public_key_identifier_label))
        assertThat(element.subElements[1].subElements[0].subElements[0].toString())
                .isEqualTo("01")
        assertThat(element.byteArray).isEqualTo(hexStringToByteArray(SUCI_CALC_INFO))
    }

    @Test
    fun decodeOpl5g_validData_returnsIndexedRecords() {
        val element = Usim5gsEfDecoders.decodeOpl5g(resources, hexStringToByteArray(OPL5G))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_opl5g_label))
        assertThat(element.subElements).hasSize(1)
        assertThat(element.subElements[0].label).isEqualTo("Operator PLMN list entry 1")
        assertThat(element.subElements[0].subElements[0].toString())
                .isEqualTo("130062 (MCC 310, MNC 260)")
        assertThat(element.subElements[0].subElements[1].toString()).isEqualTo("000001")
        assertThat(element.subElements[0].subElements[2].toString()).isEqualTo("0000FF")
        assertThat(element.subElements[0].subElements[3].toString()).isEqualTo("01 (1)")
    }

    @Test
    fun decodeInvalidData_returnsNull() {
        assertThat(Usim5gsEfDecoders.decodeSuciCalcInfo(resources, hexStringToByteArray("A100")))
                .isNull()
        assertThat(Usim5gsEfDecoders.decodeOpl5g(resources, hexStringToByteArray("130062")))
                .isNull()
    }

    @Test
    fun efDecoderRegistry_usim5gsEfDecodersAreRegistered() {
        val registered = listOf(
                FileId.EF_USIM_5GS_SUCI_CALC_INFO,
                FileId.EF_USIM_5GS_OPL5G
        )

        registered.forEach { fileId ->
            val path = FileId.PATH_ADF + FileId.DF_USIM_5GS + fileId
            assertThat(EfDecoderRegistry.has(AID_USIM, path)).isTrue()
            assertThat(EfDecoderRegistry.find(AID_USIM, path)).isNotNull()
        }
    }
}
