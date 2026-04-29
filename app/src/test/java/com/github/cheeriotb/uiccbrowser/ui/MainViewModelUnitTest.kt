/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui

import android.app.Application
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.github.cheeriotb.uiccbrowser.R
import com.github.cheeriotb.uiccbrowser.element.ef.AppTemplate
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainViewModelUnitTest {

    private lateinit var resources: Resources

    companion object {
        // USIM AID: 3GPP RID (A000000087) + USIM code (1002) + trailing bytes
        private const val USIM_AID_1 = "A0000000871002FFFFFFFFFFFFFFFF"
        private const val USIM_AID_2 = "A0000000871002FF33FFFF89010101"

        // ISIM AID: 3GPP RID (A000000087) + ISIM code (1004) + trailing bytes
        private const val ISIM_AID_1 = "A0000000871004FFFFFFFFFF"
        private const val ISIM_AID_2 = "A0000000871004FF33FFFFFF"

        // AID with an unrelated RID — should be ignored
        private const val UNKNOWN_AID = "A00000008810020000000000000000"
    }

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    }

    @Test
    fun buildNavItems_noAids_returnsMfOnly() {
        val items = MainViewModel.buildNavItems(resources, emptyList())

        assertThat(items).hasSize(1)
        assertThat(items[0].level).isEqualTo(NavLevel.MF)
        assertThat(items[0].label).isEqualTo(resources.getString(R.string.nav_item_mf))
        assertThat(items[0].iconResId).isEqualTo(R.drawable.folder)
        assertThat(items[0].aid).isNull()
    }

    @Test
    fun buildNavItems_oneUsim_returnsMfAndUsimWithoutNumber() {
        val items = MainViewModel.buildNavItems(resources, listOf(USIM_AID_1))

        assertThat(items).hasSize(2)
        assertThat(items[1].level).isEqualTo(NavLevel.USIM)
        assertThat(items[1].label).isEqualTo(resources.getString(R.string.nav_item_usim))
        assertThat(items[1].iconResId).isEqualTo(R.drawable.folder_usim)
        assertThat(items[1].aid).isEqualTo(USIM_AID_1)
    }

    @Test
    fun buildNavItems_twoUsim_returnsNumberedLabels() {
        val items = MainViewModel.buildNavItems(resources, listOf(USIM_AID_1, USIM_AID_2))

        assertThat(items).hasSize(3)
        assertThat(items[1].label).isEqualTo(resources.getString(R.string.nav_item_usim_numbered, 1))
        assertThat(items[2].label).isEqualTo(resources.getString(R.string.nav_item_usim_numbered, 2))
        assertThat(items[1].aid).isEqualTo(USIM_AID_1)
        assertThat(items[2].aid).isEqualTo(USIM_AID_2)
    }

    @Test
    fun buildNavItems_oneIsim_returnsMfAndIsimWithoutNumber() {
        val items = MainViewModel.buildNavItems(resources, listOf(ISIM_AID_1))

        assertThat(items).hasSize(2)
        assertThat(items[1].level).isEqualTo(NavLevel.ISIM)
        assertThat(items[1].label).isEqualTo(resources.getString(R.string.nav_item_isim))
        assertThat(items[1].iconResId).isEqualTo(R.drawable.folder_isim)
        assertThat(items[1].aid).isEqualTo(ISIM_AID_1)
    }

    @Test
    fun buildNavItems_twoIsim_returnsNumberedLabels() {
        val items = MainViewModel.buildNavItems(resources, listOf(ISIM_AID_1, ISIM_AID_2))

        assertThat(items).hasSize(3)
        assertThat(items[1].label).isEqualTo(resources.getString(R.string.nav_item_isim_numbered, 1))
        assertThat(items[2].label).isEqualTo(resources.getString(R.string.nav_item_isim_numbered, 2))
    }

    @Test
    fun buildNavItems_oneUsimOneIsim_returnsAll() {
        val items = MainViewModel.buildNavItems(resources, listOf(USIM_AID_1, ISIM_AID_1))

        assertThat(items).hasSize(3)
        assertThat(items[0].level).isEqualTo(NavLevel.MF)
        assertThat(items[1].level).isEqualTo(NavLevel.USIM)
        assertThat(items[2].level).isEqualTo(NavLevel.ISIM)
    }

    @Test
    fun buildNavItems_unknownAid_isIgnored() {
        val items = MainViewModel.buildNavItems(resources, listOf(UNKNOWN_AID))

        // Only MF should be present; the unrecognised AID is silently ignored.
        assertThat(items).hasSize(1)
        assertThat(items[0].level).isEqualTo(NavLevel.MF)
    }

    @Test
    fun buildNavItems_mfIsAlwaysFirst() {
        val items = MainViewModel.buildNavItems(resources, listOf(USIM_AID_1, ISIM_AID_1))

        assertThat(items[0].level).isEqualTo(NavLevel.MF)
    }

    @Test
    fun buildNavItems_usimBeforeIsim() {
        // USIM items must appear before ISIM items regardless of AID order in the list.
        val itemsUsimFirst = MainViewModel.buildNavItems(resources, listOf(USIM_AID_1, ISIM_AID_1))
        val itemsIsimFirst = MainViewModel.buildNavItems(resources, listOf(ISIM_AID_1, USIM_AID_1))

        assertThat(itemsUsimFirst[1].level).isEqualTo(NavLevel.USIM)
        assertThat(itemsUsimFirst[2].level).isEqualTo(NavLevel.ISIM)
        assertThat(itemsIsimFirst[1].level).isEqualTo(NavLevel.USIM)
        assertThat(itemsIsimFirst[2].level).isEqualTo(NavLevel.ISIM)
    }

    @Test
    fun selectNavItem_updatesSelectedNavItem() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)
        val item = NavItem(
            label = resources.getString(R.string.nav_item_mf),
            iconResId = R.drawable.folder,
            level = NavLevel.MF
        )

        viewModel.selectNavItem(item)

        assertThat(viewModel.selectedNavItem.value).isEqualTo(item)
    }
}
