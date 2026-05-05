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
}
