/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui.filebrowser

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.view.menu.MenuBuilder
import androidx.test.core.app.ApplicationProvider
import com.github.cheeriotb.uiccbrowser.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FileBrowserMenuUnitTest {

    @Test
    fun fileBrowserMenu_containsShowFcpTemplate() {
        val context = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext<Context>(),
            R.style.Theme_UiccBrowser
        )
        val menu = MenuBuilder(context)

        android.view.MenuInflater(context).inflate(R.menu.file_browser, menu)

        assertThat(menu.findItem(R.id.action_show_fcp_template).title.toString())
            .isEqualTo("Show FCP Template")
    }
}
