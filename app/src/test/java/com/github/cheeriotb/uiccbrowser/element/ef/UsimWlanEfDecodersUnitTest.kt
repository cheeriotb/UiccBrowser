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
import com.github.cheeriotb.uiccbrowser.element.EfDecoderRegistry
import com.github.cheeriotb.uiccbrowser.repository.FileId
import com.github.cheeriotb.uiccbrowser.util.hexStringToByteArray
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UsimWlanEfDecodersUnitTest {
    private lateinit var resources: Resources

    companion object {
        private const val AID_USIM = AppTemplate.RID + AppTemplate.APP_USIM
        private const val PLMN_LIST = "130062" +
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
        private const val WRI = "8005757365723181040102030482020001FFFF"
    }

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    }

    @Test
    fun decodePseudo_validData_splitsLengthValueAndUnusedStorage() {
        val bytes = hexStringToByteArray("0005616C696365FFFF")
        val element = UsimWlanEfDecoders.decodePseudo(resources, bytes)

        assertThat(element).isNotNull()
        assertThat(element!!.subElements).hasSize(3)
        assertThat(element.subElements[0].toString()).isEqualTo("0005 (5)")
        assertThat(element.subElements[1].toString()).isEqualTo("616C696365 (alice)")
        assertThat(element.subElements[2].label).isEqualTo("Unused")
        assertThat(element.byteArray).isEqualTo(bytes)
    }

    @Test
    fun decodePlmnLists_validData_returnsPriorityOrderedInterpretedPlmns() {
        val bytes = hexStringToByteArray(PLMN_LIST)
        val decoders = listOf(
                UsimWlanEfDecoders::decodeUplmnWlan,
                UsimWlanEfDecoders::decodeOplmnWlan
        )

        decoders.forEach { decoder ->
            val element = decoder(resources, bytes)
            assertThat(element).isNotNull()
            assertThat(element!!.subElements).hasSize(10)
            assertThat(element.subElements[0].label).isEqualTo("I-WLAN PLMN 1")
            assertThat(element.subElements[0].toString()).isEqualTo("130062 (MCC 310, MNC 260)")
            assertThat(element.subElements[9].toString()).isEqualTo("FFFFFF (Unused)")
            assertThat(element.byteArray).isEqualTo(bytes)
        }
    }

    @Test
    fun decodeWsidRecords_validData_splitsLengthValueAndUnusedStorage() {
        val bytes = hexStringToByteArray("03A1B2C3FFFF")
        val decoders = listOf(
                UsimWlanEfDecoders::decodeUwsidl,
                UsimWlanEfDecoders::decodeOwsidl,
                UsimWlanEfDecoders::decodeHwsidl
        )

        decoders.forEach { decoder ->
            val element = decoder(resources, bytes)
            assertThat(element).isNotNull()
            assertThat(element!!.subElements).hasSize(3)
            assertThat(element.subElements[0].toString()).isEqualTo("03 (3)")
            assertThat(element.subElements[1].toString()).isEqualTo("A1B2C3")
            assertThat(element.subElements[2].label).isEqualTo("Unused")
            assertThat(element.byteArray).isEqualTo(bytes)
        }
    }

    @Test
    fun decodeWri_validData_splitsEachTlvAndUnusedStorage() {
        val bytes = hexStringToByteArray(WRI)
        val element = UsimWlanEfDecoders.decodeWri(resources, bytes)

        assertThat(element).isNotNull()
        assertThat(element!!.subElements).hasSize(4)
        assertThat(element.subElements[0].label).isEqualTo("Reauthentication identity")
        assertThat(element.subElements[0].subElements).hasSize(3)
        assertThat(element.subElements[0].subElements[0].toString()).isEqualTo("80")
        assertThat(element.subElements[0].subElements[1].toString()).isEqualTo("05 (5)")
        assertThat(element.subElements[0].subElements[2].toString()).isEqualTo("7573657231 (user1)")
        assertThat(element.subElements[1].label).isEqualTo("Master key")
        assertThat(element.subElements[2].label).isEqualTo("Counter")
        assertThat(element.subElements[3].label).isEqualTo("Unused")
        assertThat(element.byteArray).isEqualTo(bytes)
    }

    @Test
    fun decodeIndicators_validData_returnsDedicatedInterpretations() {
        val wehplmnpi = UsimWlanEfDecoders.decodeWehplmnpi(resources, byteArrayOf(0x02))
        val whpi = UsimWlanEfDecoders.decodeWhpi(resources, byteArrayOf(0x01))
        val hplmndai = UsimWlanEfDecoders.decodeHplmndai(resources, byteArrayOf(0x01))

        assertThat(wehplmnpi!!.subElements.single().toString())
                .isEqualTo("02 (Display all the available EHPLMNs)")
        assertThat(whpi!!.subElements.single().toString())
                .isEqualTo("01 (Attempt registration on the I-WLAN home network)")
        assertThat(hplmndai!!.subElements.single().toString()).isEqualTo("01 (Enabled)")
    }

    @Test
    fun decodeWlrplmn_validData_returnsInterpretedPlmnInConstructedElement() {
        val element = UsimWlanEfDecoders.decodeWlrplmn(
                resources, hexStringToByteArray("130062")
        )

        assertThat(element).isNotNull()
        assertThat(element!!.subElements).hasSize(1)
        assertThat(element.subElements.single().label).isEqualTo("I-WLAN last registered PLMN")
        assertThat(element.subElements.single().toString()).isEqualTo("130062 (MCC 310, MNC 260)")
    }

    @Test
    fun decoders_invalidData_returnNull() {
        assertThat(UsimWlanEfDecoders.decodePseudo(resources, hexStringToByteArray("000341")))
                .isNull()
        assertThat(UsimWlanEfDecoders.decodeUplmnWlan(resources, ByteArray(27))).isNull()
        assertThat(UsimWlanEfDecoders.decodeOplmnWlan(resources, ByteArray(31))).isNull()
        assertThat(UsimWlanEfDecoders.decodeUwsidl(resources, hexStringToByteArray("0341")))
                .isNull()
        assertThat(UsimWlanEfDecoders.decodeWri(resources, hexStringToByteArray("800081008200")))
                .isNotNull()
        assertThat(UsimWlanEfDecoders.decodeWri(resources, hexStringToByteArray("810080008200")))
                .isNull()
        assertThat(UsimWlanEfDecoders.decodeWehplmnpi(resources, ByteArray(2))).isNull()
        assertThat(UsimWlanEfDecoders.decodeWlrplmn(resources, ByteArray(2))).isNull()
    }

    @Test
    fun registry_containsAllWlanDecoders() {
        val fileIds = listOf(
                FileId.EF_WLAN_PSEUDO, FileId.EF_WLAN_UPLMN, FileId.EF_WLAN_OPLMN,
                FileId.EF_WLAN_UWSIDL, FileId.EF_WLAN_OWSIDL, FileId.EF_WLAN_WRI,
                FileId.EF_WLAN_HWSIDL, FileId.EF_WLAN_WEHPLMNPI, FileId.EF_WLAN_WHPI,
                FileId.EF_WLAN_WLRPLMN, FileId.EF_WLAN_HPLMNDAI
        )

        fileIds.forEach { fileId ->
            val path = FileId.PATH_ADF + FileId.DF_WLAN + fileId
            assertThat(EfDecoderRegistry.has(AID_USIM, path)).isTrue()
            assertThat(EfDecoderRegistry.find(AID_USIM, path)).isNotNull()
        }
    }
}
