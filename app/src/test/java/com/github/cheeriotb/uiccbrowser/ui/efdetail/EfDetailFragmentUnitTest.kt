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

class EfDetailFragmentUnitTest {

    @Test
    fun buildRecordSelectorState_zeroRecords_notVisible() {
        val state = EfDetailFragment.buildRecordSelectorState(0, 0, true)

        assertThat(state.visible).isFalse()
    }

    @Test
    fun buildRecordSelectorState_nonZeroRecords_visible() {
        val state = EfDetailFragment.buildRecordSelectorState(3, 0, true)

        assertThat(state.visible).isTrue()
    }

    @Test
    fun buildRecordSelectorState_transparentEf_notVisibleOnAllTabs() {
        for (tab in 0..2) {
            assertThat(EfDetailFragment.buildRecordSelectorState(0, tab, true).visible).isFalse()
        }
    }

    @Test
    fun buildRecordSelectorState_binaryTab_enabled() {
        val state = EfDetailFragment.buildRecordSelectorState(3, 0, true)

        assertThat(state.enabled).isTrue()
    }

    @Test
    fun buildRecordSelectorState_infoTab_enabled() {
        val state = EfDetailFragment.buildRecordSelectorState(3, 1, true)

        assertThat(state.enabled).isTrue()
    }

    @Test
    fun buildRecordSelectorState_fcpTab_notEnabled() {
        val state = EfDetailFragment.buildRecordSelectorState(3, 2, true)

        assertThat(state.enabled).isFalse()
    }

    @Test
    fun buildRecordSelectorState_transparentEfOnFcpTab_notVisibleAndNotEnabled() {
        val state = EfDetailFragment.buildRecordSelectorState(0, 2, true)

        assertThat(state.visible).isFalse()
        assertThat(state.enabled).isFalse()
    }

    @Test
    fun buildRecordSelectorState_binaryTabWhenNoDecoder_enabled() {
        val state = EfDetailFragment.buildRecordSelectorState(3, 0, false)

        assertThat(state.enabled).isTrue()
    }

    @Test
    fun buildRecordSelectorState_fcpTabWhenNoDecoder_notEnabled() {
        val state = EfDetailFragment.buildRecordSelectorState(3, 1, false)

        assertThat(state.enabled).isFalse()
    }
}
