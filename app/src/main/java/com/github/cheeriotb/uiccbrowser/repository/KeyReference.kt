/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.repository

/** ETSI TS 102 221 key reference values used as VERIFY command P2. */
enum class KeyReference(val value: Int) {
    APPLICATION_PIN1(0x01),
    APPLICATION_PIN2(0x02),
    APPLICATION_PIN3(0x03),
    APPLICATION_PIN4(0x04),
    APPLICATION_PIN5(0x05),
    APPLICATION_PIN6(0x06),
    APPLICATION_PIN7(0x07),
    APPLICATION_PIN8(0x08),

    ADM1(0x0A),
    ADM2(0x0B),
    ADM3(0x0C),
    ADM4(0x0D),
    ADM5(0x0E),

    UNIVERSAL_PIN(0x11),

    LOCAL_PIN1(0x81),
    LOCAL_PIN2(0x82),
    LOCAL_PIN3(0x83),
    LOCAL_PIN4(0x84),
    LOCAL_PIN5(0x85),
    LOCAL_PIN6(0x86),
    LOCAL_PIN7(0x87),
    LOCAL_PIN8(0x88),

    ADM6(0x8A),
    ADM7(0x8B),
    ADM8(0x8C),
    ADM9(0x8D),
    ADM10(0x8E)
}
