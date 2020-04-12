/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.cacheio

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CachedSubscriptionDatabaseUnitTest {
    private lateinit var db: CachedSubscriptionDatabase
    private lateinit var dao: CachedSubscriptionDao

    companion object {
        private const val ICC_ID_INVALID = "8900000000000000003"
        private const val ICC_ID_1 = "8900000000000000001"
        private const val ICC_ID_2 = "8900000000000000002"
        private const val NAME_1 = "Carrier A"
        private const val NAME_2 = "Carrier B"
    }

    private val entry1 = CachedSubscription(ICC_ID_1, NAME_1)
    private val entry2 = CachedSubscription(ICC_ID_2, NAME_2)

    @Before
    fun setUp() = runBlocking {
        db = CachedSubscriptionDatabase.from(ApplicationProvider.getApplicationContext(), true)
        dao = db.getDao()

        dao.insert(entry1)
        dao.insert(entry2)
    }

    @After
    fun tearDown() = runBlocking {
        db.close()
    }

    @Test
    fun get() = runBlocking {
        assertThat(dao.get(ICC_ID_INVALID)).isEqualTo(null)
        assertThat(dao.get(ICC_ID_1)).isEqualTo(entry1)
        assertThat(dao.get(ICC_ID_2)).isEqualTo(entry2)
        assertThat(dao.getAll().size).isEqualTo(2)
    }

    @Test
    fun delete() = runBlocking {
        dao.delete(ICC_ID_1)
        assertThat(dao.get(ICC_ID_1)).isEqualTo(null)
        assertThat(dao.get(ICC_ID_2)).isEqualTo(entry2)
        assertThat(dao.getAll().size).isEqualTo(1)

        dao.delete(ICC_ID_2)
        assertThat(dao.get(ICC_ID_1)).isEqualTo(null)
        assertThat(dao.get(ICC_ID_2)).isEqualTo(null)
        assertThat(dao.getAll().size).isEqualTo(0)
    }

    @Test
    fun deleteAll() = runBlocking {
        dao.deleteAll()
        assertThat(dao.get(ICC_ID_1)).isEqualTo(null)
        assertThat(dao.get(ICC_ID_2)).isEqualTo(null)
        assertThat(dao.getAll().size).isEqualTo(0)
    }
}
