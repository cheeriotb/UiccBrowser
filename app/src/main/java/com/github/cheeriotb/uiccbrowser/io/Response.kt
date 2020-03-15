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
        private const val STATUS_WORD_SIZE = 2
    }

    init {
        require(response.size >= STATUS_WORD_SIZE) {
            "The size of the response data must be at least 2 for the status word"
        }
        require((response.size - STATUS_WORD_SIZE) <= 65536) {
            "The size of the response data must not be greater than 65536 (+ 2)"
        }
    }

    val data: ByteArray by lazy {
        response.copyOf(response.size - STATUS_WORD_SIZE)
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
