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
import com.github.cheeriotb.uiccbrowser.element.ef.UsimEfDecoders
import com.github.cheeriotb.uiccbrowser.repository.FileId

typealias EfDecoder = (Resources, ByteArray) -> Element?

object EfDecoderRegistry {

    private enum class EfContext { MF, USIM, ISIM }

    // Keys are concatenated paths from the ADF/MF root (parentPath + fileId, uppercase).
    // e.g. EF DIR under MF -> "2F00", EF SPN under USIM ADF -> "6F46"
    private val maps: Map<EfContext, Map<String, EfDecoder>> = mapOf(
        EfContext.MF   to mapOf(
            FileId.EF_DIR to AppTemplate::decode,
            FileId.EF_ARR to EfArrRecord::decode
        ),
        EfContext.USIM to mapOf(
            FileId.EF_USIM_LI to UsimEfDecoders::decodeLi,
            FileId.EF_USIM_ARR to EfArrRecord::decode,
            FileId.EF_USIM_IMSI to UsimEfDecoders::decodeImsi,
            FileId.EF_USIM_KEYS to UsimEfDecoders::decodeKeys,
            FileId.EF_USIM_KEYS_PS to UsimEfDecoders::decodeKeysPs,
            FileId.EF_USIM_HPPLMN to UsimEfDecoders::decodeHpplmn,
            FileId.EF_USIM_ACM_MAX to UsimEfDecoders::decodeAcmMax,
            FileId.EF_USIM_PLMN_W_ACT to UsimEfDecoders::decodePlmnWAct
        ),
        EfContext.ISIM to emptyMap()
    )

    private fun contextFrom(aid: String): EfContext? = when {
        aid == FileId.AID_NONE            -> EfContext.MF
        AppTemplate.APP_USIM in aid       -> EfContext.USIM
        AppTemplate.APP_ISIM in aid       -> EfContext.ISIM
        else                              -> null
    }

    fun find(aid: String, path: String): EfDecoder? =
        contextFrom(aid)?.let { maps[it]?.get(
            path.uppercase().removePrefix(FileId.PATH_ADF)) }

    fun has(aid: String, path: String): Boolean =
        contextFrom(aid)?.let { maps[it]?.containsKey(
            path.uppercase().removePrefix(FileId.PATH_ADF)) } == true
}
