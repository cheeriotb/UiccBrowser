/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.io

class Command(
    val ins: Int,
    val p1: Int = 0x00,
    val p2: Int = 0x00,
    val data: String = NO_COMMAND_DATA,
    val le: Int = NO_EXPECTED_DATA
) {
    constructor(
        ins: Int,
        p1: Int = 0x00,
        p2: Int = 0x00,
        le: Int
    ) : this(ins, p1, p2, NO_COMMAND_DATA, le)

    companion object {
        private const val NO_COMMAND_DATA = ""
        private const val NO_EXPECTED_DATA = 0
        private const val FURTHER_INTER_INDUSTRY_CLASS = 0x40
    }

    /** Represents Command chaining control */
    enum class Chaining(val value: Int) {
        /** The command is the last or only command of a chain */
        LAST_OR_ONLY(0),
        /** The command is not the last command of a chain */
        NOT_LAST(1)
    }

    /** Represents Secure messaging indication */
    enum class SecureMessaging(val value: Int) {
        /** No SM or no indication */
        NO(0),
        /** Proprietary SM */
        PROPRIETARY(1),
        /** SM (command header not processed) */
        HEADER_NOT_PROCESSED(2),
        /** SM (command header authenticated) */
        HEADER_AUTHENTICATED(3)
    }

    val cla: Int
        get() = cla()

    var ccc: Chaining = Chaining.LAST_OR_ONLY
    var smi: SecureMessaging = SecureMessaging.NO

    val lc: Int
        get() = data.length / 2

    init {
        require(ins in 0..255) { "INS must not be greater than 255" }
        require((ins and 0xF0) != 0x60 && (ins and 0xF0) != 0x90) {
            "The values '6X' and '9X' are invalid for instruction byte"
        }
        require(p1 in 0..255) { "P1 and P2 must not be greater than 255" }
        require(p2 in 0..255) { "P1 and P2 must not be greater than 255" }
        require(lc in 0..65535) { "Lc must not be greater than 65535" }
        require(data.length % 2 == 0) { "The format of data is incorrect" }
        require(le in 0..65536) { "Le must not be greater than 65536" }
    }

    fun cla(channel: Int = 0): Int {
        require(channel in 0..19) { "Channel number must be up to 19" }

        var value = ccc.value shl 4

        if (channel < 4) {
            /*
             * First inter-industry values of CLA
             *
             * Bits 8, 7 and 6 are set to 000.
             * Bit 5 controls command chaining.
             * Bits 4 and 3 indicate secure messaging.
             * Bits 2 and 1 encode a logical channel number.
             */
            value = value or (smi.value shl 2) or channel
        } else {
            /*
             * Further inter-industry values of CLA
             *
             * Bits 8 and 7 are set to 01.
             * Bit 6 indicates secure messaging.
             * Bit 5 controls command chaining.
             * Bits 4 to 1 encode a logical channel number (- 4).
             */
            value = value or FURTHER_INTER_INDUSTRY_CLASS or (channel - 4)
            if (smi != SecureMessaging.NO) value = value or 0x20
        }

        return value
    }

    fun build(channel: Int = 0): String {
        val builder = StringBuilder()

        // Add CLA + INS + P1 + P2 for header bytes
        builder.append(byte2HexChars(cla(channel)))
        builder.append(byte2HexChars(ins))
        builder.append(byte2HexChars(p1))
        builder.append(byte2HexChars(p2))

        // Extended format shall be applied to both Lc and Le
        val extended: Boolean = (lc > 255) || (le > 256)

        if (lc > 0) {
            if (extended) {
                // The first byte of the extended Lc field is 00.
                // The remaining 2 bytes have any value from 0001 to FFFF (never be 0000).
                builder.append(extendedBytes2HexChars(lc))
            } else {
                // The short Lc field consists of one byte from 01 to FF (never be 00).
                builder.append(byte2HexChars(lc))
            }
            builder.append(data)
        }

        if (le > 0) {
            if (extended) {
                // The extended Le field consists of three bytes.
                // The first byte is 00 and the remaining 2 bytes have any value from 0000 to FFFF.
                // The value 0000 means FFFF + 1 (65536).
                builder.append(extendedBytes2HexChars(le))
            } else {
                // A short Le field consists of one byte with any value.
                // The value 00 means FF + 1 (256).
                builder.append(byte2HexChars(le))
            }
        }

        return builder.toString()
    }

    private fun byte2HexChars(byte: Int) = "%02X".format(byte % 0x100)
    private fun extendedBytes2HexChars(byte: Int) = "%06X".format(byte % 0x10000)
}
