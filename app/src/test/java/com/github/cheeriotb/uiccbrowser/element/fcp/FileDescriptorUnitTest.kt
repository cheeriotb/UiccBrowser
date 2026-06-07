/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.element.fcp

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FileDescriptorUnitTest {

    @Test
    fun typeOf_validDescriptorBytes_returnsExpectedTypes() {
        val descriptors = mapOf(
            0x38 to FileDescriptor.Type.DF_OR_ADF,
            0x78 to FileDescriptor.Type.DF_OR_ADF,
            0x01 to FileDescriptor.Type.TRANSPARENT_EF,
            0x49 to FileDescriptor.Type.TRANSPARENT_EF,
            0x02 to FileDescriptor.Type.LINEAR_FIXED_EF,
            0x4A to FileDescriptor.Type.LINEAR_FIXED_EF,
            0x06 to FileDescriptor.Type.CYCLIC_EF,
            0x4E to FileDescriptor.Type.CYCLIC_EF,
            0x39 to FileDescriptor.Type.BER_TLV_EF,
            0x79 to FileDescriptor.Type.BER_TLV_EF
        )

        descriptors.forEach { (descriptor, expected) ->
            assertThat(FileDescriptor.typeOf(descriptor.toByte())).isEqualTo(expected)
        }
    }

    @Test
    fun typeOf_berTlvEf_isNotTransparentOrDf() {
        val type = FileDescriptor.typeOf(0x79.toByte())

        assertThat(type).isEqualTo(FileDescriptor.Type.BER_TLV_EF)
        assertThat(type).isNotEqualTo(FileDescriptor.Type.TRANSPARENT_EF)
        assertThat(type).isNotEqualTo(FileDescriptor.Type.DF_OR_ADF)
    }

    @Test
    fun typeOf_rfuAndInvalidCombinations_returnsUnknown() {
        val descriptors = listOf(0x00, 0x11, 0x3A, 0x81)

        descriptors.forEach {
            assertThat(FileDescriptor.typeOf(it.toByte())).isEqualTo(FileDescriptor.Type.UNKNOWN)
        }
    }
}
