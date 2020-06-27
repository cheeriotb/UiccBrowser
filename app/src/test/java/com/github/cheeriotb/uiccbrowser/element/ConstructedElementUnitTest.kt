/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.element

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.cheeriotb.uiccbrowser.R
import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString
import com.github.cheeriotb.uiccbrowser.util.hexStringToByteArray
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConstructedElementUnitTest {
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    private lateinit var builder: ConstructedElement.Builder

    companion object {
        /*
          Element 1 : 1 byte
          Element 2 : Element 3 (8 bytes) + Element 4 (8 bytes)
          Element 5 : 3 bytes
         */
        private const val DATA_1_STRING = "81"
        private const val DATA_1_SIZE = (DATA_1_STRING.length / 2)
        private const val LABEL_ID_1 = R.string.test1

        private const val DATA_3_STRING = "5465737400000000"
        private const val DATA_3_SIZE = (DATA_3_STRING.length / 2)
        private const val LABEL_ID_3 = R.string.test3

        private const val DATA_4_STRING = "0000000000000000"
        private const val DATA_4_SIZE = (DATA_4_STRING.length / 2)
        private const val LABEL_ID_4 = R.string.test4

        private const val DATA_2_STRING = DATA_3_STRING + DATA_4_STRING
        private const val DATA_2_SIZE = (DATA_2_STRING.length / 2)
        private const val LABEL_ID_2 = R.string.test2

        private const val DATA_5_STRING = "010203"
        private const val DATA_5_SIZE = (DATA_5_STRING.length / 2)
        private const val LABEL_ID_5 = R.string.test5

        private const val ALL_DATA_STRING = DATA_1_STRING + DATA_2_STRING + DATA_5_STRING
        private const val ALL_DATA_SIZE = (ALL_DATA_STRING.length / 2)
        private const val LABEL_ID_0 = R.string.test0

        private fun specificDecoder1(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val data1 = rawData.sliceArray(IntRange(0, DATA_1_SIZE - 1))
            val data2 = rawData.sliceArray(IntRange(DATA_1_SIZE, (DATA_1_SIZE + DATA_2_SIZE - 1)))
            val data5 = rawData.sliceArray(IntRange(DATA_1_SIZE + DATA_2_SIZE,
                    DATA_1_SIZE + DATA_2_SIZE + DATA_5_SIZE - 1))

            val list = mutableListOf<Element>()

            list.add(PrimitiveElement.Builder(data1)
                    .editable(false)
                    .labelId(LABEL_ID_1)
                    .parent(parent)
                    .validator { true }
                    .interpreter { _, b -> "%04X".format(LABEL_ID_1) + byteArrayToHexString(b) }
                    .build(resources))

            list.add(ConstructedElement.Builder(data2)
                    .editable(true)
                    .labelId(LABEL_ID_2)
                    .parent(parent)
                    .decoder(::specificDecoder2)
                    .validator { it.size == DATA_2_SIZE }
                    .interpreter { _, b -> "%04X".format(LABEL_ID_2) + byteArrayToHexString(b) }
                    .build(resources))

            list.add(PrimitiveElement.Builder(data5)
                    .editable(true)
                    .labelId(LABEL_ID_5)
                    .parent(parent)
                    .validator { it.size == DATA_5_SIZE }
                    .interpreter { _, b -> "%04X".format(LABEL_ID_5) + byteArrayToHexString(b) }
                    .build(resources))

            return list
        }

        private fun specificDecoder2(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val data3 = rawData.sliceArray(IntRange(0, DATA_3_SIZE - 1))
            val data4 = rawData.sliceArray(IntRange(DATA_3_SIZE, DATA_3_SIZE + DATA_4_SIZE - 1))

            val list = mutableListOf<Element>()

            list.add(PrimitiveElement.Builder(data3)
                    .editable(false)
                    .labelId(LABEL_ID_3)
                    .parent(parent)
                    .validator { true }
                    .interpreter { _, b -> "%04X".format(LABEL_ID_3) + byteArrayToHexString(b) }
                    .build(resources))

            list.add(PrimitiveElement.Builder(data4)
                    .editable(true)
                    .labelId(LABEL_ID_4)
                    .parent(parent)
                    .validator { it.size == DATA_4_SIZE }
                    .interpreter { _, b -> "%04X".format(LABEL_ID_4) + byteArrayToHexString(b) }
                    .build(resources))

            return list
        }
    }

    @Before
    fun setUp() {
        builder = ConstructedElement.Builder(hexStringToByteArray(ALL_DATA_STRING))
                .editable(true)
                .labelId(LABEL_ID_0)
                .decoder(::specificDecoder1)
                .validator { it.size == ALL_DATA_SIZE }
                .interpreter { _, b -> "%04X".format(LABEL_ID_0) + byteArrayToHexString(b) }
    }

    @Test
    fun label() {
        val element = builder.build(resources)

        assertThat(element.label).isEqualTo(
                resources.getString(LABEL_ID_0))
        assertThat(element.subElements[0].label).isEqualTo(
                resources.getString(LABEL_ID_1))
        assertThat(element.subElements[1].label).isEqualTo(
                resources.getString(LABEL_ID_2))
        assertThat(element.subElements[1].subElements[0].label).isEqualTo(
                resources.getString(LABEL_ID_3))
        assertThat(element.subElements[1].subElements[1].label).isEqualTo(
                resources.getString(LABEL_ID_4))
        assertThat(element.subElements[2].label).isEqualTo(
                resources.getString(LABEL_ID_5))
    }

    @Test
    fun primitive() {
        val element = builder.build(resources)

        assertThat(element.primitive).isFalse()
        assertThat(element.subElements[0].primitive).isTrue()
        assertThat(element.subElements[1].primitive).isFalse()
        assertThat(element.subElements[1].subElements[0].primitive).isTrue()
        assertThat(element.subElements[1].subElements[1].primitive).isTrue()
        assertThat(element.subElements[2].primitive).isTrue()
    }

    @Test
    fun data() {
        val element = builder.build(resources)

        assertThat(element.data).isEqualTo(
                hexStringToByteArray(ALL_DATA_STRING))
        assertThat(element.subElements[0].data).isEqualTo(
                hexStringToByteArray(DATA_1_STRING))
        assertThat(element.subElements[1].data).isEqualTo(
                hexStringToByteArray(DATA_2_STRING))
        assertThat(element.subElements[1].subElements[0].data).isEqualTo(
                hexStringToByteArray(DATA_3_STRING))
        assertThat(element.subElements[1].subElements[1].data).isEqualTo(
                hexStringToByteArray(DATA_4_STRING))
        assertThat(element.subElements[2].data).isEqualTo(
                hexStringToByteArray(DATA_5_STRING))
    }

    @Test
    fun subElements() {
        val element = builder.build(resources)

        assertThat(element.subElements.size).isEqualTo(3)
        assertThat(element.subElements[0].subElements.size).isEqualTo(0)
        assertThat(element.subElements[1].subElements.size).isEqualTo(2)
        assertThat(element.subElements[1].subElements[0].subElements.size).isEqualTo(0)
        assertThat(element.subElements[1].subElements[1].subElements.size).isEqualTo(0)
        assertThat(element.subElements[2].subElements.size).isEqualTo(0)
    }

    @Test
    fun rootElement() {
        val element = builder.build(resources)

        assertThat(element.rootElement).isEqualTo(element)
        assertThat(element.subElements[0].rootElement).isEqualTo(element)
        assertThat(element.subElements[1].rootElement).isEqualTo(element)
        assertThat(element.subElements[1].subElements[0].rootElement).isEqualTo(element)
        assertThat(element.subElements[1].subElements[1].rootElement).isEqualTo(element)
        assertThat(element.subElements[2].rootElement).isEqualTo(element)
    }

    @Test
    fun byteArray() {
        val element = builder.build(resources)

        assertThat(element.byteArray).isEqualTo(
                hexStringToByteArray(ALL_DATA_STRING))
        assertThat(element.subElements[0].byteArray).isEqualTo(
                hexStringToByteArray(DATA_1_STRING))
        assertThat(element.subElements[1].byteArray).isEqualTo(
                hexStringToByteArray(DATA_2_STRING))
        assertThat(element.subElements[1].subElements[0].byteArray).isEqualTo(
                hexStringToByteArray(DATA_3_STRING))
        assertThat(element.subElements[1].subElements[1].byteArray).isEqualTo(
                hexStringToByteArray(DATA_4_STRING))
        assertThat(element.subElements[2].byteArray).isEqualTo(
                hexStringToByteArray(DATA_5_STRING))
    }

    @Test
    fun setData_all() {
        val element = builder.build(resources)

        val data1StringAlt = "91"
        val allDataStringAlt = data1StringAlt + DATA_2_STRING + DATA_5_STRING
        assertThat(element.setData(resources, hexStringToByteArray(allDataStringAlt))).isTrue()

        assertThat(element.subElements[0].label).isEqualTo(
            resources.getString(LABEL_ID_1))
        assertThat(element.subElements[1].label).isEqualTo(
            resources.getString(LABEL_ID_2))
        assertThat(element.subElements[1].subElements[0].label).isEqualTo(
            resources.getString(LABEL_ID_3))
        assertThat(element.subElements[1].subElements[1].label).isEqualTo(
            resources.getString(LABEL_ID_4))
        assertThat(element.subElements[2].label).isEqualTo(
            resources.getString(LABEL_ID_5))

        assertThat(element.subElements[0].data).isEqualTo(
                hexStringToByteArray(data1StringAlt))
        assertThat(element.subElements[1].data).isEqualTo(
                hexStringToByteArray(DATA_2_STRING))
        assertThat(element.subElements[1].subElements[0].data).isEqualTo(
                hexStringToByteArray(DATA_3_STRING))
        assertThat(element.subElements[1].subElements[1].data).isEqualTo(
                hexStringToByteArray(DATA_4_STRING))
        assertThat(element.subElements[2].data).isEqualTo(
                hexStringToByteArray(DATA_5_STRING))
    }

    @Test
    fun setData_data2() {
        val element = builder.build(resources)

        val data3StringAlt = "0001020304050607"
        val data2StringAlt = data3StringAlt + DATA_4_STRING
        assertThat(element.subElements[1].setData(
                resources, hexStringToByteArray(data2StringAlt))).isTrue()

        assertThat(element.subElements[1].subElements[0].label).isEqualTo(
            resources.getString(LABEL_ID_3))
        assertThat(element.subElements[1].subElements[1].label).isEqualTo(
            resources.getString(LABEL_ID_4))

        assertThat(element.subElements[1].subElements[0].data).isEqualTo(
                hexStringToByteArray(data3StringAlt))
        assertThat(element.subElements[1].subElements[1].data).isEqualTo(
                hexStringToByteArray(DATA_4_STRING))
    }

    @Test
    fun setData_data4() {
        val element = builder.build(resources)

        val data4StringAlt = "0001020304050607"
        assertThat(element.subElements[1].subElements[1].setData(
                resources, hexStringToByteArray(data4StringAlt))).isTrue()

        assertThat(element.subElements[1].subElements[1].label).isEqualTo(
            resources.getString(LABEL_ID_4))

        assertThat(element.subElements[1].subElements[1].data).isEqualTo(
            hexStringToByteArray(data4StringAlt))
    }

    @Test
    fun setData_notEditable() {
        val element = builder.build(resources)

        assertThat(element.subElements[0].setData(
                resources, hexStringToByteArray(DATA_1_STRING))).isFalse()
    }

    @Test
    fun setData_notValid() {
        val element = builder.build(resources)

        val data4StringAlt = "000102030405060708"
        val data2StringAlt = DATA_3_STRING + data4StringAlt
        assertThat(element.subElements[1].setData(
                resources, hexStringToByteArray(data2StringAlt))).isFalse()

        assertThat(element.subElements[1].subElements[0].label).isEqualTo(
            resources.getString(LABEL_ID_3))
        assertThat(element.subElements[1].subElements[1].label).isEqualTo(
            resources.getString(LABEL_ID_4))

        assertThat(element.subElements[1].subElements[0].data).isEqualTo(
                hexStringToByteArray(DATA_3_STRING))
        assertThat(element.subElements[1].subElements[1].data).isEqualTo(
                hexStringToByteArray(DATA_4_STRING))
    }

    @Test
    fun interpreter() {
        val element = builder.build(resources)

        assertThat(element.toString()).isEqualTo("%04X$ALL_DATA_STRING".format(LABEL_ID_0))
        assertThat(element.subElements[0].toString()).isEqualTo(
                "%04X$DATA_1_STRING".format(LABEL_ID_1))
        assertThat(element.subElements[1].toString()).isEqualTo(
                "%04X$DATA_2_STRING".format(LABEL_ID_2))
        assertThat(element.subElements[1].subElements[0].toString()).isEqualTo(
                "%04X$DATA_3_STRING".format(LABEL_ID_3))
        assertThat(element.subElements[1].subElements[1].toString()).isEqualTo(
                "%04X$DATA_4_STRING".format(LABEL_ID_4))
        assertThat(element.subElements[2].toString()).isEqualTo(
                "%04X$DATA_5_STRING".format(LABEL_ID_5))
    }
}
