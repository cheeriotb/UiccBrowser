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
class TelecomEfDecodersUnitTest {
    private lateinit var resources: Resources

    companion object {
        private const val SUME = "85044D656E759E020101D00400010203FFFF"
        private const val ICE_DN = "49434531" + "04912143FFFFFFFFFFFFFFFFFFFF"
        private const val ICE_FF = "800600416C657274810D00416C6C6572677920746F2078FFFF"
        private const val RMA = "0A85044D656E759E020101AABB"
        private const val PSISMSC = "80147369703A736D7363406578616D706C652E636F6D"
        private const val ARR = "8001019000"
    }

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    }

    @Test
    fun decodeSume_validData_splitsComprehensionTlvsAndPadding() {
        val bytes = hexStringToByteArray(SUME)
        val element = TelecomEfDecoders.decodeSume(resources, bytes)

        assertThat(element).isNotNull()
        assertThat(element!!.subElements).hasSize(4)
        assertThat(element.subElements[0].label).isEqualTo("Title Alpha Identifier")
        assertThat(element.subElements[0].subElements).hasSize(3)
        assertThat(element.subElements[0].subElements[0].toString())
                .isEqualTo("85 (Comprehension required)")
        assertThat(element.subElements[0].subElements[1].toString()).isEqualTo("04 (4)")
        assertThat(element.subElements[0].subElements[2].toString()).isEqualTo("4D656E75 (Menu)")
        assertThat(element.subElements[1].label).isEqualTo("Title Icon Identifier")
        assertThat(element.subElements[2].label).isEqualTo("Title Text Attribute")
        assertThat(element.subElements[3].label).isEqualTo("Padding")
        assertThat(element.byteArray).isEqualTo(bytes)
    }

    @Test
    fun decodeArr_validRecord_usesCommonAccessRuleDecoder() {
        val element = TelecomEfDecoders.decodeArr(resources, hexStringToByteArray(ARR))

        assertThat(element).isNotNull()
        assertThat(element!!.subElements).hasSize(2)
    }

    @Test
    fun decodeIceDn_validRecord_splitsAdnFieldsAndInterpretsValues() {
        val bytes = hexStringToByteArray(ICE_DN)
        val element = TelecomEfDecoders.decodeIceDn(resources, bytes)

        assertThat(element).isNotNull()
        assertThat(element!!.subElements).hasSize(6)
        assertThat(element.subElements[0].toString()).isEqualTo("49434531 (ICE1)")
        assertThat(element.subElements[1].toString()).isEqualTo("04 (4)")
        assertThat(element.subElements[2].toString()).isEqualTo("91 (TON: 1, NPI: 1)")
        assertThat(element.subElements[3].toString())
                .isEqualTo("2143FFFFFFFFFFFFFFFF (1234)")
        assertThat(element.subElements[4].toString()).isEqualTo("FF (Unused)")
        assertThat(element.subElements[5].toString()).isEqualTo("FF (Unused)")
        assertThat(element.byteArray).isEqualTo(bytes)
    }

    @Test
    fun decodeIceFf_validRecord_splitsTextStringTlvsAndPadding() {
        val bytes = hexStringToByteArray(ICE_FF)
        val element = TelecomEfDecoders.decodeIceFf(resources, bytes)

        assertThat(element).isNotNull()
        assertThat(element!!.subElements).hasSize(3)
        assertThat(element.subElements[0].label).contains("ICE Free Format Label")
        assertThat(element.subElements[0].subElements[0].toString())
                .isEqualTo("00 (GSM default alphabet)")
        assertThat(element.subElements[0].subElements[1].toString())
                .isEqualTo("416C657274 (Alert)")
        assertThat(element.subElements[1].subElements[1].toString())
                .isEqualTo("416C6C6572677920746F2078 (Allergy to x)")
        assertThat(element.subElements[2].label).isEqualTo("Padding")
        assertThat(element.byteArray).isEqualTo(bytes)
    }

    @Test
    fun decodeIceFf_ucs2Text_interpretsTextUsingDataCodingScheme() {
        val element = TelecomEfDecoders.decodeIceFf(
                resources, hexStringToByteArray("800508004F004B810100")
        )

        assertThat(element).isNotNull()
        assertThat(element!!.subElements[0].subElements[0].toString()).isEqualTo("08 (UCS2)")
        assertThat(element.subElements[0].subElements[1].toString()).isEqualTo("004F004B (OK)")
    }

    @Test
    fun decodeRma_validRecord_splitsLengthComprehensionTlvsAndPadding() {
        val bytes = hexStringToByteArray(RMA)
        val element = TelecomEfDecoders.decodeRma(resources, bytes)

        assertThat(element).isNotNull()
        assertThat(element!!.subElements).hasSize(4)
        assertThat(element.subElements[0].toString()).isEqualTo("0A (10)")
        assertThat(element.subElements[1].label).isEqualTo("Comprehension TLV 1")
        assertThat(element.subElements[2].label).isEqualTo("Comprehension TLV 2")
        assertThat(element.subElements[3].label).isEqualTo("Padding")
        assertThat(element.byteArray).isEqualTo(bytes)
    }

    @Test
    fun decodePsismsc_validRecord_interpretsUtf8Uri() {
        val bytes = hexStringToByteArray(PSISMSC)
        val element = TelecomEfDecoders.decodePsismsc(resources, bytes)

        assertThat(element).isNotNull()
        assertThat(element!!.subElements).hasSize(1)
        assertThat(element.subElements.single().label)
                .contains("Public Service Identity of the SM-SC")
        assertThat(element.subElements.single().toString())
                .isEqualTo("7369703A736D7363406578616D706C652E636F6D (sip:smsc@example.com)")
        assertThat(element.byteArray).isEqualTo(bytes)
    }

    @Test
    fun decoders_invalidData_returnNull() {
        assertThat(TelecomEfDecoders.decodeSume(resources, hexStringToByteArray("9E020101")))
                .isNull()
        assertThat(TelecomEfDecoders.decodeIceDn(resources, ByteArray(13))).isNull()
        assertThat(TelecomEfDecoders.decodeIceFf(resources, hexStringToByteArray("800100")))
                .isNull()
        assertThat(TelecomEfDecoders.decodeRma(resources, hexStringToByteArray("05850141")))
                .isNull()
        assertThat(TelecomEfDecoders.decodePsismsc(resources, hexStringToByteArray("810141")))
                .isNull()
    }

    @Test
    fun registry_containsAllTelecomDecoders() {
        val fileIds = listOf(
                FileId.EF_TELECOM_ARR, FileId.EF_TELECOM_RMA, FileId.EF_TELECOM_SUME,
                FileId.EF_TELECOM_ICE_DN, FileId.EF_TELECOM_ICE_FF,
                FileId.EF_TELECOM_PSISMSC
        )

        fileIds.forEach { fileId ->
            val path = FileId.DF_TELECOM + fileId
            assertThat(EfDecoderRegistry.has(FileId.AID_NONE, path)).isTrue()
            assertThat(EfDecoderRegistry.find(FileId.AID_NONE, path)).isNotNull()
        }
    }
}
