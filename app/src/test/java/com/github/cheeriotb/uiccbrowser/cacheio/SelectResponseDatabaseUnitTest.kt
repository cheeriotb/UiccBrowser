/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.cacheio

import androidx.test.core.app.ApplicationProvider
import com.github.cheeriotb.uiccbrowser.util.hexStringToByteArray
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SelectResponseDatabaseUnitTest {
    private lateinit var db: SelectResponseDatabase
    private lateinit var dao: SelectResponseDao

    companion object {
        private const val ICC_ID_INVALID = "8900000000000000003"
        private const val ICC_ID_1 = "8900000000000000001"
        private const val ICC_ID_2 = "8900000000000000002"

        private const val AID_INVALID = "00000000000000000000000000000003"
        private const val AID_NONE = ""
        private const val AID_1 = "00000000000000000000000000000001"
        private const val AID_2 = "00000000000000000000000000000002"

        private const val PATH_INVALID = "FFFF"
        private const val PATH_MF = ""
        private const val PATH_TELECOM = "7F10"
        private const val PATH_ADF = "7FFF"

        private const val FILE_ID_INVALID = "FFFF"
        private const val FILE_ID_1 = "0001"
        private const val FILE_ID_2 = "0002"

        private const val DATA_NONE = ""
        private const val DATA_1 = "010102"
        private const val DATA_2 = "020102"

        private const val SW_SUCCESS = 0x9000
        private const val SW_FILE_NOT_FOUND = 0x6A82
    }

    private val entry1Root = SelectResponse(ICC_ID_1, AID_NONE, PATH_MF, FILE_ID_1,
            hexStringToByteArray(DATA_1), SW_SUCCESS)
    private val entry1TelecomA = SelectResponse(ICC_ID_1, AID_NONE, PATH_TELECOM, FILE_ID_1,
            hexStringToByteArray(DATA_1), SW_SUCCESS)
    private val entry1TelecomB = SelectResponse(ICC_ID_1, AID_NONE, PATH_TELECOM, FILE_ID_2,
            hexStringToByteArray(DATA_NONE), SW_FILE_NOT_FOUND)
    private val entry1Adf1A = SelectResponse(ICC_ID_1, AID_1, PATH_ADF, FILE_ID_1,
            hexStringToByteArray(DATA_1), SW_SUCCESS)
    private val entry1Adf1B = SelectResponse(ICC_ID_1, AID_1, PATH_ADF, FILE_ID_2,
            hexStringToByteArray(DATA_NONE), SW_FILE_NOT_FOUND)
    private val entry1Adf2A = SelectResponse(ICC_ID_1, AID_2, PATH_ADF, FILE_ID_1,
            hexStringToByteArray(DATA_1), SW_SUCCESS)
    private val entry1Adf2B = SelectResponse(ICC_ID_1, AID_2, PATH_ADF, FILE_ID_2,
            hexStringToByteArray(DATA_NONE), SW_FILE_NOT_FOUND)

    private val entry2Root = SelectResponse(ICC_ID_2, AID_NONE, PATH_MF, FILE_ID_1,
            hexStringToByteArray(DATA_2), SW_SUCCESS)
    private val entry2TelecomA = SelectResponse(ICC_ID_2, AID_NONE, PATH_TELECOM, FILE_ID_1,
            hexStringToByteArray(DATA_2), SW_SUCCESS)
    private val entry2TelecomB = SelectResponse(ICC_ID_2, AID_NONE, PATH_TELECOM, FILE_ID_2,
            hexStringToByteArray(DATA_NONE), SW_FILE_NOT_FOUND)
    private val entry2Adf1A = SelectResponse(ICC_ID_2, AID_1, PATH_ADF, FILE_ID_1,
            hexStringToByteArray(DATA_2), SW_SUCCESS)
    private val entry2Adf1B = SelectResponse(ICC_ID_2, AID_1, PATH_ADF, FILE_ID_2,
            hexStringToByteArray(DATA_NONE), SW_FILE_NOT_FOUND)
    private val entry2Adf2A = SelectResponse(ICC_ID_2, AID_2, PATH_ADF, FILE_ID_1,
            hexStringToByteArray(DATA_2), SW_SUCCESS)
    private val entry2Adf2B = SelectResponse(ICC_ID_2, AID_2, PATH_ADF, FILE_ID_2,
            hexStringToByteArray(DATA_NONE), SW_FILE_NOT_FOUND)

    @Before
    fun setUp() = runBlocking {
        db = SelectResponseDatabase.from(ApplicationProvider.getApplicationContext(), true)
        dao = db.getDao()

        dao.insert(entry1Root)
        dao.insert(entry1TelecomA)
        dao.insert(entry1TelecomB)
        dao.insert(entry1Adf1A)
        dao.insert(entry1Adf1B)
        dao.insert(entry1Adf2A)
        dao.insert(entry1Adf2B)

        dao.insert(entry2Root)
        dao.insert(entry2TelecomA)
        dao.insert(entry2TelecomB)
        dao.insert(entry2Adf1A)
        dao.insert(entry2Adf1B)
        dao.insert(entry2Adf2A)
        dao.insert(entry2Adf2B)
    }

    @After
    fun tearDown() = runBlocking {
        db.close()
    }

    @Test
    fun get_notFound() = runBlocking {
        assertThat(dao.get(ICC_ID_INVALID, AID_NONE, PATH_MF, FILE_ID_1)).isEqualTo(null)

        assertThat(dao.get(ICC_ID_1, AID_INVALID, PATH_MF, FILE_ID_1)).isEqualTo(null)
        assertThat(dao.get(ICC_ID_2, AID_INVALID, PATH_MF, FILE_ID_1)).isEqualTo(null)

        assertThat(dao.get(ICC_ID_1, AID_NONE, PATH_INVALID, FILE_ID_1)).isEqualTo(null)
        assertThat(dao.get(ICC_ID_2, AID_NONE, PATH_INVALID, FILE_ID_1)).isEqualTo(null)

        assertThat(dao.get(ICC_ID_1, AID_NONE, PATH_MF, FILE_ID_INVALID)).isEqualTo(null)
        assertThat(dao.get(ICC_ID_2, AID_NONE, PATH_MF, FILE_ID_INVALID)).isEqualTo(null)
    }

    @Test
    fun getAll_notFound() = runBlocking {
        assertThat(dao.getAll(ICC_ID_INVALID, AID_NONE, PATH_MF).size).isEqualTo(0)

        assertThat(dao.getAll(ICC_ID_1, AID_INVALID, PATH_MF).size).isEqualTo(0)
        assertThat(dao.getAll(ICC_ID_2, AID_INVALID, PATH_MF).size).isEqualTo(0)

        assertThat(dao.getAll(ICC_ID_1, AID_NONE, PATH_INVALID).size).isEqualTo(0)
        assertThat(dao.getAll(ICC_ID_2, AID_NONE, PATH_INVALID).size).isEqualTo(0)
    }

    @Test
    fun get_mf() = runBlocking {
        assertThat(dao.get(ICC_ID_1, AID_NONE, PATH_MF, FILE_ID_1)).isEqualTo(entry1Root)
        assertThat(dao.get(ICC_ID_2, AID_NONE, PATH_MF, FILE_ID_1)).isEqualTo(entry2Root)
    }

    @Test
    fun getAll_mf() = runBlocking {
        val all1 = dao.getAll(ICC_ID_1, AID_NONE, PATH_MF)
        assertThat(all1.size).isEqualTo(1)
        assertThat(all1[0]).isEqualTo(entry1Root)

        val all2 = dao.getAll(ICC_ID_2, AID_NONE, PATH_MF)
        assertThat(all2.size).isEqualTo(1)
        assertThat(all2[0]).isEqualTo(entry2Root)
    }

    @Test
    fun get_telecom() = runBlocking {
        assertThat(dao.get(ICC_ID_1, AID_NONE, PATH_TELECOM, FILE_ID_1)).isEqualTo(entry1TelecomA)
        assertThat(dao.get(ICC_ID_1, AID_NONE, PATH_TELECOM, FILE_ID_2)).isEqualTo(entry1TelecomB)
        assertThat(dao.get(ICC_ID_2, AID_NONE, PATH_TELECOM, FILE_ID_1)).isEqualTo(entry2TelecomA)
        assertThat(dao.get(ICC_ID_2, AID_NONE, PATH_TELECOM, FILE_ID_2)).isEqualTo(entry2TelecomB)
    }

    @Test
    fun getAll_telecom() = runBlocking {
        val all1 = dao.getAll(ICC_ID_1, AID_NONE, PATH_TELECOM)
        assertThat(all1.size).isEqualTo(2)
        assertThat(all1[0]).isEqualTo(entry1TelecomA)
        assertThat(all1[1]).isEqualTo(entry1TelecomB)

        val all2 = dao.getAll(ICC_ID_2, AID_NONE, PATH_TELECOM)
        assertThat(all2.size).isEqualTo(2)
        assertThat(all2[0]).isEqualTo(entry2TelecomA)
        assertThat(all2[1]).isEqualTo(entry2TelecomB)
    }

    @Test
    fun get_adf1() = runBlocking {
        assertThat(dao.get(ICC_ID_1, AID_1, PATH_ADF, FILE_ID_1)).isEqualTo(entry1Adf1A)
        assertThat(dao.get(ICC_ID_1, AID_1, PATH_ADF, FILE_ID_2)).isEqualTo(entry1Adf1B)
        assertThat(dao.get(ICC_ID_2, AID_1, PATH_ADF, FILE_ID_1)).isEqualTo(entry2Adf1A)
        assertThat(dao.get(ICC_ID_2, AID_1, PATH_ADF, FILE_ID_2)).isEqualTo(entry2Adf1B)
    }

    @Test
    fun getAll_adf1() = runBlocking {
        val all1 = dao.getAll(ICC_ID_1, AID_1, PATH_ADF)
        assertThat(all1.size).isEqualTo(2)
        assertThat(all1[0]).isEqualTo(entry1Adf1A)
        assertThat(all1[1]).isEqualTo(entry1Adf1B)

        val all2 = dao.getAll(ICC_ID_2, AID_1, PATH_ADF)
        assertThat(all2.size).isEqualTo(2)
        assertThat(all2[0]).isEqualTo(entry2Adf1A)
        assertThat(all2[1]).isEqualTo(entry2Adf1B)
    }

    @Test
    fun get_adf2() = runBlocking {
        assertThat(dao.get(ICC_ID_1, AID_2, PATH_ADF, FILE_ID_1)).isEqualTo(entry1Adf2A)
        assertThat(dao.get(ICC_ID_1, AID_2, PATH_ADF, FILE_ID_2)).isEqualTo(entry1Adf2B)
        assertThat(dao.get(ICC_ID_2, AID_2, PATH_ADF, FILE_ID_1)).isEqualTo(entry2Adf2A)
        assertThat(dao.get(ICC_ID_2, AID_2, PATH_ADF, FILE_ID_2)).isEqualTo(entry2Adf2B)
    }

    @Test
    fun getAll_adf2() = runBlocking {
        val all1 = dao.getAll(ICC_ID_1, AID_2, PATH_ADF)
        assertThat(all1.size).isEqualTo(2)
        assertThat(all1[0]).isEqualTo(entry1Adf2A)
        assertThat(all1[1]).isEqualTo(entry1Adf2B)

        val all2 = dao.getAll(ICC_ID_2, AID_2, PATH_ADF)
        assertThat(all2.size).isEqualTo(2)
        assertThat(all2[0]).isEqualTo(entry2Adf2A)
        assertThat(all2[1]).isEqualTo(entry2Adf2B)
    }

    @Test
    fun delete() = runBlocking {
        dao.delete(ICC_ID_1)

        assertThat(dao.getAll(ICC_ID_1, AID_NONE, PATH_MF).size).isEqualTo(0)
        assertThat(dao.getAll(ICC_ID_1, AID_NONE, PATH_TELECOM).size).isEqualTo(0)
        assertThat(dao.getAll(ICC_ID_1, AID_1, PATH_ADF).size).isEqualTo(0)
        assertThat(dao.getAll(ICC_ID_1, AID_2, PATH_ADF).size).isEqualTo(0)

        assertThat(dao.getAll(ICC_ID_2, AID_NONE, PATH_MF).size).isEqualTo(1)
        assertThat(dao.getAll(ICC_ID_2, AID_NONE, PATH_TELECOM).size).isEqualTo(2)
        assertThat(dao.getAll(ICC_ID_2, AID_1, PATH_ADF).size).isEqualTo(2)
        assertThat(dao.getAll(ICC_ID_2, AID_2, PATH_ADF).size).isEqualTo(2)
    }

    @Test
    fun deleteAll() = runBlocking {
        dao.deleteAll()

        assertThat(dao.getAll(ICC_ID_1, AID_NONE, PATH_MF).size).isEqualTo(0)
        assertThat(dao.getAll(ICC_ID_2, AID_NONE, PATH_MF).size).isEqualTo(0)

        assertThat(dao.getAll(ICC_ID_1, AID_NONE, PATH_TELECOM).size).isEqualTo(0)
        assertThat(dao.getAll(ICC_ID_2, AID_NONE, PATH_TELECOM).size).isEqualTo(0)

        assertThat(dao.getAll(ICC_ID_1, AID_1, PATH_ADF).size).isEqualTo(0)
        assertThat(dao.getAll(ICC_ID_2, AID_1, PATH_ADF).size).isEqualTo(0)

        assertThat(dao.getAll(ICC_ID_1, AID_2, PATH_ADF).size).isEqualTo(0)
        assertThat(dao.getAll(ICC_ID_2, AID_2, PATH_ADF).size).isEqualTo(0)
    }
}
