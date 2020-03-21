/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.io

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
}
