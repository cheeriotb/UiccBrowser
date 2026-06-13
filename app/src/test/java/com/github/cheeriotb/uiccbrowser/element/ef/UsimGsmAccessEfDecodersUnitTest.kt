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
class UsimGsmAccessEfDecodersUnitTest {

    private lateinit var resources: Resources

    companion object {
        private const val AID_USIM = AppTemplate.RID + AppTemplate.APP_USIM
        private const val KC = "000102030405060706"
        private const val KC_NOT_AVAILABLE = "0001020304050607FF"
        private const val CPBCCH = "3485FF7A"
    }

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    }

    @Test
    fun decodeKc_validData_returnsKeyAndSequenceNumber() {
        val element = UsimGsmAccessEfDecoders.decodeKc(resources, hexStringToByteArray(KC))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_kc_label))
        assertThat(element.subElements).hasSize(2)
        assertThat(element.subElements[0].toString()).isEqualTo("0001020304050607")
        assertThat(element.subElements[1].toString()).isEqualTo("06 (6)")
        assertThat(element.byteArray).isEqualTo(hexStringToByteArray(KC))
    }

    @Test
    fun decodeKcGprs_keyNotAvailable_returnsInterpretation() {
        val element = UsimGsmAccessEfDecoders.decodeKcGprs(
                resources,
                hexStringToByteArray(KC_NOT_AVAILABLE)
        )

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_kc_gprs_label))
        assertThat(element.subElements[1].toString()).isEqualTo("FF (Key not available)")
    }

    @Test
    fun decodeCpbcch_validData_returnsIndexedCarrierBitFields() {
        val element = UsimGsmAccessEfDecoders.decodeCpbcch(
                resources,
                hexStringToByteArray(CPBCCH)
        )

        assertThat(element).isNotNull()
        assertThat(element!!.subElements).hasSize(2)
        assertThat(element.subElements[0].label).isEqualTo("CPBCCH carrier list element 1")
        assertThat(element.subElements[0].subElements).hasSize(4)
        assertThat(element.subElements[0].subElements[0].toString()).isEqualTo("3485 (308)")
        assertThat(element.subElements[0].subElements[1].toString()).isEqualTo("01 (Higher band)")
        assertThat(element.subElements[0].subElements[2].toString()).isEqualTo("00")
        assertThat(element.subElements[0].subElements[3].toString())
                .isEqualTo("01 (No valid CPBCCH carrier stored)")
        assertThat(element.subElements[1].subElements[0].toString()).isEqualTo("FF7A (767)")
        assertThat(element.subElements[1].subElements[1].toString()).isEqualTo("00 (Lower band)")
        assertThat(element.subElements[1].subElements[2].toString()).isEqualTo("0F")
        assertThat(element.subElements[1].subElements[3].toString())
                .isEqualTo("00 (Valid CPBCCH carrier stored)")
        assertThat(element.byteArray).isEqualTo(hexStringToByteArray(CPBCCH))
    }

    @Test
    fun decodeInvScan_knownAndRfuValues_returnsInterpretations() {
        assertThat(UsimGsmAccessEfDecoders.decodeInvScan(
                resources,
                byteArrayOf(0x00)
        )!!.subElements[0].toString()).isEqualTo("00 (Investigation scan not required)")
        assertThat(UsimGsmAccessEfDecoders.decodeInvScan(
                resources,
                byteArrayOf(0x01)
        )!!.subElements[0].toString()).isEqualTo("01 (Investigation scan required)")
        assertThat(UsimGsmAccessEfDecoders.decodeInvScan(
                resources,
                byteArrayOf(0x02)
        )!!.subElements[0].toString()).isEqualTo("02 (RFU)")
    }

    @Test
    fun decoders_invalidLengths_returnNull() {
        assertThat(UsimGsmAccessEfDecoders.decodeKc(resources, byteArrayOf())).isNull()
        assertThat(UsimGsmAccessEfDecoders.decodeKcGprs(resources, ByteArray(8))).isNull()
        assertThat(UsimGsmAccessEfDecoders.decodeCpbcch(resources, byteArrayOf())).isNull()
        assertThat(UsimGsmAccessEfDecoders.decodeCpbcch(resources, byteArrayOf(0x00))).isNull()
        assertThat(UsimGsmAccessEfDecoders.decodeInvScan(resources, ByteArray(2))).isNull()
    }

    @Test
    fun registry_containsGsmAccessDecoders() {
        val fileIds = listOf(
                FileId.EF_GSM_ACCESS_KC,
                FileId.EF_GSM_ACCESS_KC_GPRS,
                FileId.EF_GSM_ACCESS_CPBCCH,
                FileId.EF_GSM_ACCESS_INVSCAN
        )

        fileIds.forEach { fileId ->
            val path = FileId.PATH_ADF + FileId.DF_GSM_ACCESS + fileId
            assertThat(EfDecoderRegistry.has(AID_USIM, path)).isTrue()
            assertThat(EfDecoderRegistry.find(AID_USIM, path)).isNotNull()
        }
    }
}
