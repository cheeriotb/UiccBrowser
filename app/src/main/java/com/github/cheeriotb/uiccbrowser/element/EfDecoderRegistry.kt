/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.element

import android.content.res.Resources
import com.github.cheeriotb.uiccbrowser.element.ef.AppTemplate
import com.github.cheeriotb.uiccbrowser.element.ef.EfArrRecord
import com.github.cheeriotb.uiccbrowser.repository.FileId

typealias EfDecoder = (Resources, ByteArray) -> Element?

object EfDecoderRegistry {

    private enum class EfContext { MF, USIM, ISIM }

    // Keys are concatenated paths from the ADF/MF root (parentPath + fileId, uppercase).
    // e.g. EF DIR under MF -> "2F00", EF SPN under USIM ADF -> "7FFF6F46"
    private val maps: Map<EfContext, Map<String, EfDecoder>> = mapOf(
        EfContext.MF   to mapOf(
            FileId.EF_DIR to AppTemplate::decode,
            FileId.EF_ARR to EfArrRecord::decode
        ),
        EfContext.USIM to emptyMap(),
        EfContext.ISIM to emptyMap()
    )

    private fun contextFrom(aid: String): EfContext? = when {
        aid == FileId.AID_NONE            -> EfContext.MF
        AppTemplate.APP_USIM in aid       -> EfContext.USIM
        AppTemplate.APP_ISIM in aid       -> EfContext.ISIM
        else                              -> null
    }

    fun find(aid: String, path: String): EfDecoder? =
        contextFrom(aid)?.let { maps[it]?.get(path.uppercase()) }

    fun has(aid: String, path: String): Boolean =
        contextFrom(aid)?.let { maps[it]?.containsKey(path.uppercase()) } == true
}
