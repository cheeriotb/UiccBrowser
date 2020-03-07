/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HexUtilsTest {
    @Test
    fun return_byteToHexString() {
        assertThat(byteToHexString(0x00)).isEqualTo("00")
        assertThat(byteToHexString(0x01)).isEqualTo("01")
        assertThat(byteToHexString(0x7F)).isEqualTo("7F")
        assertThat(byteToHexString(0x80)).isEqualTo("80")
        assertThat(byteToHexString(0xFF)).isEqualTo("FF")
        assertThat(byteToHexString(0x100)).isEqualTo("00")
    }

    @Test
    fun return_hexStringToByte() {
        assertThat(hexStringToByte("00")).isEqualTo(b(0x00))
        assertThat(hexStringToByte("01")).isEqualTo(b(0x01))
        assertThat(hexStringToByte("7F")).isEqualTo(b(0x7F))
        assertThat(hexStringToByte("80")).isEqualTo(b(0x80))
        assertThat(hexStringToByte("FF")).isEqualTo(b(0xFF))
    }

    @Test
    fun return_extendedBytesToHexString() {
        assertThat(extendedBytesToHexString(0x000000)).isEqualTo("000000")
        assertThat(extendedBytesToHexString(0x000001)).isEqualTo("000001")
        assertThat(extendedBytesToHexString(0x007FFF)).isEqualTo("007FFF")
        assertThat(extendedBytesToHexString(0x008000)).isEqualTo("008000")
        assertThat(extendedBytesToHexString(0x00FFFF)).isEqualTo("00FFFF")
        assertThat(extendedBytesToHexString(0x010000)).isEqualTo("000000")
    }

    @Test
    fun return_hexStringToByteArray_byteArrayToHexString() {
        val byteString1 = "00" + "01" + "7F" + "80" + "FF"
        val byteArray1 = byteArrayOf(b(0x00), b(0x01), b(0x7F), b(0x80), b(0xFF))
        assertThat(hexStringToByteArray(byteString1)).isEqualTo(byteArray1)
        assertThat(byteArrayToHexString(byteArray1)).isEqualTo(byteString1)

        val byteString2 = "FF" + "80" + "7F" + "01" + "00"
        val byteArray2 = byteArrayOf(b(0xFF), b(0x80), b(0x7F), b(0x01), b(0x00))
        assertThat(hexStringToByteArray(byteString2)).isEqualTo(byteArray2)
        assertThat(byteArrayToHexString(byteArray2)).isEqualTo(byteString2)
    }

    private fun b(byte: Int) = byte.toByte()
}
