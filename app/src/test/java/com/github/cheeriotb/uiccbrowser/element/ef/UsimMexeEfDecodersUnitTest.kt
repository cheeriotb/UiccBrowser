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
import com.github.cheeriotb.uiccbrowser.element.EfDecoderRegistry
import com.github.cheeriotb.uiccbrowser.repository.FileId
import com.github.cheeriotb.uiccbrowser.util.hexStringToByteArray
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UsimMexeEfDecodersUnitTest {
    private lateinit var resources: Resources

    companion object {
        private const val AID_USIM = AppTemplate.RID + AppTemplate.APP_USIM
        private const val ROOT_PUBLIC_KEY = "0101034F5000100020044B455931"
        private const val THIRD_PARTY_ROOT_PUBLIC_KEY =
                "0101034F5000100020044B45593103434131"
    }

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    }

    @Test
    fun decodeMexeSt_validData_returnsIndexedServiceBits() {
        val element = UsimMexeEfDecoders.decodeMexeSt(resources, byteArrayOf(0x05))

        assertThat(element).isNotNull()
        assertThat(element!!.subElements).hasSize(8)
        assertThat(element.subElements[0].label).isEqualTo("Operator Root Public Key service")
        assertThat(element.subElements[0].toString()).isEqualTo("Available")
        assertThat(element.subElements[1].toString()).isEqualTo("Not available")
        assertThat(element.subElements[2].toString()).isEqualTo("Available")
        assertThat(element.byteArray).isEqualTo(byteArrayOf(0x05))
    }

    @Test
    fun decodeRootPublicKeys_validRecord_returnsAllDescriptorFields() {
        val bytes = hexStringToByteArray(ROOT_PUBLIC_KEY)
        val decoders = listOf(
                UsimMexeEfDecoders::decodeOrpk,
                UsimMexeEfDecoders::decodeArpk
        )

        decoders.forEach { decoder ->
            val element = decoder(resources, bytes)
            assertThat(element).isNotNull()
            assertThat(element!!.subElements).hasSize(8)
            assertThat(element.subElements[0].toString())
                    .isEqualTo("01 (Certificate descriptor valid)")
            assertThat(element.subElements[1].toString()).isEqualTo("01 (Authority certificate)")
            assertThat(element.subElements[2].toString()).isEqualTo("03 (RFU)")
            assertThat(element.subElements[4].toString()).isEqualTo("0010 (16)")
            assertThat(element.subElements[5].toString()).isEqualTo("0020 (32)")
            assertThat(element.subElements[6].toString()).isEqualTo("04 (4)")
            assertThat(element.subElements[7].toString()).isEqualTo("4B455931 (KEY1)")
            assertThat(element.byteArray).isEqualTo(bytes)
        }
    }

    @Test
    fun decodeTprpk_validRecord_returnsCertificateIdentifierFields() {
        val element = UsimMexeEfDecoders.decodeTprpk(
                resources, hexStringToByteArray(THIRD_PARTY_ROOT_PUBLIC_KEY)
        )

        assertThat(element).isNotNull()
        assertThat(element!!.subElements).hasSize(10)
        assertThat(element.subElements[8].toString()).isEqualTo("03 (3)")
        assertThat(element.subElements[9].toString()).isEqualTo("434131 (CA1)")
    }

    @Test
    fun decodeTkcdf_validData_returnsSingleDataElement() {
        val bytes = hexStringToByteArray("01020304")
        val element = UsimMexeEfDecoders.decodeTkcdf(resources, bytes)

        assertThat(element).isNotNull()
        assertThat(element!!.subElements).hasSize(1)
        assertThat(element.subElements[0].toString()).isEqualTo("01020304")
        assertThat(element.byteArray).isEqualTo(bytes)
    }

    @Test
    fun decoders_invalidData_returnNull() {
        assertThat(UsimMexeEfDecoders.decodeMexeSt(resources, byteArrayOf())).isNull()
        assertThat(UsimMexeEfDecoders.decodeOrpk(resources, ByteArray(9))).isNull()
        assertThat(UsimMexeEfDecoders.decodeArpk(resources, ByteArray(10))).isNotNull()
        assertThat(UsimMexeEfDecoders.decodeTprpk(
                resources, hexStringToByteArray("0000000000000000000000")
        )).isNotNull()
        assertThat(UsimMexeEfDecoders.decodeTprpk(
                resources, hexStringToByteArray("0000000000000000000001")
        )).isNull()
        assertThat(UsimMexeEfDecoders.decodeTkcdf(resources, byteArrayOf())).isNull()
    }

    @Test
    fun registry_containsFixedMexeDecoders() {
        val fileIds = listOf(
                FileId.EF_MEXE_ST,
                FileId.EF_MEXE_ORPK,
                FileId.EF_MEXE_ARPK,
                FileId.EF_MEXE_TPRPK
        )

        fileIds.forEach { fileId ->
            val path = FileId.PATH_ADF + FileId.DF_MEXE + fileId
            assertThat(EfDecoderRegistry.has(AID_USIM, path)).isTrue()
            assertThat(EfDecoderRegistry.find(AID_USIM, path)).isNotNull()
        }
    }
}
