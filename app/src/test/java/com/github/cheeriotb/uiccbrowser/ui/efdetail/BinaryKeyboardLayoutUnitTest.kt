/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui.efdetail

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import com.github.cheeriotb.uiccbrowser.R
import com.google.android.material.button.MaterialButton
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BinaryKeyboardLayoutUnitTest {

    @Test
    fun editKeyboardLayout_initiallyGone() {
        val root = inflateBinaryLayout()

        assertThat(root.findViewById<View>(R.id.editKeyboardLayout).visibility)
            .isEqualTo(View.GONE)
    }

    @Test
    fun editKeyboardLayout_containsDedicatedKeyboardButtons() {
        val keyboard = inflateBinaryLayout().findViewById<ViewGroup>(R.id.editKeyboardLayout)

        val labels = keyboard.materialButtonLabels()

        assertThat(labels).containsExactly(
            "0", "1", "2", "3", "Insert",
            "4", "5", "6", "7", "Delete",
            "8", "9", "A", "B", "Quit",
            "C", "D", "E", "F", "Save"
        ).inOrder()
    }

    private fun inflateBinaryLayout(): View {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val themedContext = ContextThemeWrapper(context, R.style.Theme_UiccBrowser)
        return LayoutInflater.from(themedContext).inflate(R.layout.fragment_binary, null, false)
    }

    private fun ViewGroup.materialButtonLabels(): List<String> {
        val labels = mutableListOf<String>()
        collectMaterialButtonLabels(labels)
        return labels
    }

    private fun View.collectMaterialButtonLabels(labels: MutableList<String>) {
        if (this is MaterialButton) {
            labels.add(text.toString())
        }
        if (this is ViewGroup) {
            for (index in 0 until childCount) {
                getChildAt(index).collectMaterialButtonLabels(labels)
            }
        }
    }
}
