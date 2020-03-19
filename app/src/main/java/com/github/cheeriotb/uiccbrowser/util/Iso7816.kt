/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.util

class Iso7816 {
    companion object {
        const val INS_VERIFY = 0x20
        const val INS_UNBLOCK_PIN = 0x2C
        const val INS_SELECT_FILE = 0xA4
        const val INS_READ_BINARY = 0xB0
        const val INS_READ_RECORD = 0xB2
        const val INS_UPDATE_BINARY = 0xD6
        const val INS_UPDATE_RECORD = 0xDC
        const val INS_GET_RESPONSE = 0xC0

        const val SW1_DATA_AVAILABLE = 0x61
        const val SW1_WRONG_LE = 0x6C
        const val SW1_INTERNAL_EXCEPTION = 0x6F

        const val MAX_LC = 0xFFFF
        const val MAX_LE = 0x10000
    }
}
