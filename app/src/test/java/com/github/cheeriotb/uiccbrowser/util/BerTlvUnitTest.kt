/*
 *  Copyright (C) 2020-2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BerTlvUnitTest {
    @Test
    fun tag_oneByte() {
        // Tag: 1E, Length: 00, Value: N/A
        val input = hexStringToByteArray("1E00")
        val tlvs = BerTlv.listFrom(input)

        assertThat(tlvs.size).isEqualTo(1)

        assertThat(tlvs[0].tag).isEqualTo(0x1E)
        assertThat(tlvs[0].isConstructed).isFalse()
        assertThat(tlvs[0].value.size).isEqualTo(0)
        assertThat(tlvs[0].toByteArray()).isEqualTo(input)
    }

    @Test
    fun tag_twoBytes() {
        // Tag: 1F1F, Length: 00, Value: N/A
        val input = hexStringToByteArray("1F1F00")
        val tlvs = BerTlv.listFrom(input)

        assertThat(tlvs.size).isEqualTo(1)

        assertThat(tlvs[0].tag).isEqualTo(0x1F1F)
        assertThat(tlvs[0].isConstructed).isFalse()
        assertThat(tlvs[0].value.size).isEqualTo(0)
        assertThat(tlvs[0].toByteArray()).isEqualTo(input)
    }

    @Test
    fun tag_threeBytes() {
        // Tag: 1F8100, Length: 00, Value: N/A
        val input = hexStringToByteArray("1F810000")
        val tlvs = BerTlv.listFrom(input)

        assertThat(tlvs.size).isEqualTo(1)

        assertThat(tlvs[0].tag).isEqualTo(0x1F8100)
        assertThat(tlvs[0].isConstructed).isFalse()
        assertThat(tlvs[0].value.size).isEqualTo(0)
        assertThat(tlvs[0].toByteArray()).isEqualTo(input)
    }

    @Test
    fun length_oneByte() {
        // Tag: 01, Length: 7F, Value: 00..
        val value = ByteArray(0x7F) { i -> i.toByte() }
        val input = hexStringToByteArray("017F") + value
        val tlvs = BerTlv.listFrom(input)

        assertThat(tlvs.size).isEqualTo(1)

        assertThat(tlvs[0].tag).isEqualTo(0x01)
        assertThat(tlvs[0].isConstructed).isFalse()
        assertThat(tlvs[0].value).isEqualTo(value)
        assertThat(tlvs[0].toByteArray()).isEqualTo(input)
    }

    @Test
    fun length_twoBytes() {
        // Tag: 02, Length: (81)80, Value: 00..
        val value = ByteArray(0x80) { i -> i.toByte() }
        val input = hexStringToByteArray("028180") + value
        val tlvs = BerTlv.listFrom(input)

        assertThat(tlvs.size).isEqualTo(1)

        assertThat(tlvs[0].tag).isEqualTo(0x02)
        assertThat(tlvs[0].isConstructed).isFalse()
        assertThat(tlvs[0].value).isEqualTo(value)
        assertThat(tlvs[0].toByteArray()).isEqualTo(input)
    }

    @Test
    fun length_threeBytes() {
        // Tag: 03, Length: (82)FFFF, Value: 00..
        val value = ByteArray(0xFFFF) { i -> i.toByte() }
        val input = hexStringToByteArray("0382FFFF") + value
        val tlvs = BerTlv.listFrom(input)

        assertThat(tlvs.size).isEqualTo(1)

        assertThat(tlvs[0].tag).isEqualTo(0x03)
        assertThat(tlvs[0].isConstructed).isFalse()
        assertThat(tlvs[0].value).isEqualTo(value)
        assertThat(tlvs[0].toByteArray()).isEqualTo(input)
    }

    @Test
    fun length_fourBytes() {
        // Tag: 04, Length: (83)FFFFFF, Value: 00..
        val value = ByteArray(0xFFFFFF) { i -> i.toByte() }
        val input = hexStringToByteArray("0483FFFFFF") + value
        val tlvs = BerTlv.listFrom(input)

        assertThat(tlvs.size).isEqualTo(1)

        assertThat(tlvs[0].tag).isEqualTo(0x04)
        assertThat(tlvs[0].isConstructed).isFalse()
        assertThat(tlvs[0].value).isEqualTo(value)
        assertThat(tlvs[0].toByteArray()).isEqualTo(input)
    }

    @Test
    fun listFrom_trailingFfPadding_stopsBeforePadding() {
        // Two valid TLVs followed by 0xFF padding bytes — mirrors the structure seen in
        // linear-fixed EF records (e.g. EF DIR Application template children).
        val input = hexStringToByteArray("4F02AABB5001CCFFFFFF")
        val tlvs = BerTlv.listFrom(input)

        assertThat(tlvs.size).isEqualTo(2)
        assertThat(tlvs[0].tag).isEqualTo(0x4F)
        assertThat(tlvs[0].value).isEqualTo(hexStringToByteArray("AABB"))
        assertThat(tlvs[1].tag).isEqualTo(0x50)
        assertThat(tlvs[1].value).isEqualTo(hexStringToByteArray("CC"))
    }

    @Test
    fun listFrom_allFfBytes_returnsEmpty() {
        // Completely unused record — all bytes are 0xFF padding.
        val input = ByteArray(8) { 0xFF.toByte() }
        val tlvs = BerTlv.listFrom(input)

        assertThat(tlvs).isEmpty()
    }

    @Test
    fun listFrom_ffInsideValue_notTreatedAsPadding() {
        // 0xFF bytes that appear inside a value field must not be mistaken for padding.
        // AID tag 0x4F, 3-byte value containing 0xFF in the middle.
        val input = hexStringToByteArray("4F03A0FF01")
        val tlvs = BerTlv.listFrom(input)

        assertThat(tlvs.size).isEqualTo(1)
        assertThat(tlvs[0].tag).isEqualTo(0x4F)
        assertThat(tlvs[0].value).isEqualTo(hexStringToByteArray("A0FF01"))
    }

    @Test
    fun constructed() {
        /*
           |  T | 21 | (1) A constructed BER-TLV contains a constructed one and a primitive one
           |  L | 0A |
           |  V |  T | 22 | (2) A constructed BER-TLV contains a primitive one and a constructed one
           |    |  L | 05 |
           |    |  V |  T | 01 | (3) A primitive BER-TLV
           |    |    |  L | 01 |
           |    |    |  V | 01 |
           |    |    |  T | 21 | (4) A constructed BER-TLV with no value
           |    |    |  L | 00 |
           |    |    |  V | -- |
           |    |  T | 02 | (5) A primitive BER-TLV
           |    |  L | 01 |
           |    |  V | 01 |
         */
        val input = hexStringToByteArray("210A22050101012100020101")
        val tlvs = BerTlv.listFrom(input)

        assertThat(tlvs.size).isEqualTo(1)

        // (1) A constructed BER-TLV contains a constructed one and a primitive one
        assertThat(tlvs[0].tag).isEqualTo(0x21)
        assertThat(tlvs[0].isConstructed).isTrue()
        assertThat(tlvs[0].value).isEqualTo(hexStringToByteArray("22050101012100020101"))
        assertThat(tlvs[0].toByteArray()).isEqualTo(
                hexStringToByteArray("210A22050101012100020101"))
        assertThat(tlvs[0].tlvs.size).isEqualTo(2)

        // (2) A constructed BER-TLV contains a primitive one and a constructed one
        assertThat(tlvs[0].tlvs[0].tag).isEqualTo(0x22)
        assertThat(tlvs[0].tlvs[0].isConstructed).isTrue()
        assertThat(tlvs[0].tlvs[0].value).isEqualTo(hexStringToByteArray("0101012100"))
        assertThat(tlvs[0].tlvs[0].toByteArray()).isEqualTo(
                hexStringToByteArray("22050101012100"))
        assertThat(tlvs[0].tlvs[0].tlvs.size).isEqualTo(2)

        // (3) A primitive BER-TLV
        assertThat(tlvs[0].tlvs[0].tlvs[0].tag).isEqualTo(0x01)
        assertThat(tlvs[0].tlvs[0].tlvs[0].isConstructed).isFalse()
        assertThat(tlvs[0].tlvs[0].tlvs[0].value).isEqualTo(hexStringToByteArray("01"))
        assertThat(tlvs[0].tlvs[0].tlvs[0].toByteArray()).isEqualTo(
                hexStringToByteArray("010101"))

        // (4) A constructed BER-TLV with no value
        assertThat(tlvs[0].tlvs[0].tlvs[1].tag).isEqualTo(0x21)
        assertThat(tlvs[0].tlvs[0].tlvs[1].isConstructed).isTrue()
        assertThat(tlvs[0].tlvs[0].tlvs[1].value).isEqualTo(byteArrayOf())
        assertThat(tlvs[0].tlvs[0].tlvs[1].toByteArray()).isEqualTo(
                hexStringToByteArray("2100"))

        // (5) A primitive BER-TLV
        assertThat(tlvs[0].tlvs[1].tag).isEqualTo(0x02)
        assertThat(tlvs[0].tlvs[1].isConstructed).isFalse()
        assertThat(tlvs[0].tlvs[1].value).isEqualTo(hexStringToByteArray("01"))
        assertThat(tlvs[0].tlvs[1].toByteArray()).isEqualTo(
                hexStringToByteArray("020101"))
    }
}
