/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.repository

data class FileId(
    val aid: String = AID_NONE,
    val path: String = PATH_MF,
    val fileId: String = FID_ALMIGHTY
) {
    companion object {
        const val AID_NONE = ""
        const val PATH_MF = ""
        const val PATH_ADF = "7FFF"
        const val FID_ALMIGHTY = "****"

        const val MF = ""

        const val EF_DIR = "2F00"
        const val EF_PL = "2F05"
        const val EF_ARR = "2F06"
        const val EF_UMPC = "2F08"
        const val EF_ICCID = "2FE2"

        const val EF_USIM_LI = "6F05"
        const val EF_USIM_ARR = "6F06"
        const val EF_USIM_IMSI = "6F07"
        const val EF_USIM_KEYS = "6F08"
        const val EF_USIM_KEYS_PS = "6F09"
        const val EF_USIM_HPPLMN = "6F31"
        const val EF_USIM_ACM_MAX = "6F37"
        const val EF_USIM_UST = "6F38"
        const val EF_USIM_GID1 = "6F3E"
        const val EF_USIM_GID2 = "6F3F"
        const val EF_USIM_PUCT = "6F41"
        const val EF_USIM_CBMI = "6F45"
        const val EF_USIM_SPN = "6F46"
        const val EF_USIM_PLMN_W_ACT = "6F60"
        const val EF_USIM_ACC = "6F78"
        const val EF_USIM_FPLMN = "6F7B"
        const val EF_USIM_LOCI = "6F7E"
        const val EF_USIM_AD = "6FAD"
        const val DF_USIM_5GS = "5FC0"
        const val EF_USIM_5GS_SUCI_CALC_INFO = "4F07"
        const val EF_USIM_5GS_OPL5G = "4F08"

        const val EF_ISIM_IMPI = "6F02"
        const val EF_ISIM_DOMAIN = "6F03"
        const val EF_ISIM_IMPU = "6F04"
    }

    class Builder(
        private var aid: String = AID_NONE,
        private var path: String = PATH_MF,
        private var fileId: String = FID_ALMIGHTY
    ) {
        fun aid(aid: String) = apply { this.aid = aid }
        fun path(path: String) = apply { this.path = path }
        fun fileId(fileId: String) = apply { this.fileId = fileId }
        fun build() = FileId(aid, path, fileId)
    }
}
