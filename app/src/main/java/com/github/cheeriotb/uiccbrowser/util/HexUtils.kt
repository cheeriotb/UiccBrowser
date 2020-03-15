/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.util

fun byteToHexString(byte: Int) = "%02X".format(byte.toByte())

fun hexStringToByte(hexString: String) = hexString.toInt(16).toByte()

fun extendedBytesToHexString(byte: Int) = "%04X".format(byte and 0xFFFF)

fun hexStringToByteArray(hexString: String): ByteArray {
    val array = ByteArray(hexString.length / 2)
    for (index in 0 until array.count()) {
        val pointer = index * 2
        array[index] = hexStringToByte(hexString.substring(pointer, pointer + 2))
    }
    return array
}

fun byteArrayToHexString(bytes: ByteArray): String {
    val builder = StringBuilder()
    for (byte in bytes) {
        builder.append(byteToHexString(byte.toInt()))
    }
    return builder.toString()
}
