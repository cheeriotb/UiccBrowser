/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.repository

import com.github.cheeriotb.uiccbrowser.cacheio.SelectResponse

class Result(
    val fileId: String,
    val data: ByteArray,
    val sw: Int
) {
    val sw1: Int by lazy {
        sw.shr(8)
    }

    val sw2: Int by lazy {
        sw.and(0xFF)
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

    companion object {
        fun listFrom(source: List<SelectResponse>): List<Result> {
            val destination: MutableList<Result> = mutableListOf()
            source.forEach { destination.add(Result(it.fileId, it.data, it.sw)) }
            return destination
        }
    }
}
