/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui.efdetail

import android.content.Context
import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
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

    @Test
    fun editKeyboardLayout_usesFixedTextSize() {
        val normalButton = inflateBinaryLayout(fontScale = 1.0f)
            .findViewById<MaterialButton>(R.id.keyboardButtonSave)
        val largeButton = inflateBinaryLayout(fontScale = 2.0f)
            .findViewById<MaterialButton>(R.id.keyboardButtonSave)
        val expectedSize = normalButton.resources.getDimension(R.dimen.binary_keyboard_text_size)

        assertThat(normalButton.textSize).isWithin(0.01f).of(expectedSize)
        assertThat(largeButton.textSize).isWithin(0.01f).of(expectedSize)
    }

    @Test
    fun editKeyboardLayout_keepsButtonTextVisible() {
        val root = inflateBinaryLayout()
        val keyboard = root.findViewById<ViewGroup>(R.id.editKeyboardLayout)
        val button = root.findViewById<MaterialButton>(R.id.keyboardButtonSave)

        assertThat(keyboard.paddingBottom)
            .isEqualTo(root.resources.getDimensionPixelSize(
                R.dimen.binary_keyboard_padding_bottom
            ))
        assertThat(button.includeFontPadding).isFalse()
        assertThat(button.maxLines).isEqualTo(1)
        assertThat(button.layoutParams.height)
            .isEqualTo(button.resources.getDimensionPixelSize(
                R.dimen.binary_keyboard_button_height
            ))
        assertThat(button.minimumHeight)
            .isEqualTo(button.resources.getDimensionPixelSize(
                R.dimen.binary_keyboard_button_min_height
            ))
        assertThat(button.elevation).isEqualTo(0f)
        assertThat(button.stateListAnimator).isNull()
    }

    @Test
    fun binaryGrid_usesMinimumWidthInsideHorizontalScrollViews() {
        val root = inflateBinaryLayout()
        val headerScrollView = root.findViewById<HorizontalScrollView>(R.id.headerScrollView)
        val dataScrollView = root.findViewById<HorizontalScrollView>(R.id.dataScrollView)
        val expectedWidth = root.resources.getDimensionPixelSize(R.dimen.binary_grid_min_width)

        assertThat(headerScrollView.getChildAt(0).layoutParams.width).isEqualTo(expectedWidth)
        assertThat(dataScrollView.getChildAt(0).layoutParams.width).isEqualTo(expectedWidth)
    }

    @Test
    fun binaryGrid_stretchesToViewportWhenWiderThanMinimum() {
        val root = inflateBinaryLayout()
        val headerScrollView = root.findViewById<HorizontalScrollView>(R.id.headerScrollView)
        val dataScrollView = root.findViewById<HorizontalScrollView>(R.id.dataScrollView)

        assertThat(headerScrollView.isFillViewport).isTrue()
        assertThat(dataScrollView.isFillViewport).isTrue()
    }

    @Test
    fun editKeyboardLayout_keepsRowGap() {
        val keyboard = inflateBinaryLayout().findViewById<LinearLayout>(R.id.editKeyboardLayout)
        val expectedGap = keyboard.resources.getDimensionPixelSize(R.dimen.binary_keyboard_row_gap)
        val buttonRows = (0 until keyboard.childCount)
            .map { keyboard.getChildAt(it) }
            .filterIsInstance<LinearLayout>()

        assertThat(buttonRows).hasSize(4)
        buttonRows.forEach { row ->
            val params = row.layoutParams as ViewGroup.MarginLayoutParams
            assertThat(params.bottomMargin).isEqualTo(expectedGap)
        }
    }

    @Test
    fun binaryGridContentWidth_usesLargerWidth() {
        assertThat(BinaryFragment.gridContentWidth(minWidth = 360, availableWidth = 320))
            .isEqualTo(360)
        assertThat(BinaryFragment.gridContentWidth(minWidth = 360, availableWidth = 480))
            .isEqualTo(480)
    }

    @Test
    fun binaryGridText_usesFixedTextSize() {
        val normalCell = inflateTextView(R.layout.item_binary_cell, fontScale = 1.0f)
        val largeCell = inflateTextView(R.layout.item_binary_cell, fontScale = 2.0f)
        val normalHeader = inflateTextView(R.layout.item_binary_header, fontScale = 1.0f)
        val largeHeader = inflateTextView(R.layout.item_binary_header, fontScale = 2.0f)
        val expectedSize = normalCell.resources.getDimension(R.dimen.binary_grid_text_size)

        assertThat(normalCell.textSize).isWithin(0.01f).of(expectedSize)
        assertThat(largeCell.textSize).isWithin(0.01f).of(expectedSize)
        assertThat(normalHeader.textSize).isWithin(0.01f).of(expectedSize)
        assertThat(largeHeader.textSize).isWithin(0.01f).of(expectedSize)
    }

    @Test
    fun binaryGridText_doesNotWrap() {
        val cell = inflateTextView(R.layout.item_binary_cell, fontScale = 2.0f)
        val header = inflateTextView(R.layout.item_binary_header, fontScale = 2.0f)

        assertThat(cell.includeFontPadding).isFalse()
        assertThat(cell.maxLines).isEqualTo(1)
        assertThat(header.includeFontPadding).isFalse()
        assertThat(header.maxLines).isEqualTo(1)
    }

    private fun inflateBinaryLayout(fontScale: Float = 1.0f): View {
        return LayoutInflater.from(themedContext(fontScale))
            .inflate(R.layout.fragment_binary, null, false)
    }

    private fun inflateTextView(layoutId: Int, fontScale: Float): TextView {
        return LayoutInflater.from(themedContext(fontScale)).inflate(layoutId, null, false)
            as TextView
    }

    private fun themedContext(fontScale: Float): Context {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = Configuration(context.resources.configuration).apply {
            this.fontScale = fontScale
        }
        return ContextThemeWrapper(
            context.createConfigurationContext(configuration),
            R.style.Theme_UiccBrowser
        )
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
