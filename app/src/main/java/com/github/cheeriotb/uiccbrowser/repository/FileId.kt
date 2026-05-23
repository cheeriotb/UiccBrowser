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
        const val EF_ATR = "2F01"
        const val EF_PL = "2F05"
        const val EF_ARR = "2F06"
        const val EF_UMPC = "2F08"
        const val EF_ICCID = "2FE2"

        const val EF_USIM_LI = "6F05"
        const val EF_USIM_ARR = "6F06"
        const val EF_USIM_IMSI = "6F07"
        const val EF_USIM_KEYS = "6F08"
        const val EF_USIM_KEYS_PS = "6F09"
        const val EF_USIM_DCK = "6F2C"
        const val EF_USIM_HPPLMN = "6F31"
        const val EF_USIM_CNL = "6F32"
        const val EF_USIM_ACM_MAX = "6F37"
        const val EF_USIM_UST = "6F38"
        const val EF_USIM_ACM = "6F39"
        const val EF_USIM_FDN = "6F3B"
        const val EF_USIM_SMS = "6F3C"
        const val EF_USIM_GID1 = "6F3E"
        const val EF_USIM_GID2 = "6F3F"
        const val EF_USIM_MSISDN = "6F40"
        const val EF_USIM_PUCT = "6F41"
        const val EF_USIM_SMSP = "6F42"
        const val EF_USIM_SMSS = "6F43"
        const val EF_USIM_CBMI = "6F45"
        const val EF_USIM_SPN = "6F46"
        const val EF_USIM_SMSR = "6F47"
        const val EF_USIM_CBMID = "6F48"
        const val EF_USIM_SDN = "6F49"
        const val EF_USIM_EXT2 = "6F4B"
        const val EF_USIM_EXT3 = "6F4C"
        const val EF_USIM_BDN = "6F4D"
        const val EF_USIM_EXT5 = "6F4E"
        const val EF_USIM_CCP2 = "6F4F"
        const val EF_USIM_CBMIR = "6F50"
        const val EF_USIM_EXT4 = "6F55"
        const val EF_USIM_EST = "6F56"
        const val EF_USIM_ACL = "6F57"
        const val EF_USIM_CMI = "6F58"
        const val EF_USIM_START_HFN = "6F5B"
        const val EF_USIM_THRESHOLD = "6F5C"
        const val EF_USIM_PLMN_W_ACT = "6F60"
        const val EF_USIM_OPLMN_W_ACT = "6F61"
        const val EF_USIM_HPLMN_W_ACT = "6F62"
        const val EF_USIM_PSLOCI = "6F73"
        const val EF_USIM_ACC = "6F78"
        const val EF_USIM_FPLMN = "6F7B"
        const val EF_USIM_LOCI = "6F7E"
        const val EF_USIM_ICI = "6F80"
        const val EF_USIM_OCI = "6F81"
        const val EF_USIM_ICT = "6F82"
        const val EF_USIM_OCT = "6F83"
        const val EF_USIM_AD = "6FAD"
        const val EF_USIM_EMLPP = "6FB5"
        const val EF_USIM_AAEM = "6FB6"
        const val EF_USIM_ECC = "6FB7"
        const val EF_USIM_HIDDENKEY = "6FC3"
        const val EF_USIM_NETPAR = "6FC4"
        const val EF_USIM_PNN = "6FC5"
        const val EF_USIM_OPL = "6FC6"
        const val EF_USIM_MBDN = "6FC7"
        const val EF_USIM_EXT6 = "6FC8"
        const val EF_USIM_MBI = "6FC9"
        const val EF_USIM_MWIS = "6FCA"
        const val EF_USIM_CFIS = "6FCB"
        const val EF_USIM_EXT7 = "6FCC"
        const val EF_USIM_SPDI = "6FCD"
        const val EF_USIM_MMSN = "6FCE"
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
