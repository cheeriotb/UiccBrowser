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
        assertThat(command.dataString.length).isEqualTo(0)
        assertThat(command.dataArray.size).isEqualTo(0)
        assertThat(command.le).isEqualTo(0)

        assertThat(command.buildString()).isEqualTo("00FF0000")
        assertThat(command.buildArray()).isEqualTo(byteArrayOf(0x00, 0xFF.toByte(), 0x00, 0x00))
    }

    @Test
    fun cla_firstInterIndustry_channelNumber() {
        // Bits 8, 7 and 6 (b7..b5) are set to 000.
        // Bits 2 and 1 (b1..b0) encode a logical channel number.
        val command = Command(0xFF)

        assertThat(command.cla(0)).isEqualTo(0x00)
        assertThat(command.buildString(0).substring(0..1)).isEqualTo("00")
        assertThat(command.buildArray(0)[0]).isEqualTo(0x00)

        assertThat(command.cla(3)).isEqualTo(0x03)
        assertThat(command.buildString(3).substring(0..1)).isEqualTo("03")
        assertThat(command.buildArray(3)[0]).isEqualTo(0x03)
    }

    @Test
    fun cla_firstInterIndustry_chaining() {
        // Bit 5 (b4) controls command chaining.
        val command = Command(0xFF)

        command.ccc = Command.Chaining.LAST_OR_ONLY
        assertThat(command.cla(3)).isEqualTo(0x03)
        assertThat(command.buildString(3).substring(0..1)).isEqualTo("03")
        assertThat(command.buildArray(3)[0]).isEqualTo(0x03)

        command.ccc = Command.Chaining.NOT_LAST
        assertThat(command.cla(3)).isEqualTo(0x13)
        assertThat(command.buildString(3).substring(0..1)).isEqualTo("13")
        assertThat(command.buildArray(3)[0]).isEqualTo(0x13)
    }

    @Test
    fun cla_firstInterIndustry_secureMessaging() {
        // Bits 4 and 3 (b3..b2) indicate secure messaging.
        val command = Command(0xFF)

        command.smi = Command.SecureMessaging.NO
        assertThat(command.cla(3)).isEqualTo(0x03)
        assertThat(command.buildString(3).substring(0..1)).isEqualTo("03")
        assertThat(command.buildArray(3)[0]).isEqualTo(0x03)

        command.smi = Command.SecureMessaging.PROPRIETARY
        assertThat(command.cla(3)).isEqualTo(0x07)
        assertThat(command.buildString(3).substring(0..1)).isEqualTo("07")
        assertThat(command.buildArray(3)[0]).isEqualTo(0x07)

        command.smi = Command.SecureMessaging.HEADER_NOT_PROCESSED
        assertThat(command.cla(3)).isEqualTo(0x0B)
        assertThat(command.buildString(3).substring(0..1)).isEqualTo("0B")
        assertThat(command.buildArray(3)[0]).isEqualTo(0x0B)

        command.smi = Command.SecureMessaging.HEADER_AUTHENTICATED
        assertThat(command.cla(3)).isEqualTo(0x0F)
        assertThat(command.buildString(3).substring(0..1)).isEqualTo("0F")
        assertThat(command.buildArray(3)[0]).isEqualTo(0x0F)
    }

    @Test
    fun cla_furtherInterIndustry_channelNumber() {
        // Bits 8 and bit 7 (b7..b6) are set to 01.
        // Bits 4 to 1 (b3..b0) encode a logical channel number (- 4).
        val command = Command(0xFF)

        assertThat(command.cla(4)).isEqualTo(0x40)
        assertThat(command.buildString(4).substring(0..1)).isEqualTo("40")
        assertThat(command.buildArray(4)[0]).isEqualTo(0x40)

        assertThat(command.cla(19)).isEqualTo(0x4F)
        assertThat(command.buildString(19).substring(0..1)).isEqualTo("4F")
        assertThat(command.buildArray(19)[0]).isEqualTo(0x4F)
    }

    @Test
    fun cla_furtherInterIndustry_chaining() {
        // Bit 5 (b4) controls command chaining.
        val command = Command(0xFF)

        command.ccc = Command.Chaining.LAST_OR_ONLY
        assertThat(command.cla(4)).isEqualTo(0x40)
        assertThat(command.buildString(4).substring(0..1)).isEqualTo("40")
        assertThat(command.buildArray(4)[0]).isEqualTo(0x40)

        command.ccc = Command.Chaining.NOT_LAST
        assertThat(command.cla(4)).isEqualTo(0x50)
        assertThat(command.buildString(4).substring(0..1)).isEqualTo("50")
        assertThat(command.buildArray(4)[0]).isEqualTo(0x50)
    }

    @Test
    fun cla_furtherInterIndustry_secureMessaging() {
        // Bit 6 (b5) indicate secure messaging.
        val command = Command(0xFF)

        command.smi = Command.SecureMessaging.NO
        assertThat(command.cla(4)).isEqualTo(0x40)
        assertThat(command.buildString(4).substring(0..1)).isEqualTo("40")
        assertThat(command.buildArray(4)[0]).isEqualTo(0x40)

        command.smi = Command.SecureMessaging.PROPRIETARY
        assertThat(command.cla(4)).isEqualTo(0x60)
        assertThat(command.buildString(4).substring(0..1)).isEqualTo("60")
        assertThat(command.buildArray(4)[0]).isEqualTo(0x60)

        command.smi = Command.SecureMessaging.HEADER_NOT_PROCESSED
        assertThat(command.cla(4)).isEqualTo(0x60)
        assertThat(command.buildString(4).substring(0..1)).isEqualTo("60")
        assertThat(command.buildArray(4)[0]).isEqualTo(0x60)

        command.smi = Command.SecureMessaging.HEADER_AUTHENTICATED
        assertThat(command.cla(4)).isEqualTo(0x60)
        assertThat(command.buildString(4).substring(0..1)).isEqualTo("60")
        assertThat(command.buildArray(4)[0]).isEqualTo(0x60)
    }

    @Test
    fun ins() {
        for (ins in 0..255) {
            val higherOctet = (ins and 0xF0)
            // The values '6X' and '9X' are invalid for instruction byte.
            val invalid = (higherOctet == 0x60) or (higherOctet == 0x90)

            try {
                val command = Command(ins)
                if (invalid) {
                    Assert.fail("%02X shall never be allowed".format(ins))
                    return
                }
                assertThat(command.ins).isEqualTo(ins)
                assertThat(command.buildString().substring(1 * 2, (1 + 1) * 2))
                        .isEqualTo("%02X".format(ins))
                assertThat(command.buildArray()[1]).isEqualTo(ins.toByte())
            } catch (iae: IllegalArgumentException) {
                if (!invalid) {
                    Assert.fail("%02X shall be allowed".format(ins))
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
            assertThat(command.buildString().substring(2 * 2, (2 + 1) * 2))
                    .isEqualTo("%02X".format(value))
            assertThat(command.buildArray()[2]).isEqualTo(value.toByte())
        }
    }

    @Test
    fun p2() {
        for (value in 0x00..0xFF) {
            val command = Command(0xFF, 0x01, value)
            assertThat(command.p2).isEqualTo(value)
            assertThat(command.buildString().substring(3 * 2, (3 + 1) * 2))
                    .isEqualTo("%02X".format(value))
            assertThat(command.buildArray()[3]).isEqualTo(value.toByte())
        }
    }

    @Test
    fun case2s() {
        // CLA + INS + P1 + P2 + Short Le (1 byte).
        // A short Le field consists of one byte with any value.
        // The value of 00 means FF + 1 (256).
        case2sInternal(0x01, "01")
        case2sInternal(0xFF, "FF")
        case2sInternal(0x100, "00")
    }

    private fun case2sInternal(leValue: Int, leField: String) {
        val command = Command(0xFF, le = leValue)
        assertThat(command.lc).isEqualTo(0x00)
        assertThat(command.le).isEqualTo(leValue)

        val apduString = command.buildString()
        assertThat(apduString.length).isEqualTo((4 + 1) * 2)
        assertThat(apduString.substring(4 * 2)).isEqualTo(leField)

        val apduArray = command.buildArray()
        assertThat(apduArray.size).isEqualTo(4 + 1)
        assertThat("%02X".format(apduArray[4])).isEqualTo(leField)
    }

    @Test
    fun case2e() {
        // CLA + INS + P1 + P2 + Extended Le (3 bytes).
        // The extended Le field consists of three bytes.
        // The first byte is 00 and the remaining bytes have any value from 0000 to FFFF.
        // The value of 0000 means FFFF + 1 (65536)
        case2eInternal(0x101, "000101")
        case2eInternal(0xFFFF, "00FFFF")
        case2eInternal(0x10000, "000000")
    }

    private fun case2eInternal(leValue: Int, leField: String) {
        val command = Command(0xFF, le = leValue)
        assertThat(command.lc).isEqualTo(0x00)
        assertThat(command.le).isEqualTo(leValue)

        val apduString = command.buildString()
        assertThat(apduString.length).isEqualTo((4 + 3) * 2)
        assertThat(apduString.substring(4 * 2)).isEqualTo(leField)

        val apduArray = command.buildArray()
        assertThat(apduArray.size).isEqualTo(4 + 3)
        assertThat("%02X%02X%02X".format(apduArray[4], apduArray[5], apduArray[6]))
                .isEqualTo(leField)
    }

    @Test
    fun case3s() {
        case3sInternal(0x01, "01")
        case3sInternal(0xFF, "FF")
    }

    private fun case3sInternal(lcValue: Int, lcField: String) {
        case3sString(lcValue, lcField)
        case3sArray(lcValue, lcField)
    }

    private fun case3sString(lcValue: Int, lcField: String) {
        val dataString = "AB".repeat(lcValue)
        val dataArray = ByteArray(lcValue)
        dataArray.fill(0xAB.toByte())
        val command = Command(0xFF, data = dataString)

        // CLA + INS + P1 + P2 + Short Lc (1 byte) + Data.
        // The short Lc field consists of one byte from 01 to FF (never be 00).
        assertThat(command.lc).isEqualTo(lcValue)
        assertThat(command.le).isEqualTo(0x00)
        assertThat(command.dataString).isEqualTo(dataString)
        assertThat(command.dataArray).isEqualTo(dataArray)

        val apduString = command.buildString()
        assertThat(apduString.length).isEqualTo((4 + 1 + lcValue) * 2)
        assertThat(apduString.substring(4 * 2, (4 + 1) * 2)).isEqualTo(lcField)
        assertThat(apduString.substring((4 + 1) * 2)).isEqualTo(dataString)

        val apduArray = command.buildArray()
        assertThat(apduArray.size).isEqualTo(4 + 1 + lcValue)
        assertThat("%02X".format(apduArray[4])).isEqualTo(lcField)
        assertThat(apduArray.sliceArray(IntRange(4 + 1, 4 + 1 + lcValue - 1))).isEqualTo(dataArray)
    }

    private fun case3sArray(lcValue: Int, lcField: String) {
        val dataString = "AB".repeat(lcValue)
        val dataArray = ByteArray(lcValue)
        dataArray.fill(0xAB.toByte())
        val command = Command(0xFF, data = dataArray)

        // CLA + INS + P1 + P2 + Short Lc (1 byte) + Data.
        // The short Lc field consists of one byte from 01 to FF (never be 00).
        assertThat(command.lc).isEqualTo(lcValue)
        assertThat(command.le).isEqualTo(0x00)
        assertThat(command.dataString).isEqualTo(dataString)
        assertThat(command.dataArray).isEqualTo(dataArray)

        val apduString = command.buildString()
        assertThat(apduString.length).isEqualTo((4 + 1 + lcValue) * 2)
        assertThat(apduString.substring(4 * 2, (4 + 1) * 2)).isEqualTo(lcField)
        assertThat(apduString.substring((4 + 1) * 2)).isEqualTo(dataString)

        val apduArray = command.buildArray()
        assertThat(apduArray.size).isEqualTo(4 + 1 + lcValue)
        assertThat("%02X".format(apduArray[4])).isEqualTo(lcField)
        assertThat(apduArray.sliceArray(IntRange(4 + 1, 4 + 1 + lcValue - 1))).isEqualTo(dataArray)
    }

    @Test
    fun case3e() {
        case3eInternal(0x100, "000100")
        case3eInternal(0xFFFF, "00FFFF")
    }

    private fun case3eInternal(lcValue: Int, lcField: String) {
        case3eString(lcValue, lcField)
        case3eArray(lcValue, lcField)
    }

    private fun case3eString(lcValue: Int, lcField: String) {
        val dataString = "AB".repeat(lcValue)
        val dataArray = ByteArray(lcValue)
        dataArray.fill(0xAB.toByte())
        val command = Command(0xFF, data = dataString)

        // CLA + INS + P1 + P2 + Extended Lc (3 byte) + Data.
        // The first byte of the extended Lc field is 00.
        // The remaining 2 bytes have any value from 0001 to FFFF (never be 0000).
        assertThat(command.lc).isEqualTo(lcValue)
        assertThat(command.le).isEqualTo(0x00)
        assertThat(command.dataString).isEqualTo(dataString)
        assertThat(command.dataArray).isEqualTo(dataArray)

        val apduString = command.buildString()
        assertThat(apduString.length).isEqualTo((4 + 3 + lcValue) * 2)
        assertThat(apduString.substring(4 * 2, (4 + 3) * 2)).isEqualTo(lcField)
        assertThat(apduString.substring((4 + 3) * 2)).isEqualTo(dataString)

        val apduArray = command.buildArray()
        assertThat(apduArray.size).isEqualTo(4 + 3 + lcValue)
        assertThat("%02X%02X%02X".format(apduArray[4], apduArray[5], apduArray[6]))
                .isEqualTo(lcField)
        assertThat(apduArray.sliceArray(IntRange(4 + 3, 4 + 3 + lcValue - 1))).isEqualTo(dataArray)
    }

    private fun case3eArray(lcValue: Int, lcField: String) {
        val dataString = "AB".repeat(lcValue)
        val dataArray = ByteArray(lcValue)
        dataArray.fill(0xAB.toByte())
        val command = Command(0xFF, data = dataArray)

        // CLA + INS + P1 + P2 + Extended Lc (3 byte) + Data.
        // The first byte of the extended Lc field is 00.
        // The remaining 2 bytes have any value from 0001 to FFFF (never be 0000).
        assertThat(command.lc).isEqualTo(lcValue)
        assertThat(command.le).isEqualTo(0x00)
        assertThat(command.dataString).isEqualTo(dataString)
        assertThat(command.dataArray).isEqualTo(dataArray)

        val apduString = command.buildString()
        assertThat(apduString.length).isEqualTo((4 + 3 + lcValue) * 2)
        assertThat(apduString.substring(4 * 2, (4 + 3) * 2)).isEqualTo(lcField)
        assertThat(apduString.substring((4 + 3) * 2)).isEqualTo(dataString)

        val apduArray = command.buildArray()
        assertThat(apduArray.size).isEqualTo(4 + 3 + lcValue)
        assertThat("%02X%02X%02X".format(apduArray[4], apduArray[5], apduArray[6]))
                .isEqualTo(lcField)
        assertThat(apduArray.sliceArray(IntRange(4 + 3, 4 + 3 + lcValue - 1))).isEqualTo(dataArray)
    }

    @Test
    fun case4s() {
        case4sString(0x01, "01", 0xFF, "FF")
        case4sString(0xFF, "FF", 0x01, "01")
        case4sString(0x01, "01", 0x100, "00")
    }

    private fun case4sString(lcValue: Int, lcField: String, leValue: Int, leField: String) {
        val dataString = "AB".repeat(lcValue)
        val dataArray = ByteArray(lcValue)
        dataArray.fill(0xAB.toByte())
        val command = Command(0xFF, 0x01, 0x02, dataString, leValue)

        // CLA + INS + P1 + P2 + Short Lc (1 byte) + Data + Short Le (1 byte).
        // The short Lc field must be 01 to FF.
        // The short Le field must be 01 to FF and 00 (256).
        assertThat(command.lc).isEqualTo(lcValue)
        assertThat(command.le).isEqualTo(leValue)
        assertThat(command.dataString).isEqualTo(dataString)
        assertThat(command.dataArray).isEqualTo(dataArray)

        val apduString = command.buildString()
        assertThat(apduString.length).isEqualTo((4 + 1 + lcValue + 1) * 2)
        assertThat(apduString.substring(4 * 2, (4 + 1) * 2)).isEqualTo(lcField)
        assertThat(apduString.substring((4 + 1) * 2, (4 + 1 + lcValue) * 2)).isEqualTo(dataString)
        assertThat(apduString.substring((4 + 1 + lcValue) * 2)).isEqualTo(leField)

        val apduArray = command.buildArray()
        assertThat(apduArray.size).isEqualTo(4 + 1 + lcValue + 1)
        assertThat("%02X".format(apduArray[4])).isEqualTo(lcField)
        assertThat(apduArray.sliceArray(IntRange(4 + 1, 4 + 1 + lcValue - 1))).isEqualTo(dataArray)
        assertThat("%02X".format(apduArray[4 + 1 + lcValue])).isEqualTo(leField)
    }

    @Test
    fun case4e() {
        // Big Lc x Small Le
        case4eInternal(0x100, "000100", 0x01, "000001")
        // Small Lc x Big Le
        case4eInternal(0x01, "000001", 0x101, "000101")
        // Big Lc x Big Le
        case4eInternal(0xFFFF, "00FFFF", 0x10000, "000000")
    }

    private fun case4eInternal(lcValue: Int, lcField: String, leValue: Int, leField: String) {
        case4eString(lcValue, lcField, leValue, leField)
        case4eArray(lcValue, lcField, leValue, leField)
    }

    private fun case4eString(lcValue: Int, lcField: String, leValue: Int, leField: String) {
        val dataString = "AB".repeat(lcValue)
        val dataArray = ByteArray(lcValue)
        dataArray.fill(0xAB.toByte())
        val command = Command(0xFF, 0x01, 0x02, dataString, leValue)

        // CLA + INS + P1 + P2 + Extended Lc (3 byte) + Data + Extended Le (3 byte).
        // The extended Lc field must be 000001 to 00FFFF.
        // The extended Le field must be 000001 to 00FFFF and 000000 (65536).
        assertThat(command.lc).isEqualTo(lcValue)
        assertThat(command.le).isEqualTo(leValue)
        assertThat(command.dataString).isEqualTo(dataString)

        val apduString = command.buildString()
        assertThat(apduString.length).isEqualTo((4 + 3 + lcValue + 3) * 2)
        assertThat(apduString.substring(4 * 2, (4 + 3) * 2)).isEqualTo(lcField)
        assertThat(apduString.substring((4 + 3) * 2, (4 + 3 + lcValue) * 2)).isEqualTo(dataString)
        assertThat(apduString.substring((4 + 3 + lcValue) * 2)).isEqualTo(leField)

        val apduArray = command.buildArray()
        assertThat(apduArray.size).isEqualTo(4 + 3 + lcValue + 3)
        assertThat("%02X%02X%02X".format(apduArray[4], apduArray[5], apduArray[6]))
                .isEqualTo(lcField)
        assertThat(apduArray.sliceArray(IntRange(4 + 3, 4 + 3 + lcValue - 1))).isEqualTo(dataArray)
        assertThat("%02X%02X%02X".format(apduArray[4 + 3 + lcValue], apduArray[4 + 3 + lcValue + 1],
                apduArray[4 + 3 + lcValue + 2])).isEqualTo(leField)
    }

    private fun case4eArray(lcValue: Int, lcField: String, leValue: Int, leField: String) {
        val dataString = "AB".repeat(lcValue)
        val dataArray = ByteArray(lcValue)
        dataArray.fill(0xAB.toByte())
        val command = Command(0xFF, 0x01, 0x02, dataArray, leValue)

        // CLA + INS + P1 + P2 + Extended Lc (3 byte) + Data + Extended Le (3 byte).
        // The extended Lc field must be 000001 to 00FFFF.
        // The extended Le field must be 000001 to 00FFFF and 000000 (65536).
        assertThat(command.lc).isEqualTo(lcValue)
        assertThat(command.le).isEqualTo(leValue)
        assertThat(command.dataString).isEqualTo(dataString)

        val apduString = command.buildString()
        assertThat(apduString.length).isEqualTo((4 + 3 + lcValue + 3) * 2)
        assertThat(apduString.substring(4 * 2, (4 + 3) * 2)).isEqualTo(lcField)
        assertThat(apduString.substring((4 + 3) * 2, (4 + 3 + lcValue) * 2)).isEqualTo(dataString)
        assertThat(apduString.substring((4 + 3 + lcValue) * 2)).isEqualTo(leField)

        val apduArray = command.buildArray()
        assertThat(apduArray.size).isEqualTo(4 + 3 + lcValue + 3)
        assertThat("%02X%02X%02X".format(apduArray[4], apduArray[5], apduArray[6]))
                .isEqualTo(lcField)
        assertThat(apduArray.sliceArray(IntRange(4 + 3, 4 + 3 + lcValue - 1))).isEqualTo(dataArray)
        assertThat("%02X%02X%02X".format(apduArray[4 + 3 + lcValue], apduArray[4 + 3 + lcValue + 1],
                apduArray[4 + 3 + lcValue + 2])).isEqualTo(leField)
    }
}
