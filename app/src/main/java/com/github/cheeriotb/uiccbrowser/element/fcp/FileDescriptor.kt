/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.element.fcp

/**
 * Classifies an ETSI TS 102 221 File descriptor byte.
 *
 * Bit b7, which indicates whether the file is shareable, does not affect the classification.
 * RFU values and invalid combinations are classified as [Type.UNKNOWN].
 */
object FileDescriptor {

    enum class Type {
        DF_OR_ADF,
        TRANSPARENT_EF,
        LINEAR_FIXED_EF,
        CYCLIC_EF,
        BER_TLV_EF,
        UNKNOWN
    }

    /** Returns the file type and structure represented by [descriptorByte]. */
    fun typeOf(descriptorByte: Byte): Type {
        val descriptor = descriptorByte.toInt() and 0xFF
        if (descriptor and RFU_B8_MASK != 0) return Type.UNKNOWN

        return when (descriptor and TYPE_AND_STRUCTURE_MASK) {
            DF_OR_ADF -> Type.DF_OR_ADF
            BER_TLV_EF -> Type.BER_TLV_EF
            WORKING_TRANSPARENT_EF, INTERNAL_TRANSPARENT_EF -> Type.TRANSPARENT_EF
            WORKING_LINEAR_FIXED_EF, INTERNAL_LINEAR_FIXED_EF -> Type.LINEAR_FIXED_EF
            WORKING_CYCLIC_EF, INTERNAL_CYCLIC_EF -> Type.CYCLIC_EF
            else -> Type.UNKNOWN
        }
    }

    private const val RFU_B8_MASK = 0x80
    private const val TYPE_AND_STRUCTURE_MASK = 0x3F

    private const val DF_OR_ADF = 0x38
    private const val BER_TLV_EF = 0x39
    private const val WORKING_TRANSPARENT_EF = 0x01
    private const val INTERNAL_TRANSPARENT_EF = 0x09
    private const val WORKING_LINEAR_FIXED_EF = 0x02
    private const val INTERNAL_LINEAR_FIXED_EF = 0x0A
    private const val WORKING_CYCLIC_EF = 0x06
    private const val INTERNAL_CYCLIC_EF = 0x0E
}
