/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.io

import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString
import com.github.cheeriotb.uiccbrowser.util.byteToHexString
import com.github.cheeriotb.uiccbrowser.util.extendedBytesToHexString
import com.github.cheeriotb.uiccbrowser.util.hexStringToByteArray

class Command(
    val ins: Int,
    val p1: Int = 0x00,
    val p2: Int = 0x00,
    val le: Int = NO_EXPECTED_DATA
) {
    constructor(
        ins: Int,
        p1: Int = 0x00,
        p2: Int = 0x00,
        data: String,
        le: Int = NO_EXPECTED_DATA
    ) : this(ins, p1, p2, le) {
        require(data.length % 2 == 0) { "The format of data is incorrect" }
        require(dataStringPrivate.length / 2 in 0..65535) { "Lc must not be greater than 65535" }
        dataStringPrivate = data
    }

    constructor(
        ins: Int,
        p1: Int = 0x00,
        p2: Int = 0x00,
        data: ByteArray,
        le: Int = NO_EXPECTED_DATA
    ) : this(ins, p1, p2, le) {
        require(dataArrayPrivate.size in 0..65535) { "Lc must not be greater than 65535" }
        dataArrayPrivate = data
    }

    companion object {
        private const val NO_COMMAND_DATA_STRING = ""
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

    val lc: Int by lazy {
        if (dataStringPrivate.isNotEmpty()) dataStringPrivate.length / 2 else dataArrayPrivate.size
    }

    private var dataStringPrivate = NO_COMMAND_DATA_STRING
    private var dataArrayPrivate = ByteArray(0)

    val dataString: String by lazy {
        if (dataStringPrivate.isNotEmpty()) dataStringPrivate
                else byteArrayToHexString(dataArrayPrivate)
    }

    val dataArray: ByteArray by lazy {
        if (dataArrayPrivate.isNotEmpty()) dataArrayPrivate
                else hexStringToByteArray(dataStringPrivate)
    }

    init {
        require(ins in 0..255) { "INS must not be greater than 255" }
        require((ins and 0xF0) != 0x60 && (ins and 0xF0) != 0x90) {
            "The values '6X' and '9X' are invalid for instruction byte"
        }
        require(p1 in 0..255) { "P1 and P2 must not be greater than 255" }
        require(p2 in 0..255) { "P1 and P2 must not be greater than 255" }
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

    fun buildString(channel: Int = 0): String {
        val builder = StringBuilder()

        // Add CLA + INS + P1 + P2 for header bytes
        builder.append(byteToHexString(cla(channel)))
        builder.append(byteToHexString(ins))
        builder.append(byteToHexString(p1))
        builder.append(byteToHexString(p2))

        // Extended format shall be applied to both Lc and Le
        val extended: Boolean = (lc > 255) || (le > 256)

        if (lc > 0) {
            if (extended) {
                // The first byte of the extended Lc field is 00.
                // The remaining 2 bytes have any value from 0001 to FFFF (never be 0000).
                builder.append(extendedBytesToHexString(lc))
            } else {
                // The short Lc field consists of one byte from 01 to FF (never be 00).
                builder.append(byteToHexString(lc))
            }
            builder.append(dataString)
        }

        if (le > 0) {
            if (extended) {
                // The extended Le field consists of three bytes.
                // The first byte is 00 and the remaining 2 bytes have any value from 0000 to FFFF.
                // The value 0000 means FFFF + 1 (65536).
                builder.append(extendedBytesToHexString(le))
            } else {
                // A short Le field consists of one byte with any value.
                // The value 00 means FF + 1 (256).
                builder.append(byteToHexString(le))
            }
        }

        return builder.toString()
    }

    fun buildArray(channel: Int = 0): ByteArray {
        // Extended format shall be applied to both Lc and Le
        val extended: Boolean = (lc > 255) || (le > 256)

        // CLA + INS + P1 + P2 for header bytes
        var array = byteArrayOf(cla(channel).toByte(), ins.toByte(), p1.toByte(), p2.toByte())

        if (lc > 0) {
            array = if (extended) {
                // The first byte of the extended Lc field is 00.
                // The remaining 2 bytes have any value from 0001 to FFFF (never be 0000).
                array.plus(byteArrayOf(0x00, lc.shr(8).toByte(), lc.toByte()))
            } else {
                // The short Lc field consists of one byte from 01 to FF (never be 00).
                array.plus(lc.toByte())
            }
            array = array.plus(dataArray)
        }

        if (le > 0) {
            array = if (extended) {
                // The extended Le field consists of three bytes.
                // The first byte is 00 and the remaining 2 bytes have any value from 0000 to FFFF.
                // The value 0000 means FFFF + 1 (65536).
                array.plus(byteArrayOf(0x00, le.shr(8).toByte(), le.toByte()))
            } else {
                // A short Le field consists of one byte with any value.
                // The value 00 means FF + 1 (256).
                array.plus(le.toByte())
            }
        }

        return array
    }
}
