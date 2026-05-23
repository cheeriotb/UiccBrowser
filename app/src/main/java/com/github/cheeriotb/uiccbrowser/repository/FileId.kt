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

        const val EF_USIM_EAKA = "6F01"
        const val EF_USIM_OCST = "6F02"
        const val EF_USIM_LI = "6F05"
        const val EF_USIM_ARR = "6F06"
        const val EF_USIM_IMSI = "6F07"
        const val EF_USIM_KEYS = "6F08"
        const val EF_USIM_KEYS_PS = "6F09"
        const val EF_USIM_AC_GBAUAPI = "6F0A"
        const val EF_USIM_IMSDCI = "6F0B"
        const val EF_USIM_OPLMN_W_ACT_LSP = "6F0C"
        const val EF_USIM_LSPPLMN = "6F0D"
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
        const val EF_USIM_VGCS = "6FB1"
        const val EF_USIM_VGCSS = "6FB2"
        const val EF_USIM_VBS = "6FB3"
        const val EF_USIM_VBSS = "6FB4"
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
        const val EF_USIM_EXT8 = "6FCF"
        const val EF_USIM_MMSICP = "6FD0"
        const val EF_USIM_MMSUP = "6FD1"
        const val EF_USIM_MMSUCP = "6FD2"
        const val EF_USIM_NIA = "6FD3"
        const val EF_USIM_VGCSCA = "6FD4"
        const val EF_USIM_VBSCA = "6FD5"
        const val EF_USIM_GBABP = "6FD6"
        const val EF_USIM_MSK = "6FD7"
        const val EF_USIM_MUK = "6FD8"
        const val EF_USIM_EHPLMN = "6FD9"
        const val EF_USIM_GBANL = "6FDA"
        const val EF_USIM_EHPLMNPI = "6FDB"
        const val EF_USIM_LRPLMNSI = "6FDC"
        const val EF_USIM_NAFKCA = "6FDD"
        const val EF_USIM_SPNI = "6FDE"
        const val EF_USIM_PNNI = "6FDF"
        const val EF_USIM_NCP_IP = "6FE2"
        const val EF_USIM_EPSLOCI = "6FE3"
        const val EF_USIM_EPSNSC = "6FE4"
        const val EF_USIM_UFC = "6FE6"
        const val EF_USIM_UICCIARI = "6FE7"
        const val EF_USIM_NASCONFIG = "6FE8"
        const val EF_USIM_PWS = "6FEC"
        const val EF_USIM_FDNURI = "6FED"
        const val EF_USIM_BDNURI = "6FEE"
        const val EF_USIM_SDNURI = "6FEF"
        const val EF_USIM_IAL = "6FF0"
        const val EF_USIM_IPS = "6FF1"
        const val EF_USIM_IPD = "6FF2"
        const val EF_USIM_EPDGID = "6FF3"
        const val EF_USIM_EPDGSELECTION = "6FF4"
        const val EF_USIM_EPDGIDEM = "6FF5"
        const val EF_USIM_EPDGSELECTIONEM = "6FF6"
        const val EF_USIM_FROMPREFERRED = "6FF7"
        const val EF_USIM_IMS_CONFIG_DATA = "6FF8"
        const val EF_USIM_3GPPPSDATAOFF = "6FF9"
        const val EF_USIM_3GPPPSDATAOFF_SERVICE_LIST = "6FFA"
        const val EF_USIM_TVCONFIG = "6FFB"
        const val EF_USIM_XCAP_CONFIG_DATA = "6FFC"
        const val EF_USIM_EARFCN_LIST = "6FFD"
        const val EF_USIM_MUDMID_CONFIG_DATA = "6FFE"
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
