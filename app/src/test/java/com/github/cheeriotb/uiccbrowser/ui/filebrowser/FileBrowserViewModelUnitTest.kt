/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui.filebrowser

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FileBrowserViewModelUnitTest {

    @Test
    fun buildRootTitle_withId_includesIdInParens() {
        val title = FileBrowserViewModel.buildRootTitle("ADF USIM", "7FFF")

        assertThat(title).isEqualTo("ADF USIM (7FFF)")
    }

    @Test
    fun buildRootTitle_withoutId_returnsNameOnly() {
        val title = FileBrowserViewModel.buildRootTitle("MF", "")

        assertThat(title).isEqualTo("MF")
    }

    @Test
    fun buildSubTitle_includesDisplayPathAndDfId() {
        val title = FileBrowserViewModel.buildSubTitle("5GS", "5FC0", "7FFF")

        assertThat(title).isEqualTo("5GS (7FFF/5FC0)")
    }

    @Test
    fun buildSubTitle_deeperNesting_includesFullPath() {
        val title = FileBrowserViewModel.buildSubTitle("SomeEF", "4F07", "7FFF/5FC0")

        assertThat(title).isEqualTo("SomeEF (7FFF/5FC0/4F07)")
    }

    @Test
    fun formatDisplayPath_twoIds_addsSlash() {
        val result = FileBrowserViewModel.formatDisplayPath("7FFF5FC0")

        assertThat(result).isEqualTo("7FFF/5FC0")
    }

    @Test
    fun formatDisplayPath_singleId_noSlash() {
        val result = FileBrowserViewModel.formatDisplayPath("7FFF")

        assertThat(result).isEqualTo("7FFF")
    }

    @Test
    fun formatDisplayPath_threeIds_twoSlashes() {
        val result = FileBrowserViewModel.formatDisplayPath("7FFF5FC04F07")

        assertThat(result).isEqualTo("7FFF/5FC0/4F07")
    }

    @Test
    fun formatDisplayPath_emptyPath_returnsEmpty() {
        val result = FileBrowserViewModel.formatDisplayPath("")

        assertThat(result).isEmpty()
    }
}
