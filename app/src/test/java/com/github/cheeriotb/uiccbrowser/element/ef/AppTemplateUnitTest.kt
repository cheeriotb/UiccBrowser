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
import com.github.cheeriotb.uiccbrowser.element.BerTlvElement
import com.github.cheeriotb.uiccbrowser.util.hexStringToByteArray
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppTemplateUnitTest {

    private lateinit var resources: Resources

    companion object {
        // Application template (0x61) containing only an AID (0x4F):
        //   61 12 4F 10 A0000000871002FFFFFFFFFFFFFFFF
        private const val USIM_AID = "A0000000871002FFFFFFFFFFFFFFFFFF"
        private const val RECORD_USIM = "61124F10" + USIM_AID

        // Application template with AID + Application label (0x50):
        //   61 1C 4F 10 <AID> 50 08 "USIM App" (ASCII)
        private const val ISIM_AID = "A0000000871004FFFFFFFFFF"

        // Record with 0xFF padding (common for linear-fixed EFs)
        private const val RECORD_WITH_PADDING = RECORD_USIM + "FFFFFFFFFFFFFFFFFFFFFFFF"

        // Record that is all 0xFF (unused slot)
        private const val RECORD_UNUSED = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"

        // Real EF DIR record (SIM A): length 0x1E includes 0xFF padding inside the 0x61 value.
        //   4F 10 <AID 16B>  50 04 "USIM"  FF FF FF FF FF FF
        private const val RECORD_REAL_SIM_A =
                "611E4F10A0000000871002FF33FFFF890101010050045553494DFFFFFFFFFFFF"

        // Real EF DIR record (SIM B): length 0x1A is exact — 0xFF padding is outside the TLV.
        //   4F 10 <AID 16B>  50 06 "3 USIM"  | FF...FF (record-level padding, not in TLV)
        private const val RECORD_REAL_SIM_B =
                "611A4F10A0000000871002FFFFFFFF89040300FF500633205553494D" +
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"

        // Wrong top-level tag (0x62 = FCP template, not Application template)
        private const val RECORD_WRONG_TAG = "620B82054221001B0183022F00"
    }

    @Before
    fun setUp() {
        resources = (ApplicationProvider.getApplicationContext<android.content.Context>()).resources
    }

    @Test
    fun decode_usimAid_returnsElement() {
        val element = AppTemplate.decode(resources, hexStringToByteArray(RECORD_USIM))
        assertThat(element).isNotNull()
        assertThat(element!!.tag).isEqualTo(AppTemplate.TAG_APPLICATION_TEMPLATE)
    }

    @Test
    fun decode_usimAid_containsAidChild() {
        val element = AppTemplate.decode(resources, hexStringToByteArray(RECORD_USIM))!!
        val aidElement = element.subElements
                .filterIsInstance<BerTlvElement>()
                .find { it.tag == AppTemplate.TAG_APPLICATION_ID }
        assertThat(aidElement).isNotNull()
        assertThat(aidElement!!.data).isEqualTo(hexStringToByteArray(USIM_AID))
    }

    @Test
    fun decode_withPadding_returnsElement() {
        val element = AppTemplate.decode(resources, hexStringToByteArray(RECORD_WITH_PADDING))
        assertThat(element).isNotNull()
        val aidElement = element!!.subElements
                .filterIsInstance<BerTlvElement>()
                .find { it.tag == AppTemplate.TAG_APPLICATION_ID }
        assertThat(aidElement).isNotNull()
        assertThat(aidElement!!.data).isEqualTo(hexStringToByteArray(USIM_AID))
    }

    @Test
    fun decode_unusedRecord_returnsNull() {
        val element = AppTemplate.decode(resources, hexStringToByteArray(RECORD_UNUSED))
        assertThat(element).isNull()
    }

    @Test
    fun decode_wrongTag_returnsNull() {
        val element = AppTemplate.decode(resources, hexStringToByteArray(RECORD_WRONG_TAG))
        assertThat(element).isNull()
    }

    @Test
    fun decode_emptyBytes_returnsNull() {
        val element = AppTemplate.decode(resources, byteArrayOf())
        assertThat(element).isNull()
    }

    @Test
    fun decode_realSimRecord_ffPaddingInsideValue_returnsBothChildren() {
        // SIM A: 0xFF padding is encoded inside the 0x61 TLV value (length 0x1E includes padding).
        // Regression test for the "Unsupported tag field length" exception seen on a physical SIM.
        val element = AppTemplate.decode(resources, hexStringToByteArray(RECORD_REAL_SIM_A))
        assertThat(element).isNotNull()

        val children = element!!.subElements.filterIsInstance<BerTlvElement>()
        assertThat(children.any { it.tag == AppTemplate.TAG_APPLICATION_ID }).isTrue()
        assertThat(children.any { it.tag == AppTemplate.TAG_APPLICATION_LABEL }).isTrue()
    }

    @Test
    fun decode_realSimRecord_ffPaddingOutsideValue_returnsBothChildren() {
        // SIM B: 0xFF padding is record-level (outside the 0x61 TLV — length 0x1A is exact).
        // extractFirstTlv() strips the record-level padding; inner TLVs contain no FF padding.
        val element = AppTemplate.decode(resources, hexStringToByteArray(RECORD_REAL_SIM_B))
        assertThat(element).isNotNull()

        val children = element!!.subElements.filterIsInstance<BerTlvElement>()
        assertThat(children.any { it.tag == AppTemplate.TAG_APPLICATION_ID }).isTrue()
        assertThat(children.any { it.tag == AppTemplate.TAG_APPLICATION_LABEL }).isTrue()
    }

    @Test
    fun decode_withLabel_containsBothChildren() {
        // Build the record bytes manually to get exact length right
        val aidBytes = hexStringToByteArray(USIM_AID)            // 16 bytes
        val labelBytes = "USIM App".toByteArray(Charsets.US_ASCII) // 8 bytes
        val valueBytes =
                byteArrayOf(0x4F, aidBytes.size.toByte()) + aidBytes +
                byteArrayOf(0x50, labelBytes.size.toByte()) + labelBytes
        val recordBytes = byteArrayOf(0x61, valueBytes.size.toByte()) + valueBytes

        val element = AppTemplate.decode(resources, recordBytes)
        assertThat(element).isNotNull()

        val children = element!!.subElements.filterIsInstance<BerTlvElement>()
        assertThat(children.any { it.tag == AppTemplate.TAG_APPLICATION_ID }).isTrue()
        assertThat(children.any { it.tag == AppTemplate.TAG_APPLICATION_LABEL }).isTrue()
    }
}
