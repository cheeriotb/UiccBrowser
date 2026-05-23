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
import com.github.cheeriotb.uiccbrowser.element.ef.IsimEfDecoders
import com.github.cheeriotb.uiccbrowser.element.ef.MfEfDecoders
import com.github.cheeriotb.uiccbrowser.element.ef.Usim5gsEfDecoders
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
            FileId.EF_ATR to MfEfDecoders::decodeAtr,
            FileId.EF_PL to MfEfDecoders::decodePl,
            FileId.EF_ARR to EfArrRecord::decode,
            FileId.EF_UMPC to MfEfDecoders::decodeUmpc,
            FileId.EF_ICCID to MfEfDecoders::decodeIccid
        ),
        EfContext.USIM to mapOf(
            FileId.EF_USIM_LI to UsimEfDecoders::decodeLi,
            FileId.EF_USIM_ARR to EfArrRecord::decode,
            FileId.EF_USIM_IMSI to UsimEfDecoders::decodeImsi,
            FileId.EF_USIM_KEYS to UsimEfDecoders::decodeKeys,
            FileId.EF_USIM_KEYS_PS to UsimEfDecoders::decodeKeysPs,
            FileId.EF_USIM_DCK to UsimEfDecoders::decodeDck,
            FileId.EF_USIM_HPPLMN to UsimEfDecoders::decodeHpplmn,
            FileId.EF_USIM_CNL to UsimEfDecoders::decodeCnl,
            FileId.EF_USIM_ACM_MAX to UsimEfDecoders::decodeAcmMax,
            FileId.EF_USIM_UST to UsimEfDecoders::decodeUst,
            // (Cyclic) FileId.EF_USIM_ACM to UsimEfDecoders::decodeAcm,
            FileId.EF_USIM_CBMID to UsimEfDecoders::decodeCbmid,
            FileId.EF_USIM_ECC to UsimEfDecoders::decodeEcc,
            FileId.EF_USIM_CBMIR to UsimEfDecoders::decodeCbmir,
            FileId.EF_USIM_PSLOCI to UsimEfDecoders::decodePsloci,
            FileId.EF_USIM_FDN to UsimEfDecoders::decodeFdn,
            FileId.EF_USIM_SMS to UsimEfDecoders::decodeSms,
            FileId.EF_USIM_MSISDN to UsimEfDecoders::decodeMsisdn,
            FileId.EF_USIM_SMSP to UsimEfDecoders::decodeSmsp,
            FileId.EF_USIM_SMSS to UsimEfDecoders::decodeSmss,
            FileId.EF_USIM_SDN to UsimEfDecoders::decodeSdn,
            FileId.EF_USIM_EXT2 to UsimEfDecoders::decodeExt2,
            FileId.EF_USIM_EXT3 to UsimEfDecoders::decodeExt3,
            FileId.EF_USIM_BDN to UsimEfDecoders::decodeBdn,
            FileId.EF_USIM_SMSR to UsimEfDecoders::decodeSmsr,
            // (Cyclic) FileId.EF_USIM_ICI to UsimEfDecoders::decodeIci,
            // (Cyclic) FileId.EF_USIM_OCI to UsimEfDecoders::decodeOci,
            // (Cyclic) FileId.EF_USIM_ICT to UsimEfDecoders::decodeIct,
            // (Cyclic) FileId.EF_USIM_OCT to UsimEfDecoders::decodeOct,
            FileId.EF_USIM_EXT5 to UsimEfDecoders::decodeExt5,
            FileId.EF_USIM_CCP2 to UsimEfDecoders::decodeCcp2,
            FileId.EF_USIM_EMLPP to UsimEfDecoders::decodeEmlpp,
            FileId.EF_USIM_AAEM to UsimEfDecoders::decodeAaem,
            FileId.EF_USIM_HIDDENKEY to UsimEfDecoders::decodeHiddenkey,
            FileId.EF_USIM_EXT4 to UsimEfDecoders::decodeExt4,
            FileId.EF_USIM_CMI to UsimEfDecoders::decodeCmi,
            FileId.EF_USIM_EST to UsimEfDecoders::decodeEst,
            FileId.EF_USIM_ACL to UsimEfDecoders::decodeAcl,
            FileId.EF_USIM_START_HFN to UsimEfDecoders::decodeStartHfn,
            FileId.EF_USIM_THRESHOLD to UsimEfDecoders::decodeThreshold,
            FileId.EF_USIM_OPLMN_W_ACT to UsimEfDecoders::decodeOplmnWAct,
            FileId.EF_USIM_HPLMN_W_ACT to UsimEfDecoders::decodeHplmnWAct,
            FileId.EF_USIM_NETPAR to UsimEfDecoders::decodeNetpar,
            FileId.EF_USIM_PNN to UsimEfDecoders::decodePnn,
            FileId.EF_USIM_OPL to UsimEfDecoders::decodeOpl,
            FileId.EF_USIM_MBDN to UsimEfDecoders::decodeMbdn,
            FileId.EF_USIM_EXT6 to UsimEfDecoders::decodeExt6,
            FileId.EF_USIM_MBI to UsimEfDecoders::decodeMbi,
            FileId.EF_USIM_MWIS to UsimEfDecoders::decodeMwis,
            FileId.EF_USIM_CFIS to UsimEfDecoders::decodeCfis,
            FileId.EF_USIM_EXT7 to UsimEfDecoders::decodeExt7,
            FileId.EF_USIM_SPDI to UsimEfDecoders::decodeSpdi,
            FileId.EF_USIM_MMSN to UsimEfDecoders::decodeMmsn,
            FileId.EF_USIM_GID1 to UsimEfDecoders::decodeGid1,
            FileId.EF_USIM_GID2 to UsimEfDecoders::decodeGid2,
            FileId.EF_USIM_PUCT to UsimEfDecoders::decodePuct,
            FileId.EF_USIM_CBMI to UsimEfDecoders::decodeCbmi,
            FileId.EF_USIM_SPN to UsimEfDecoders::decodeSpn,
            FileId.EF_USIM_PLMN_W_ACT to UsimEfDecoders::decodePlmnWAct,
            FileId.EF_USIM_ACC to UsimEfDecoders::decodeAcc,
            FileId.EF_USIM_FPLMN to UsimEfDecoders::decodeFplmn,
            FileId.EF_USIM_LOCI to UsimEfDecoders::decodeLoci,
            FileId.EF_USIM_AD to UsimEfDecoders::decodeAd,
            FileId.DF_USIM_5GS + FileId.EF_USIM_5GS_SUCI_CALC_INFO to
                    Usim5gsEfDecoders::decodeSuciCalcInfo,
            FileId.DF_USIM_5GS + FileId.EF_USIM_5GS_OPL5G to Usim5gsEfDecoders::decodeOpl5g
        ),
        EfContext.ISIM to mapOf(
            FileId.EF_ISIM_IMPI to IsimEfDecoders::decodeImpi,
            FileId.EF_ISIM_DOMAIN to IsimEfDecoders::decodeDomain,
            FileId.EF_ISIM_IMPU to IsimEfDecoders::decodeImpu
        )
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
