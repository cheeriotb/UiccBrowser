/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.element

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.cheeriotb.uiccbrowser.util.BerTlv
import com.github.cheeriotb.uiccbrowser.util.Tlv
import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString
import com.github.cheeriotb.uiccbrowser.util.hexStringToByteArray
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BerTlvElementUnitTest {
    private lateinit var builder: BerTlvElement.Builder

    companion object {
        /*
           |  T | 21 | (1) A constructed BER-TLV contains a constructed one and a primitive one
           |  L | 0A |
           |  V |  T | 22 | (2) A constructed BER-TLV contains a primitive one and a constructed one
           |    |  L | 05 |
           |    |  V |  T | 01 | (3) A primitive BER-TLV
           |    |    |  L | 01 |
           |    |    |  V | 01 |
           |    |    |  T | 21 | (4) A constructed BER-TLV with no value
           |    |    |  L | 00 |
           |    |    |  V | -- |
           |    |  T | 02 | (5) A primitive BER-TLV
           |    |  L | 01 |
           |    |  V | 01 |
         */

        private const val BER_TLV_STRING_1 = "210A22050101012100020101"
        private const val BER_TLV_DATA_STRING_1 = "22050101012100020101"
        private const val LABEL_ID_1 = 0x1001
        private const val DESCRIPTION_ID_1 = 0x1002

        private const val BER_TLV_STRING_2 = "22050101012100"
        private const val BER_TLV_DATA_STRING_2 = "0101012100"
        private const val LABEL_ID_2 = 0x2001
        private const val DESCRIPTION_ID_2 = 0x2002

        private const val BER_TLV_STRING_3 = "010101"
        private const val BER_TLV_DATA_STRING_3 = "01"
        private const val LABEL_ID_3 = 0x3001
        private const val DESCRIPTION_ID_3 = 0x3002

        private const val BER_TLV_STRING_4 = "2100"
        private const val BER_TLV_DATA_STRING_4 = ""
        private const val LABEL_ID_4 = 0x4001
        private const val DESCRIPTION_ID_4 = 0x4002

        private const val BER_TLV_STRING_5 = "020101"
        private const val BER_TLV_DATA_STRING_5 = "01"
        private const val LABEL_ID_5 = 0x5001
        private const val DESCRIPTION_ID_5 = 0x5002

        private fun specificDecoder1(tlvs: List<Tlv>, parent: Element?): List<Element> {
            val elements = mutableListOf<Element>()

            tlvs.forEach { tlv ->
                val element = when (tlv.tag) {
                    0x22 -> {
                        BerTlvElement.Builder(tlv)
                            .editable(true)
                            .labelId(LABEL_ID_2)
                            .descriptionId(DESCRIPTION_ID_2)
                            .parent(parent)
                            .decoder(::specificDecoder2)
                            .interpreter { byteArrayToHexString(it) }
                            .build()
                    }
                    0x02 -> {
                        BerTlvElement.Builder(tlv)
                            .editable(true)
                            .labelId(LABEL_ID_5)
                            .descriptionId(DESCRIPTION_ID_5)
                            .parent(parent)
                            .interpreter { byteArrayToHexString(it) }
                            .build()
                    }
                    else -> return listOf()
                }
                elements.add(element)
            }

            return elements
        }

        private fun specificDecoder2(tlvs: List<Tlv>, parent: Element?): List<Element> {
            val elements = mutableListOf<Element>()

            tlvs.forEach { tlv ->
                val element = when (tlv.tag) {
                    0x01 -> {
                        BerTlvElement.Builder(tlv)
                            .editable(true)
                            .labelId(LABEL_ID_3)
                            .descriptionId(DESCRIPTION_ID_3)
                            .parent(parent)
                            .decoder(::specificDecoder2)
                            .validator { (it[0] == 0x01.toByte()) || (it[0] == 0x99.toByte()) }
                            .interpreter { byteArrayToHexString(it) }
                            .build()
                    }
                    0x21 -> {
                        BerTlvElement.Builder(tlv)
                            .editable(true)
                            .labelId(LABEL_ID_4)
                            .descriptionId(DESCRIPTION_ID_4)
                            .parent(parent)
                            .validator { it.isEmpty() || (it[0] == 0x01.toByte()) }
                            .interpreter { byteArrayToHexString(it) }
                            .build()
                    }
                    else -> return listOf()
                }
                elements.add(element)
            }

            return elements
        }
    }

    @Before
    fun setUp() {
        val data = hexStringToByteArray(BER_TLV_STRING_1)
        val tlvs = BerTlv.listFrom(data)

        builder = BerTlvElement.Builder(tlvs[0])
            .labelId(LABEL_ID_1)
            .descriptionId(DESCRIPTION_ID_1)
            .decoder(::specificDecoder1)
            .interpreter { byteArrayToHexString(it) }
    }

    @Test
    fun labelId() {
        val element = builder.build()

        assertThat(element.labelId).isEqualTo(LABEL_ID_1)
        assertThat(element.subElements[0].labelId).isEqualTo(LABEL_ID_2)
        assertThat(element.subElements[0].subElements[0].labelId).isEqualTo(LABEL_ID_3)
        assertThat(element.subElements[0].subElements[1].labelId).isEqualTo(LABEL_ID_4)
        assertThat(element.subElements[1].labelId).isEqualTo(LABEL_ID_5)
    }

    @Test
    fun descriptionId() {
        val element = builder.build()

        assertThat(element.descriptionId).isEqualTo(DESCRIPTION_ID_1)
        assertThat(element.subElements[0].descriptionId).isEqualTo(DESCRIPTION_ID_2)
        assertThat(element.subElements[0].subElements[0].descriptionId).isEqualTo(DESCRIPTION_ID_3)
        assertThat(element.subElements[0].subElements[1].descriptionId).isEqualTo(DESCRIPTION_ID_4)
        assertThat(element.subElements[1].descriptionId).isEqualTo(DESCRIPTION_ID_5)
    }

    @Test
    fun primitive() {
        val element = builder.build()

        assertThat(element.primitive).isFalse()
        assertThat(element.subElements[0].primitive).isFalse()
        assertThat(element.subElements[0].subElements[0].primitive).isTrue()
        assertThat(element.subElements[0].subElements[1].primitive).isFalse()
        assertThat(element.subElements[1].primitive).isTrue()
    }

    @Test
    fun data() {
        val element = builder.build()

        assertThat(element.data).isEqualTo(
                hexStringToByteArray(BER_TLV_DATA_STRING_1))
        assertThat(element.subElements[0].data).isEqualTo(
                hexStringToByteArray(BER_TLV_DATA_STRING_2))
        assertThat(element.subElements[0].subElements[0].data).isEqualTo(
                hexStringToByteArray(BER_TLV_DATA_STRING_3))
        assertThat(element.subElements[0].subElements[1].data).isEqualTo(
                hexStringToByteArray(BER_TLV_DATA_STRING_4))
        assertThat(element.subElements[1].data).isEqualTo(
                hexStringToByteArray(BER_TLV_DATA_STRING_5))
    }

    @Test
    fun subElements() {
        val element = builder.build()

        assertThat(element.subElements.size).isEqualTo(2)
        assertThat(element.subElements[0].subElements.size).isEqualTo(2)
        assertThat(element.subElements[0].subElements[0].subElements.size).isEqualTo(0)
        assertThat(element.subElements[0].subElements[1].subElements.size).isEqualTo(0)
        assertThat(element.subElements[1].subElements.size).isEqualTo(0)
    }

    @Test
    fun rootElement() {
        val element = builder.build()

        assertThat(element.rootElement).isEqualTo(element)
        assertThat(element.subElements[0].rootElement).isEqualTo(element)
        assertThat(element.subElements[0].subElements[0].rootElement).isEqualTo(element)
        assertThat(element.subElements[0].subElements[1].rootElement).isEqualTo(element)
        assertThat(element.subElements[1].rootElement).isEqualTo(element)
    }

    @Test
    fun toByteArray() {
        val element = builder.build()

        assertThat(element.toByteArray()).isEqualTo(
                hexStringToByteArray(BER_TLV_STRING_1))
        assertThat(element.subElements[0].toByteArray()).isEqualTo(
                hexStringToByteArray(BER_TLV_STRING_2))
        assertThat(element.subElements[0].subElements[0].toByteArray()).isEqualTo(
                hexStringToByteArray(BER_TLV_STRING_3))
        assertThat(element.subElements[0].subElements[1].toByteArray()).isEqualTo(
                hexStringToByteArray(BER_TLV_STRING_4))
        assertThat(element.subElements[1].toByteArray()).isEqualTo(
                hexStringToByteArray(BER_TLV_STRING_5))
    }

    @Test
    fun setData_primitive_valid() {
        val element = builder.build()

        // The primitive BER-TLV 3 allows only '01' and '99' in this test scenario.
        val newData = byteArrayOf(0x99.toByte())
        assertThat(element.subElements[0].subElements[0].setData(newData)).isTrue()
        assertThat(element.subElements[0].subElements[0].data).isEqualTo(newData)
    }

    @Test
    fun setData_primitive_validForParent() {
        val element = builder.validator { it.size <= 137 }.build()

        // The primitive BER-TLV 5 has no validator, but the parent BET-TLV 1 has it.
        // The constructed BER-TLV 1 allows less or equal to 137 bytes data.
        val newData = ByteArray(127) { it.toByte() }
        assertThat(element.subElements[1].setData(newData)).isTrue()
        assertThat(element.subElements[1].data).isEqualTo(newData)
    }

    @Test
    fun setData_primitive_invalid() {
        val element = builder.build()

        // The primitive BER-TLV 3 allows only '01' and '99' in this test scenario.
        // The data '80' shall be rejected and the original data '01' shall be retained.
        assertThat(element.subElements[0].subElements[0].setData(
                byteArrayOf(0x80.toByte()))).isFalse()
        assertThat(element.subElements[0].subElements[0].data).isEqualTo(
                hexStringToByteArray(BER_TLV_DATA_STRING_3))
    }

    @Test
    fun setData_primitive_invalidForParent() {
        val element = builder.validator { it.size <= 137 }.build()

        // The primitive BER-TLV 5 has no validator, but the parent BET-TLV 1 has it.
        // The constructed BER-TLV 1 does not allow more than 137 bytes data.
        // The original data shall be retained.
        assertThat(element.subElements[1].setData(ByteArray(128) { it.toByte() })).isFalse()
        assertThat(element.subElements[1].data).isEqualTo(
                hexStringToByteArray(BER_TLV_DATA_STRING_5))
    }

    @Test
    fun setData_constructed() {
        val element = builder.build()
        val berTlvString2Alt = "0101992103010101"

        // New data for the BET-TLV 2 which contains the BER-TLV 3 and 4 shall be accepted.
        assertThat(element.subElements[0].setData(hexStringToByteArray(berTlvString2Alt))).isTrue()

        assertThat(element.subElements[0].subElements[0].labelId).isEqualTo(LABEL_ID_3)
        assertThat(element.subElements[0].subElements[0].descriptionId).isEqualTo(DESCRIPTION_ID_3)
        assertThat(element.subElements[0].subElements[0].data).isEqualTo(
                byteArrayOf(0x99.toByte()))

        assertThat(element.subElements[0].subElements[1].labelId).isEqualTo(LABEL_ID_4)
        assertThat(element.subElements[0].subElements[1].descriptionId).isEqualTo(DESCRIPTION_ID_4)
        assertThat(element.subElements[0].subElements[1].data).isEqualTo(
                byteArrayOf(0x01.toByte(), 0x01.toByte(), 0x01.toByte()))
    }

    @Test
    fun setData_constructed_invalid() {
        val element = builder.build()
        val berTlvString2Alt = "0101992103020101"

        // The first byte '02' of the data will not be allowed by the BER-TLV 4 in this test.
        // Both the BER-TLV 3 and 4 shall not be changed in this scenario.
        assertThat(element.subElements[0].setData(hexStringToByteArray(berTlvString2Alt))).isFalse()
        assertThat(element.subElements[0].subElements[0].data).isEqualTo(
                hexStringToByteArray(BER_TLV_DATA_STRING_3))
        assertThat(element.subElements[0].subElements[1].data).isEqualTo(
                hexStringToByteArray(BER_TLV_DATA_STRING_4))
    }

    @Test
    fun interpreter() {
        val element = builder.build()

        assertThat(element.toString()).isEqualTo(BER_TLV_DATA_STRING_1)
        assertThat(element.subElements[0].toString()).isEqualTo(BER_TLV_DATA_STRING_2)
        assertThat(element.subElements[0].subElements[0].toString()).isEqualTo(
                BER_TLV_DATA_STRING_3)
        assertThat(element.subElements[0].subElements[1].toString()).isEqualTo(
                BER_TLV_DATA_STRING_4)
        assertThat(element.subElements[1].toString()).isEqualTo(BER_TLV_DATA_STRING_5)
    }
}
