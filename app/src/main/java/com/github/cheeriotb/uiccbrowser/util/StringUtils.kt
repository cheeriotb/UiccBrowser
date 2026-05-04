/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.util

import java.nio.charset.StandardCharsets

object StringUtils {
    /**
     * Decodes binary data to String based on ETSI TS 102 221 Annex A
     */
    fun decode(data: ByteArray): String {
        if (data.isEmpty()) return ""

        return when (data[0].toInt() and 0xFF) {
            0x80 -> decodeUCS2A2(data) // Annex A.2: UCS2 80 coding
            0x81 -> decodeUCS2A3(data, 0x81) // Annex A.3: UCS2 81 coding
            0x82 -> decodeUCS2A3(data, 0x82) // Annex A.3: UCS2 82 coding
            else -> decodeGSM7Bit(data) // Annex A.1: GSM 7-bit (or default alphabet)
        }
    }

    // Annex A.2: Standard UCS2 coding (prefixed with 0x80)
    private fun decodeUCS2A2(data: ByteArray): String {
        // 先頭の0x80を除いた残りをUTF-16BEとして解釈
        return String(data, 1, data.size - 1, StandardCharsets.UTF_16BE)
            .trimEnd { it == '\uFFFF' || it == '\u0000' }
    }

    // Annex A.3: Compressed UCS2 (0x81 or 0x82)
    private fun decodeUCS2A3(data: ByteArray, type: Int): String {
        if (data.size < 3) return ""
        val len = data[1].toInt() and 0xFF
        val basePointer = (data[2].toInt() and 0xFF) shl 7
        val sb = StringBuilder()

        val offset = if (type == 0x81) 3 else 4

        for (i in offset until data.size) {
            val b = data[i].toInt() and 0xFF
            if (b and 0x80 != 0) {
                val charCode = basePointer + (b and 0x7F)
                sb.append(charCode.toChar())
            } else {
                sb.append(b.toChar())
            }
        }
        return sb.toString()
    }

    // Annex A.1: GSM 7-bit default alphabet
    private fun decodeGSM7Bit(data: ByteArray): String {
        return String(data, StandardCharsets.US_ASCII)
            .trimEnd { it == '\uFFFF' || it == '\u0000' }
    }
}
