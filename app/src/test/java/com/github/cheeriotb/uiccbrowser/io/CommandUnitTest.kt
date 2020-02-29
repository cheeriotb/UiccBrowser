/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.io

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommandUnitTest {
    @Test
    fun default() {
        val command = Command(0xFF)

        assertThat(command.cla).isEqualTo(0x00)
        assertThat(command.ccc).isEqualTo(Command.Chaining.LAST_OR_ONLY)
        assertThat(command.smi).isEqualTo(Command.SecureMessaging.NO)
        assertThat(command.p1).isEqualTo(0x00)
        assertThat(command.p2).isEqualTo(0x00)
        assertThat(command.lc).isEqualTo(0)
        assertThat(command.data.length).isEqualTo(0)
        assertThat(command.le).isEqualTo(0)
        assertThat(command.build()).isEqualTo("00FF0000")
    }

    @Test
    fun cla_firstInterIndustry_channelNumber() {
        // Bits 8, 7 and 6 (b7..b5) are set to 000.
        // Bits 2 and 1 (b1..b0) encode a logical channel number.
        val command = Command(0xFF)

        assertThat(command.cla(0)).isEqualTo(0x00)
        assertThat(command.build(0).substring(0..1)).isEqualTo("00")

        assertThat(command.cla(3)).isEqualTo(0x03)
        assertThat(command.build(3).substring(0..1)).isEqualTo("03")
    }

    @Test
    fun cla_firstInterIndustry_chaining() {
        // Bit 5 (b4) controls command chaining.
        val command = Command(0xFF)

        command.ccc = Command.Chaining.LAST_OR_ONLY
        assertThat(command.cla(3)).isEqualTo(0x03)
        assertThat(command.build(3).substring(0..1)).isEqualTo("03")

        command.ccc = Command.Chaining.NOT_LAST
        assertThat(command.cla(3)).isEqualTo(0x13)
        assertThat(command.build(3).substring(0..1)).isEqualTo("13")
    }

    @Test
    fun cla_firstInterIndustry_secureMessaging() {
        // Bits 4 and 3 (b3..b2) indicate secure messaging.
        val command = Command(0xFF)

        command.smi = Command.SecureMessaging.NO
        assertThat(command.cla(3)).isEqualTo(0x03)
        assertThat(command.build(3).substring(0..1)).isEqualTo("03")

        command.smi = Command.SecureMessaging.PROPRIETARY
        assertThat(command.cla(3)).isEqualTo(0x07)
        assertThat(command.build(3).substring(0..1)).isEqualTo("07")

        command.smi = Command.SecureMessaging.HEADER_NOT_PROCESSED
        assertThat(command.cla(3)).isEqualTo(0x0B)
        assertThat(command.build(3).substring(0..1)).isEqualTo("0B")

        command.smi = Command.SecureMessaging.HEADER_AUTHENTICATED
        assertThat(command.cla(3)).isEqualTo(0x0F)
        assertThat(command.build(3).substring(0..1)).isEqualTo("0F")
    }

    @Test
    fun cla_furtherInterIndustry_channelNumber() {
        // Bits 8 and bit 7 (b7..b6) are set to 01.
        // Bits 4 to 1 (b3..b0) encode a logical channel number (- 4).
        val command = Command(0xFF)

        assertThat(command.cla(4)).isEqualTo(0x40)
        assertThat(command.build(4).substring(0..1)).isEqualTo("40")

        assertThat(command.cla(19)).isEqualTo(0x4F)
        assertThat(command.build(19).substring(0..1)).isEqualTo("4F")
    }

    @Test
    fun cla_furtherInterIndustry_chaining() {
        // Bit 5 (b4) controls command chaining.
        val command = Command(0xFF)

        command.ccc = Command.Chaining.LAST_OR_ONLY
        assertThat(command.cla(4)).isEqualTo(0x40)
        assertThat(command.build(4).substring(0..1)).isEqualTo("40")

        command.ccc = Command.Chaining.NOT_LAST
        assertThat(command.cla(4)).isEqualTo(0x50)
        assertThat(command.build(4).substring(0..1)).isEqualTo("50")
    }

    @Test
    fun cla_furtherInterIndustry_secureMessaging() {
        // Bit 6 (b5) indicate secure messaging.
        val command = Command(0xFF)

        command.smi = Command.SecureMessaging.NO
        assertThat(command.cla(4)).isEqualTo(0x40)
        assertThat(command.build(4).substring(0..1)).isEqualTo("40")

        command.smi = Command.SecureMessaging.PROPRIETARY
        assertThat(command.cla(4)).isEqualTo(0x60)
        assertThat(command.build(4).substring(0..1)).isEqualTo("60")

        command.smi = Command.SecureMessaging.HEADER_NOT_PROCESSED
        assertThat(command.cla(4)).isEqualTo(0x60)
        assertThat(command.build(4).substring(0..1)).isEqualTo("60")

        command.smi = Command.SecureMessaging.HEADER_AUTHENTICATED
        assertThat(command.cla(4)).isEqualTo(0x60)
        assertThat(command.build(4).substring(0..1)).isEqualTo("60")
    }

    @Test
    fun ins() {
        for (instruction in 0..255) {
            val higherOctet = (instruction and 0xF0)
            // The values '6X' and '9X' are invalid for instruction byte.
            val invalid = (higherOctet == 0x60) or (higherOctet == 0x90)

            try {
                val command = Command(instruction)
                if (invalid) {
                    Assert.fail("%02X shall never be allowed".format(instruction))
                    return
                }
                assertThat(command.ins).isEqualTo(instruction)
                assertThat(command.build().substring(2..3)).isEqualTo("%02X".format(instruction))
            } catch (iae: IllegalArgumentException) {
                if (!invalid) {
                    Assert.fail("%02X shall be allowed".format(instruction))
                    return
                }
            }
        }
    }

    @Test
    fun p1() {
        for (value in 0x00..0xFF) {
            val command = Command(0xFF, value, 0x02)
            assertThat(command.p1).isEqualTo(value)
            assertThat(command.build().substring(4..5)).isEqualTo("%02X".format(value))
        }
    }

    @Test
    fun p2() {
        for (value in 0x00..0xFF) {
            val command = Command(0xFF, 0x01, value)
            assertThat(command.p2).isEqualTo(value)
            assertThat(command.build().substring(6..7)).isEqualTo("%02X".format(value))
        }
    }

    @Test
    fun case2s() {
        // CLA + INS + P1 + P2 + Short Le (1 byte).
        // A short Le field consists of one byte with any value.
        // The value of 00 means FF + 1 (256).
        val command1 = Command(0xFF, le = 0x01)
        assertThat(command1.lc).isEqualTo(0x00)
        assertThat(command1.le).isEqualTo(0x01)
        assertThat(command1.build().length).isEqualTo(10 /* 5 byte APDU */)
        assertThat(command1.build().substring(8..9)).isEqualTo("01")

        val command2 = Command(0xFF, 0x01, 0x02, 0xFF)
        assertThat(command2.lc).isEqualTo(0x00)
        assertThat(command2.le).isEqualTo(0xFF)
        assertThat(command2.build().length).isEqualTo(10 /* 5 byte APDU */)
        assertThat(command2.build().substring(8..9)).isEqualTo("FF")

        val command3 = Command(0xFF, 0x01, 0x02, 0x100)
        assertThat(command3.lc).isEqualTo(0x00)
        assertThat(command3.le).isEqualTo(0x100)
        assertThat(command3.build().length).isEqualTo(10 /* 5 byte APDU */)
        assertThat(command3.build().substring(8..9)).isEqualTo("00")
    }

    @Test
    fun case2e() {
        // CLA + INS + P1 + P2 + Extended Le (3 bytes).
        // The extended Le field consists of three bytes.
        // The first byte is 00 and the remaining bytes have any value from 0000 to FFFF.
        // The value of 0000 means FFFF + 1 (65536)
        val command1 = Command(0xFF, le = 0x101)
        assertThat(command1.lc).isEqualTo(0x00)
        assertThat(command1.le).isEqualTo(0x101)
        assertThat(command1.build().length).isEqualTo(14 /* 7 bytes APDU */)
        assertThat(command1.build().substring(8..13)).isEqualTo("000101")

        val command2 = Command(0xFF, 0x01, 0x02, 0xFFFF)
        assertThat(command2.lc).isEqualTo(0x00)
        assertThat(command2.le).isEqualTo(0xFFFF)
        assertThat(command2.build().length).isEqualTo(14 /* 7 bytes APDU */)
        assertThat(command2.build().substring(8..13)).isEqualTo("00FFFF")

        val command3 = Command(0xFF, 0x01, 0x02, 0x10000)
        assertThat(command3.lc).isEqualTo(0x00)
        assertThat(command3.le).isEqualTo(0x10000)
        assertThat(command3.build().length).isEqualTo(14 /* 7 bytes APDU */)
        assertThat(command3.build().substring(8..13)).isEqualTo("000000")
    }

    @Test
    fun case3s() {
        case3sInternal(lc = 0x01)
        case3sInternal(lc = 0xFF)
    }

    private fun case3sInternal(lc: Int) {
        val data = "AB".repeat(lc)
        val command = Command(0xFF, 0x01, 0x02, data)
        val apdu = command.build()

        // CLA + INS + P1 + P2 + Short Lc (1 byte) + Data.
        // The short Lc field consists of one byte from 01 to FF (never be 00).
        assertThat(command.lc).isEqualTo(lc)
        assertThat(command.le).isEqualTo(0x00)
        assertThat(apdu.length).isEqualTo((5 + lc) * 2)
        assertThat(apdu.substring(8..9)).isEqualTo("%02X".format(lc))
        assertThat(apdu.substring(10, 10 + lc * 2)).isEqualTo(data)
    }

    @Test
    fun case3e() {
        case3eInternal(lc = 0x100)
        case3eInternal(lc = 0xFFFF)
    }

    private fun case3eInternal(lc: Int) {
        val data = "AB".repeat(lc)
        val command = Command(0xFF, 0x01, 0x02, data)
        val apdu = command.build()

        // CLA + INS + P1 + P2 + Extended Lc (3 byte) + Data.
        // The first byte of the extended Lc field is 00.
        // The remaining 2 bytes have any value from 0001 to FFFF (never be 0000).
        assertThat(command.lc).isEqualTo(lc)
        assertThat(command.le).isEqualTo(0x00)
        assertThat(apdu.length).isEqualTo((7 + lc) * 2)
        assertThat(apdu.substring(8..13)).isEqualTo("%06X".format(lc))
        assertThat(apdu.substring(14, 14 + lc * 2)).isEqualTo(data)
    }

    @Test
    fun case4s() {
        case4sInternal(lc = 0x01, le = 0xFF)
        case4sInternal(lc = 0xFF, le = 0x01)
        case4sInternal(lc = 0x01, le = 0x100)
    }

    private fun case4sInternal(lc: Int, le: Int) {
        val data = "AB".repeat(lc)
        val command = Command(0xFF, 0x01, 0x02, data, le)
        val apdu = command.build()

        // CLA + INS + P1 + P2 + Short Lc (1 byte) + Data + Short Le (1 byte).
        // The short Lc field must be 01 to FF.
        // The short Le field must be 01 to FF and 00 (256).
        assertThat(command.lc).isEqualTo(lc)
        assertThat(command.le).isEqualTo(le)
        assertThat(apdu.length).isEqualTo((6 + lc) * 2)
        assertThat(apdu.substring(8..9)).isEqualTo("%02X".format(lc))
        assertThat(apdu.substring(10, 10 + lc * 2)).isEqualTo(data)
        assertThat(apdu.substring(10 + lc * 2, 10 + lc * 2 + 2))
                .isEqualTo(if (le < 0x100) "%02X".format(le) else "00")
    }

    @Test
    fun case4e() {
        // Big Lc x Small Le
        case4eInternal(lc = 0x100, le = 0x01)
        // Small Lc x Big Le
        case4eInternal(lc = 0x01, le = 0x101)
        // Big Lc x Big Le
        case4eInternal(lc = 0xFFFF, le = 0x10000)
    }

    private fun case4eInternal(lc: Int, le: Int) {
        val data = "AB".repeat(lc)
        val command = Command(0xFF, 0x01, 0x02, data, le)
        val apdu = command.build()

        // CLA + INS + P1 + P2 + Extended Lc (3 byte) + Data + Extended Le (3 byte).
        // The extended Lc field must be 000001 to 00FFFF.
        // The extended Le field must be 000001 to 00FFFF and 000000 (65536).
        assertThat(command.lc).isEqualTo(lc)
        assertThat(command.le).isEqualTo(le)
        assertThat(apdu.length).isEqualTo((10 + lc) * 2)
        assertThat(apdu.substring(8..13)).isEqualTo("%06X".format(lc))
        assertThat(apdu.substring(14, 14 + lc * 2)).isEqualTo(data)
        assertThat(apdu.substring(14 + lc * 2, 14 + (lc + 3) * 2))
            .isEqualTo(if (le < 0x10000) "%06X".format(le) else "000000")
    }
}
