/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.repository

import com.github.cheeriotb.uiccbrowser.cacheio.SelectResponse

data class Result(
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

    class Builder(
        private var fileId: String = "",
        private var data: ByteArray = DATA_NONE,
        private var sw: Int = SW_NORMAL
    ) {
        fun fileId(fileId: String) = apply { this.fileId = fileId }
        fun data(data: ByteArray) = apply { this.data = data }
        fun sw(sw: Int) = apply { this.sw = sw }
        fun build() = Result(fileId, data, sw)
    }

    companion object {
        fun listFrom(source: List<SelectResponse>): List<Result> {
            val destination: MutableList<Result> = mutableListOf()
            source.forEach {
                val result = Builder()
                        .fileId(it.fileId)
                        .data(it.data)
                        .sw(it.sw)
                        .build()
                destination.add(result)
            }
            return destination
        }

        fun from(source: SelectResponse?): List<Result> {
            val destination: MutableList<Result> = mutableListOf()
            if (source != null) {
                val result = Builder()
                        .fileId(source.fileId)
                        .data(source.data)
                        .sw(source.sw)
                        .build()
                destination.add(result)
            }
            return destination
        }

        const val SW_NORMAL = 0x9000
        const val SW_NOT_FOUND = 0x6A82

        val DATA_NONE = ByteArray(0)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Result

        if (fileId != other.fileId) return false
        if (!data.contentEquals(other.data)) return false
        if (sw != other.sw) return false
        if (isOk != other.isOk) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileId.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + sw
        result = 31 * result + isOk.hashCode()
        return result
    }
}
