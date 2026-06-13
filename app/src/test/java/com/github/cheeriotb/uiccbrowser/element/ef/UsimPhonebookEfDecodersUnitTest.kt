/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.element.ef

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.github.cheeriotb.uiccbrowser.R
import com.github.cheeriotb.uiccbrowser.element.EfDecoderRegistry
import com.github.cheeriotb.uiccbrowser.repository.FileId
import com.github.cheeriotb.uiccbrowser.util.hexStringToByteArray
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UsimPhonebookEfDecodersUnitTest {

    private lateinit var resources: Resources

    companion object {
        private const val AID_USIM = AppTemplate.RID + AppTemplate.APP_USIM
        private const val PBR =
                "A80DC0034F3A01CA024F50CA024F51A904C1024F17AA04C2024F4AFFFF"
    }

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    }

    @Test
    fun decodePbr_validRecord_returnsReferencesAndPadding() {
        val element = UsimPhonebookEfDecoders.decodePbr(
                resources,
                hexStringToByteArray(PBR)
        )

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_pbr_label))
        assertThat(element.subElements).hasSize(4)
        assertThat(element.subElements[0].label).contains("Type 1 phone book reference")
        assertThat(element.subElements[0].subElements).hasSize(3)
        assertThat(element.subElements[0].subElements[0].label).contains("EF ADN reference 1")
        assertThat(element.subElements[0].subElements[0].subElements[0].toString())
                .isEqualTo("4F3A")
        assertThat(element.subElements[0].subElements[0].subElements[1].toString())
                .isEqualTo("01 (1)")
        assertThat(element.subElements[0].subElements[1].label).contains("EF EMAIL reference 1")
        assertThat(element.subElements[0].subElements[2].label).contains("EF EMAIL reference 2")
        assertThat(element.subElements[1].label).contains("Type 2 phone book reference")
        assertThat(element.subElements[2].label).contains("Type 3 phone book reference")
        assertThat(element.subElements[3].label).isEqualTo(resources.getString(
                R.string.padding_label))
        assertThat(element.byteArray).isEqualTo(hexStringToByteArray(PBR))
    }

    @Test
    fun decodeCounters_validData_returnsUnsignedDecimalInterpretations() {
        val psc = UsimPhonebookEfDecoders.decodePsc(
                resources,
                hexStringToByteArray("FFFFFFFF")
        )
        val cc = UsimPhonebookEfDecoders.decodeCc(resources, hexStringToByteArray("0102"))
        val puid = UsimPhonebookEfDecoders.decodePuid(resources, hexStringToByteArray("0001"))

        assertThat(psc!!.subElements).hasSize(1)
        assertThat(psc.subElements[0].toString()).isEqualTo("FFFFFFFF (4294967295)")
        assertThat(cc!!.subElements[0].toString()).isEqualTo("0102 (258)")
        assertThat(puid!!.subElements[0].toString()).isEqualTo("0001 (1)")
    }

    @Test
    fun decodeInvalidData_returnsNull() {
        assertThat(UsimPhonebookEfDecoders.decodePbr(
                resources,
                hexStringToByteArray("A803C00100")
        )).isNull()
        assertThat(UsimPhonebookEfDecoders.decodePsc(
                resources,
                hexStringToByteArray("0000")
        )).isNull()
        assertThat(UsimPhonebookEfDecoders.decodeCc(
                resources,
                hexStringToByteArray("00")
        )).isNull()
        assertThat(UsimPhonebookEfDecoders.decodePuid(
                resources,
                hexStringToByteArray("000000")
        )).isNull()
    }

    @Test
    fun efDecoderRegistry_globalAndLocalPhonebookDecodersAreRegistered() {
        val files = listOf(
                FileId.EF_PHONEBOOK_PSC,
                FileId.EF_PHONEBOOK_CC,
                FileId.EF_PHONEBOOK_PUID,
                FileId.EF_PHONEBOOK_PBR
        )

        files.forEach { fileId ->
            assertThat(EfDecoderRegistry.has(
                    FileId.AID_NONE,
                    FileId.DF_TELECOM + FileId.DF_PHONEBOOK + fileId
            )).isTrue()
            assertThat(EfDecoderRegistry.has(
                    AID_USIM,
                    FileId.PATH_ADF + FileId.DF_PHONEBOOK + fileId
            )).isTrue()
        }
    }
}
