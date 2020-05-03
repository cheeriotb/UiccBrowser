/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.util

abstract class Tlv protected constructor(
    val tag: Int,
    valueArg: ByteArray
) {
    // True if the value field contains TLV objects (constructed), false otherwise (primitive).
    abstract val isConstructed: Boolean
    // Contains valid value if this object is for a primitive value
    private val primitiveValue: ByteArray = if (isConstructed) byteArrayOf() else valueArg
    // Contains valid value if this object is for a constructed value
    val tlvs: List<Tlv> = if (isConstructed) listFrom(valueArg) else listOf()

    // This represents length in addition to value itself.
    val value: ByteArray
        get() {
            if (!isConstructed) return primitiveValue
            var array = byteArrayOf()
            for (tlv in tlvs) {
                array += tlv.toByteArray()
            }
            return array
        }

    abstract fun listFrom(bytes: ByteArray): List<Tlv>
    abstract fun toByteArray(): ByteArray

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Tlv

        if (tag != other.tag) return false
        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tag
        result = 31 * result + value.contentHashCode()
        return result
    }
}
