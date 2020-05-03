/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.util

import android.util.Log

class BerTlv private constructor(
    tag: Int,
    valueArg: ByteArray
) : Tlv(tag, valueArg) {
    // Up to 3 bytes are supported as a tag field in this implementation.
    private val numberOfTagBytes: Int
        get() {
            return when {
                (tag < 0x100) -> 1
                (tag < 0x10000) -> 2
                else -> 3
            }
        }

    // True if the value field contains BER-TLV objects (constructed), false otherwise (primitive).
    override val isConstructed: Boolean
        get() {
            val identifierOctet = tag shr ((numberOfTagBytes - 1) * 8)
            return (identifierOctet and 0x20) != 0x00
        }

    companion object {
        private val TAG = BerTlv::class.java.simpleName

        fun listFrom(bytes: ByteArray): List<Tlv> {
            val list = mutableListOf<Tlv>()
            var index = 0
            try {
                while (index < bytes.size) {
                    var tag = (bytes[index++].toInt() and 0xFF)
                    // If all the bits for the tag number (5 bits from LSB) are set,
                    // the tag field is coded in multi byte format.
                    if ((tag and 0x1F) == 0x1F) {
                        tag = (tag shl 8) + (bytes[index++].toInt() and 0xFF)
                        // If MSB in the second byte is set, it is not the last byte for the tag.
                        if ((tag and 0x80) == 0x80) {
                            tag = (tag shl 8) + (bytes[index++].toInt() and 0xFF)
                            // Up to 3 bytes are supported as a tag field in this implementation.
                            if ((tag and 0x80) == 0x80) {
                                throw IllegalArgumentException("Unsupported tag field length")
                            }
                        }
                    }
                    var length = (bytes[index++].toInt() and 0xFF)
                    if (length > 0x7F) {
                        length = when (length) {
                            0x81 -> (bytes[index++].toInt() and 0xFF)
                            0x82 -> {
                                (((bytes[index++].toInt() and 0xFF) shl 8)
                                        + (bytes[index++].toInt() and 0xFF))
                            }
                            0x83 -> {
                                (((bytes[index++].toInt() and 0xFF) shl 16)
                                        + ((bytes[index++].toInt() and 0xFF) shl 8)
                                        + (bytes[index++].toInt() and 0xFF))
                            }
                            // The length is coded onto 1, 2, 3 or 4 bytes
                            // according to the clause 7.1.2 "Length encoding" in ETSI TS 101 220
                            // The definite form with > 4 bytes and the indefinite form are unused.
                            else -> throw IllegalArgumentException("Unsupported length format")
                        }
                    }
                    list.add(BerTlv(tag, if (length == 0) byteArrayOf() else
                            bytes.sliceArray(IntRange(index, index + length - 1))))
                    index += length
                }
            } catch (e: Exception) {
                // Something is wrong in the BET-TLV data structure.
                Log.w(TAG, "Something is wrong in the data structure.", e)
                return mutableListOf()
            }
            return list
        }
    }

    override fun listFrom(bytes: ByteArray): List<Tlv> = BerTlv.listFrom(bytes)

    override fun toByteArray(): ByteArray {
        // Up to 3 bytes are supported as a tag field in this implementation.
        val tagArray = hexStringToByteArray(
            when (numberOfTagBytes) {
                1 -> "%02X"
                2 -> "%04X"
                else -> "%06X"
            }.format(tag)
        )
        val valueArray = value
        // The length field is coded onto 1, 2, 3 or 4 bytes
        // according to the clause 7.1.2 "Length encoding" in ETSI TS 101 220.
        val lengthArray = hexStringToByteArray(
            when (valueArray.size) {
                in 0x00..0x7F -> "%02X"          // 1 byte  : '00' to '7F'
                in 0x80..0xFF -> "81%02X"        // 2 bytes : '81' + '00' to 'FF'
                in 0x100..0xFFFF -> "82%04X"     // 3 bytes : '82' + '0100' to 'FFFF'
                in 0x10000..0xFFFFFF -> "83%06X" // 4 bytes : '83' + '010000' to 'FFFFFF'
                else -> return byteArrayOf()     // Something is wrong if the array size is zero.
            }.format(valueArray.size)
        )
        return tagArray + lengthArray + valueArray
    }
}
