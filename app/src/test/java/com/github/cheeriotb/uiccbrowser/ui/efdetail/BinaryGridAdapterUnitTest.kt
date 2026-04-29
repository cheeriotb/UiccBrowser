/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui.efdetail

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BinaryGridAdapterUnitTest {

    @Test
    fun itemCount_emptyData_returnsZero() {
        assertThat(BinaryGridAdapter.itemCount(0)).isEqualTo(0)
    }

    @Test
    fun itemCount_oneRow_returnsNine() {
        assertThat(BinaryGridAdapter.itemCount(1)).isEqualTo(9)
        assertThat(BinaryGridAdapter.itemCount(8)).isEqualTo(9)
    }

    @Test
    fun itemCount_twoRows_returnsEighteen() {
        assertThat(BinaryGridAdapter.itemCount(9)).isEqualTo(18)
        assertThat(BinaryGridAdapter.itemCount(16)).isEqualTo(18)
    }

    @Test
    fun itemCount_exactRowBoundary_returnsCorrectCount() {
        // 256 bytes = 32 full rows
        assertThat(BinaryGridAdapter.itemCount(256)).isEqualTo(32 * 9)
    }

    @Test
    fun formatOffset_rowZero_returnsFourZeros() {
        assertThat(BinaryGridAdapter.formatOffset(0)).isEqualTo("0000")
    }

    @Test
    fun formatOffset_rowOne_returnsEight() {
        assertThat(BinaryGridAdapter.formatOffset(1)).isEqualTo("0008")
    }

    @Test
    fun formatOffset_rowTwo_returnsSixteen() {
        assertThat(BinaryGridAdapter.formatOffset(2)).isEqualTo("0010")
    }

    @Test
    fun formatOffset_largeRow_isUppercaseHex() {
        // Row 256 * 8 = 2048 = 0x0800
        assertThat(BinaryGridAdapter.formatOffset(256)).isEqualTo("0800")
    }

    @Test
    fun formatByte_zeroByte_returnsTwoZeros() {
        assertThat(BinaryGridAdapter.formatByte(0x00.toByte())).isEqualTo("00")
    }

    @Test
    fun formatByte_maxByte_returnsFF() {
        assertThat(BinaryGridAdapter.formatByte(0xFF.toByte())).isEqualTo("FF")
    }

    @Test
    fun formatByte_singleNibble_isPaddedWithLeadingZero() {
        assertThat(BinaryGridAdapter.formatByte(0x0A.toByte())).isEqualTo("0A")
    }

    @Test
    fun formatByte_isUppercaseHex() {
        assertThat(BinaryGridAdapter.formatByte(0xAB.toByte())).isEqualTo("AB")
    }
}
