/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.cardio

class Response(private val response: ByteArray) {
    companion object {
        const val SW_SIZE = 2
    }

    init {
        require(response.size >= SW_SIZE) {
            "The size of the response data must be at least 2 for the status word"
        }
        require((response.size - SW_SIZE) <= Iso7816.MAX_LE) {
            "The size of the response data must not be greater than 65536 (+ 2)"
        }
    }

    val data: ByteArray by lazy {
        response.copyOf(response.size - SW_SIZE)
    }

    val sw: Int by lazy {
        sw1.shl(8).or(sw2)
    }

    val sw1: Int by lazy {
        0xFF and response[response.size - 2].toInt()
    }

    val sw2: Int by lazy {
        0xFF and response[response.size - 1].toInt()
    }

    val isOk: Boolean =
        // Refer to the clause 10.2.1.1 Normal processing in ETSI TS 102 221.
        when (sw1) {
            0x90 -> true /* Normal ending of the command */
            0x91 -> true /* Normal ending of the command, with extra information
                            from the proactive UICC containing a command for the terminal.
                            SW2 is the length of the response data */
            0x92 -> true /* Normal ending of the command, with extra information
                            concerning an ongoing data transfer session. */
            else -> false
        }
}
