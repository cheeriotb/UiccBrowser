/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.util

fun byteToHexString(byte: Int) = "%02X".format(byte % 0x100)

fun extendedBytesToHexString(byte: Int) = "%06X".format(byte % 0x10000)

fun hexStringToByteArray(hex: String): ByteArray {
    var index = 0
    val array = ByteArray(hex.length / 2)
    while (index < array.count()) {
        val pointer = index * 2
        array[index] = hex.substring(pointer, pointer + 2).toInt(16).toByte()
        index++
    }
    return array
}

fun byteArrayToHexString(bytes: ByteArray): String {
    val builder = StringBuilder()
    for (byte in bytes) {
        builder.append("%02X".format(byte.toInt() and 0xFF))
    }
    return builder.toString()
}
