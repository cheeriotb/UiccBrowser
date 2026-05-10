/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui.efdetail

import com.github.cheeriotb.uiccbrowser.repository.FileId
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EfDetailViewModelUnitTest {

    @Test
    fun isEditModeEnabled_initiallyFalse() {
        val viewModel = EfDetailViewModel("EF", "6FAD", FileId(FileId.AID_NONE, "", "6FAD"))

        assertThat(viewModel.isEditModeEnabled.value).isFalse()
    }

    @Test
    fun enableEditMode_setsIsEditModeEnabled() {
        val viewModel = EfDetailViewModel("EF", "6FAD", FileId(FileId.AID_NONE, "", "6FAD"))

        viewModel.enableEditMode()

        assertThat(viewModel.isEditModeEnabled.value).isTrue()
    }

    @Test
    fun disableEditMode_clearsIsEditModeEnabled() {
        val viewModel = EfDetailViewModel("EF", "6FAD", FileId(FileId.AID_NONE, "", "6FAD"))

        viewModel.enableEditMode()
        viewModel.disableEditMode()

        assertThat(viewModel.isEditModeEnabled.value).isFalse()
    }
}
