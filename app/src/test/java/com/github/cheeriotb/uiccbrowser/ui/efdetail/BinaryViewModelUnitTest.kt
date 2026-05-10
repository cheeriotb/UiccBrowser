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

class BinaryViewModelUnitTest {

    @Test
    fun buildRefreshTarget_unknown_returnsInitial() {
        val target = BinaryViewModel.buildRefreshTarget(
            BinaryViewModel.DataSource.UNKNOWN,
            recordLength = 0
        )

        assertThat(target).isEqualTo(BinaryViewModel.RefreshTarget.INITIAL)
    }

    @Test
    fun buildRefreshTarget_transparent_returnsBinary() {
        val target = BinaryViewModel.buildRefreshTarget(
            BinaryViewModel.DataSource.TRANSPARENT,
            recordLength = 0
        )

        assertThat(target).isEqualTo(BinaryViewModel.RefreshTarget.BINARY)
    }

    @Test
    fun buildRefreshTarget_linearFixedWithRecordLength_returnsCurrentRecord() {
        val target = BinaryViewModel.buildRefreshTarget(
            BinaryViewModel.DataSource.LINEAR_FIXED,
            recordLength = 10
        )

        assertThat(target).isEqualTo(BinaryViewModel.RefreshTarget.CURRENT_RECORD)
    }

    @Test
    fun buildRefreshTarget_linearFixedWithoutRecordLength_returnsInitial() {
        val target = BinaryViewModel.buildRefreshTarget(
            BinaryViewModel.DataSource.LINEAR_FIXED,
            recordLength = 0
        )

        assertThat(target).isEqualTo(BinaryViewModel.RefreshTarget.INITIAL)
    }

    @Test
    fun byteIndexForGridPosition_offsetColumn_returnsNull() {
        assertThat(BinaryViewModel.byteIndexForGridPosition(0, dataSize = 16)).isNull()
        assertThat(BinaryViewModel.byteIndexForGridPosition(9, dataSize = 16)).isNull()
    }

    @Test
    fun byteIndexForGridPosition_byteCell_returnsByteIndex() {
        assertThat(BinaryViewModel.byteIndexForGridPosition(1, dataSize = 16)).isEqualTo(0)
        assertThat(BinaryViewModel.byteIndexForGridPosition(8, dataSize = 16)).isEqualTo(7)
        assertThat(BinaryViewModel.byteIndexForGridPosition(10, dataSize = 16)).isEqualTo(8)
    }

    @Test
    fun byteIndexForGridPosition_blankCell_returnsNull() {
        assertThat(BinaryViewModel.byteIndexForGridPosition(17, dataSize = 9)).isNull()
    }

    @Test
    fun gridPositionForByteIndex_returnsAdapterPosition() {
        assertThat(BinaryViewModel.gridPositionForByteIndex(0)).isEqualTo(1)
        assertThat(BinaryViewModel.gridPositionForByteIndex(7)).isEqualTo(8)
        assertThat(BinaryViewModel.gridPositionForByteIndex(8)).isEqualTo(10)
    }

    @Test
    fun nextCursorIndex_middleByte_movesToNextByte() {
        assertThat(BinaryViewModel.nextCursorIndex(1, dataSize = 4)).isEqualTo(2)
    }

    @Test
    fun nextCursorIndex_lastByte_staysOnLastByte() {
        assertThat(BinaryViewModel.nextCursorIndex(3, dataSize = 4)).isEqualTo(3)
    }

    @Test
    fun insertByteAt_insertsZeroAndDropsLastByte() {
        val data = byteArrayOf(0xA5.toByte(), 0xA6.toByte(), 0xA7.toByte(), 0xA8.toByte())

        val result = BinaryViewModel.insertByteAt(data, index = 1)

        assertThat(result.asList()).containsExactly(
            0xA5.toByte(),
            0x00.toByte(),
            0xA6.toByte(),
            0xA7.toByte()
        ).inOrder()
    }

    @Test
    fun deleteByteAt_deletesByteAndPadsLastByteWithFf() {
        val data = byteArrayOf(0xA5.toByte(), 0xA6.toByte(), 0xA7.toByte(), 0xA8.toByte())

        val result = BinaryViewModel.deleteByteAt(data, index = 1)

        assertThat(result.asList()).containsExactly(
            0xA5.toByte(),
            0xA7.toByte(),
            0xA8.toByte(),
            0xFF.toByte()
        ).inOrder()
    }
}
