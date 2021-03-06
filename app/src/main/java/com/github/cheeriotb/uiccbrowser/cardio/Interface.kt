/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.cardio

interface Interface {
    val isAvailable: Boolean

    companion object {
        val NO_AID_SPECIFIED = ByteArray(0)
        val SW_SUCCESS = byteArrayOf(b(0x90), b(0x00))
        val SW_INTERNAL_EXCEPTION = byteArrayOf(b(0x6F), b(0x00))
        const val OPEN_P2 = 0x04
        private fun b(byte: Int) = byte.toByte()
    }

    enum class OpenChannelResult {
        /** No error happened. */
        SUCCESS,
        /** No logical channels are available. */
        MISSING_RESOURCE,
        /** The specified AID was not found. */
        NO_SUCH_ELEMENT,
        /** Unspecific error happened */
        GENERIC_FAILURE
    }

    fun openChannel(aid: ByteArray = NO_AID_SPECIFIED): OpenChannelResult
    fun transmit(command: Command): Response
    fun closeRemainingChannel()
    fun dispose()
}
