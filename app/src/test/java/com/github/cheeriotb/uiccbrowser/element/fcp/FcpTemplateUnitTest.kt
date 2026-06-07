/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.element.fcp

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.github.cheeriotb.uiccbrowser.element.BerTlvElement
import com.github.cheeriotb.uiccbrowser.util.hexStringToByteArray
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FcpTemplateUnitTest {

    private lateinit var resources: Resources

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    }

    @Test
    fun fileDescriptorByteInterpreter_knownValues_returnsDescriptions() {
        val interpretations = mapOf(
            0x78 to "78 (Sharable / DF or ADF)",
            0x41 to "41 (Sharable / Transparent)",
            0x02 to "02 (Not sharable / Linear Fixed)",
            0x46 to "46 (Sharable / Cyclic)",
            0x39 to "39 (Not sharable / BER-TLV)"
        )

        interpretations.forEach { (descriptor, expected) ->
            assertThat(fileDescriptorByteInterpretation(descriptor)).isEqualTo(expected)
        }
    }

    @Test
    fun fileDescriptorByteInterpreter_b8Set_returnsHexOnly() {
        assertThat(fileDescriptorByteInterpretation(0x88)).isEqualTo("88")
    }

    @Test
    fun fileDescriptorByteInterpreter_unknownStructure_returnsHexOnly() {
        assertThat(fileDescriptorByteInterpretation(0x00)).isEqualTo("00")
        assertThat(fileDescriptorByteInterpretation(0x3A)).isEqualTo("3A")
    }

    @Test
    fun fileDescriptorByteInterpreter_nonDfFileType_usesEfStructure() {
        assertThat(fileDescriptorByteInterpretation(0x11))
            .isEqualTo("11 (Not sharable / Transparent)")
    }

    private fun fileDescriptorByteInterpretation(descriptor: Int): String {
        val bytes = hexStringToByteArray("62048202%02X21".format(descriptor))
        val fcp = FcpTemplate.decode(resources, bytes)!!
        val fileDescriptor = fcp.subElements
            .filterIsInstance<BerTlvElement>()
            .first { it.tag == FcpTemplate.TAG_FILE_DESCRIPTOR }
        return fileDescriptor.subElements.first().toString()
    }
}
