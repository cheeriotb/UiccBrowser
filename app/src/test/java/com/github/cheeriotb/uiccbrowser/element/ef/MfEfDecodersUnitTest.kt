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
class MfEfDecodersUnitTest {

    private lateinit var resources: Resources

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    }

    @Test
    fun decodeAtr_berTlvDataObjects_returnsElement() {
        val element = MfEfDecoders.decodeAtr(
                resources,
                hexStringToByteArray("4F07A000000087100250045553494D")
        )

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_atr_label))
        assertThat(element.subElements).hasSize(2)
        assertThat(element.subElements[0].label).isEqualTo("Application identifier '4F'")
        assertThat(element.subElements[1].label).isEqualTo("Application label '50'")
        assertThat(element.subElements[1].toString()).isEqualTo("5553494D (USIM)")
    }

    @Test
    fun decodeAtr_applicationTemplate_returnsNestedElements() {
        val element = MfEfDecoders.decodeAtr(
                resources,
                hexStringToByteArray("610D4F07A000000087100250025533")
        )

        assertThat(element).isNotNull()
        assertThat(element!!.subElements[0].label).isEqualTo("Application Template '61'")
        assertThat(element.subElements[0].subElements).hasSize(2)
        assertThat(element.subElements[0].subElements[0].label)
                .isEqualTo("Application identifier '4F'")
        assertThat(element.subElements[0].subElements[1].toString()).isEqualTo("5533 (U3)")
    }

    @Test
    fun decodeAtr_withPadding_returnsOnlyDataObjects() {
        val element = MfEfDecoders.decodeAtr(
                resources,
                hexStringToByteArray("4F07A0000000871002FFFFFFFF")
        )

        assertThat(element).isNotNull()
        assertThat(element!!.subElements).hasSize(1)
        assertThat(element.subElements[0].label).isEqualTo("Application identifier '4F'")
    }

    @Test
    fun decodePl_languageEntries_returnsElement() {
        val element = MfEfDecoders.decodePl(resources, hexStringToByteArray("656EFFFF"))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_pl_label))
        assertThat(element.subElements).hasSize(2)
        assertThat(element.subElements[0].label).isEqualTo("Preferred language 1")
        assertThat(element.subElements[0].toString()).isEqualTo("656E (en)")
        assertThat(element.subElements[1].label).isEqualTo("Preferred language 2")
        assertThat(element.subElements[1].toString()).isEqualTo("FFFF (Unused)")
    }

    @Test
    fun decodeUmpc_validData_returnsFields() {
        val element = MfEfDecoders.decodeUmpc(resources, hexStringToByteArray("0A1E03FFFF"))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_umpc_label))
        assertThat(element.subElements.map { it.label }).containsExactly(
                resources.getString(R.string.uicc_max_power_consumption_label),
                resources.getString(R.string.operator_timeout_label),
                resources.getString(R.string.additional_information_label),
                resources.getString(R.string.rfu_label)
        ).inOrder()
        assertThat(element.subElements[0].toString()).isEqualTo("0A (10 mA)")
        assertThat(element.subElements[1].toString()).isEqualTo("1E (30 seconds)")
        assertThat(element.subElements[2].toString())
                .isEqualTo("03 (Increased idle current required; UICC suspension supported)")
    }

    @Test
    fun decodeIccid_validData_interpretsSwappedBcdDigits() {
        val element = MfEfDecoders.decodeIccid(
                resources,
                hexStringToByteArray("2143658709FFFFFFFFFF")
        )

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_iccid_label))
        assertThat(element.subElements).hasSize(1)
        assertThat(element.subElements[0].label)
                .isEqualTo(resources.getString(R.string.identification_number_label))
        assertThat(element.subElements[0].toString())
                .isEqualTo("2143658709FFFFFFFFFF (1234567890)")
    }

    @Test
    fun decodeInvalidData_returnsNull() {
        assertThat(MfEfDecoders.decodeAtr(resources, byteArrayOf())).isNull()
        assertThat(MfEfDecoders.decodeAtr(resources, hexStringToByteArray("FFFFFFFF"))).isNull()
        assertThat(MfEfDecoders.decodePl(resources, hexStringToByteArray("65"))).isNull()
        assertThat(MfEfDecoders.decodeUmpc(resources, hexStringToByteArray("0A"))).isNull()
        assertThat(MfEfDecoders.decodeIccid(resources, hexStringToByteArray("2143"))).isNull()
    }

    @Test
    fun efDecoderRegistry_mfEfDecodersAreRegistered() {
        val registered = listOf(
                FileId.EF_ATR,
                FileId.EF_PL,
                FileId.EF_UMPC,
                FileId.EF_ICCID
        )

        registered.forEach { fileId ->
            assertThat(EfDecoderRegistry.has(FileId.AID_NONE, fileId)).isTrue()
            assertThat(EfDecoderRegistry.find(FileId.AID_NONE, fileId)).isNotNull()
        }
    }
}
