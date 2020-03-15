/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.io

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResponseUnitTest {
    @Test
    fun noData() {
        val response = Response(byteArrayOf(b(0x90), b(0x00)))
        assertThat(response.data.size).isEqualTo(0)
        assertThat(response.sw).isEqualTo(0x9000)
        assertThat(response.sw1).isEqualTo(0x90)
        assertThat(response.sw2).isEqualTo(0x00)
    }

    @Test
    fun shortData() {
        val response = Response(byteArrayOf(b(0xFF), b(0x6A), b(0x82)))
        assertThat(response.data).isEqualTo(byteArrayOf(b(0xFF)))
        assertThat(response.sw).isEqualTo(0x6A82)
        assertThat(response.sw1).isEqualTo(0x6A)
        assertThat(response.sw2).isEqualTo(0x82)
    }

    @Test
    fun longData() {
        val byteArray = ByteArray(65536 + 2) { i -> i.toByte() }
        byteArray[65535 + 1] = 0x9D.toByte()
        byteArray[65535 + 2] = 0x1A.toByte()
        val response = Response(byteArray)
        assertThat(response.data).isEqualTo(byteArray.copyOf(65536))
        assertThat(response.sw).isEqualTo(0x9D1A)
        assertThat(response.sw1).isEqualTo(0x9D)
        assertThat(response.sw2).isEqualTo(0x1A)
    }

    private fun b(byte: Int) = byte.toByte()
}
