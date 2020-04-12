/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.cardio

class Command(
    val ins: Int,
    val p1: Int = 0x00,
    val p2: Int = 0x00,
    val data: ByteArray,
    val le: Int = 0
) {
    constructor(
        ins: Int,
        p1: Int = 0x00,
        p2: Int = 0x00,
        le: Int = 0
    ) : this(ins, p1, p2, ByteArray(0), le)

    constructor(
        command: Command
    ) : this(command.ins, command.p1, command.p2, command.data, command.le)

    companion object {
        private const val FURTHER_INTER_INDUSTRY_CLASS = 0x40
    }

    /** Represents Command chaining control */
    enum class Chaining(val value: Int) {
        /** The command is the last or only command of a chain */
        LAST_OR_ONLY(0),
        /** The command is not the last command of a chain */
        NOT_LAST(1)
    }

    var ccc: Chaining = Chaining.LAST_OR_ONLY

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

    var smi: SecureMessaging = SecureMessaging.NO

    val cla: Int
        get() = cla()

    val lc: Int
        get() = data.size

    val extended: Boolean by lazy {
        // Extended format shall be applied to both Lc and Le
        (lc > 255) || (le > 256)
    }

    init {
        require(ins in 0..255) { "INS must not be greater than 255" }
        require((ins and 0xF0) != 0x60 && (ins and 0xF0) != 0x90) {
            "The values '6X' and '9X' are invalid for instruction byte"
        }
        require(p1 in 0..255) { "P1 and P2 must not be greater than 255" }
        require(p2 in 0..255) { "P1 and P2 must not be greater than 255" }
        require(lc in IntRange(0, Iso7816.MAX_LC)) { "Lc must not be greater than 65535" }
        require(le in IntRange(0, Iso7816.MAX_LE)) { "Le must not be greater than 65536" }
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

    fun build(channel: Int = 0): ByteArray {
        // CLA + INS + P1 + P2 for header bytes
        var array = byteArrayOf(cla(channel).toByte(), ins.toByte(), p1.toByte(), p2.toByte())

        if (lc > 0) {
            array = if (extended) {
                // The first byte of the extended Lc field is 00.
                // The remaining 2 bytes have any value from 0001 - FFFF (never be 0000).
                array.plus(byteArrayOf(0x00, lc.shr(8).toByte(), lc.toByte()))
            } else {
                // The short Lc field consists of one byte from 01 - FF (never be 00).
                array.plus(lc.toByte())
            }
            array = array.plus(data)
        }

        if (le > 0) {
            array = if (extended) {
                if (lc > 0) {
                    // The extended Le field of Case 4E format consists of two bytes.
                    // The range of the value is 0000 - FFFF.
                    // The value 0000 means FFFF + 1 (65536).
                    array.plus(byteArrayOf(le.shr(8).toByte(), le.toByte()))
                } else {
                    // The extended Le field of Case 2E format consists of three bytes.
                    // The first byte is 00 and the remaining bytes have any value from 0000 - FFFF.
                    // The value 0000 means FFFF + 1 (65536).
                    array.plus(byteArrayOf(0x00, le.shr(8).toByte(), le.toByte()))
                }
            } else {
                // A short Le field consists of one byte with any value.
                // The value 00 means FF + 1 (256).
                array.plus(le.toByte())
            }
        }

        return array
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Command

        if (cla != other.cla) return false
        if (ins != other.ins) return false
        if (p1 != other.p1) return false
        if (p2 != other.p2) return false
        if (!data.contentEquals(other.data)) return false
        if (le != other.le) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cla.hashCode()
        result = 31 * result + ins.hashCode()
        result = 31 * result + p1.hashCode()
        result = 31 * result + p2.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + le
        return result
    }
}
