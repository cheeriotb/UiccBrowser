/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.element.ef

import android.content.res.Resources
import com.github.cheeriotb.uiccbrowser.R
import com.github.cheeriotb.uiccbrowser.element.BerTlvElement
import com.github.cheeriotb.uiccbrowser.element.ConstructedElement
import com.github.cheeriotb.uiccbrowser.element.Element
import com.github.cheeriotb.uiccbrowser.element.PrimitiveElement
import com.github.cheeriotb.uiccbrowser.util.BerTlv
import com.github.cheeriotb.uiccbrowser.util.StringUtils
import com.github.cheeriotb.uiccbrowser.util.Tlv
import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString
import kotlin.math.pow
import java.util.Locale

// ETSI TS 131 102, clauses 4.2.1 to 4.2.38.
class UsimEfDecoders {
    companion object {
        private const val IMSI_LENGTH = 9
        private const val KEYS_LENGTH = 33
        private const val HPPLMN_LENGTH = 1
        private const val ACM_MAX_LENGTH = 3
        private const val ACM_LENGTH = 3
        private const val SPN_LENGTH = 17
        private const val PUCT_LENGTH = 5
        private const val ACC_LENGTH = 2
        private const val LOCI_LENGTH = 11
        private const val MIN_AD_LENGTH = 4
        private const val LI_ENTRY_LENGTH = 2
        private const val CBMI_ENTRY_LENGTH = 2
        private const val CBMIR_ENTRY_LENGTH = 4
        private const val ECC_MIN_LENGTH = 3
        private const val PSLOCI_LENGTH = 14
        private const val DIALING_NUMBER_TRAILER_LENGTH = 14
        private const val SMS_LENGTH = 176
        private const val SMSP_TRAILER_LENGTH = 28
        private const val SMSS_LENGTH = 2
        private const val SMSR_MIN_LENGTH = 30
        private const val ICI_TRAILER_LENGTH = 28
        private const val OCI_TRAILER_LENGTH = 27
        private const val CALL_TIMER_LENGTH = 3
        private const val EXTENSION_RECORD_LENGTH = 13
        private const val CCP2_MIN_LENGTH = 15
        private const val FPLMN_ENTRY_LENGTH = 3
        private const val PLMN_W_ACT_ENTRY_LENGTH = 5
        private const val MIN_PLMN_W_ACT_LENGTH = 40
        private const val MIN_FPLMN_LENGTH = 12
        private const val EMLPP_LENGTH = 2
        private const val AAEM_LENGTH = 1
        private const val HIDDEN_KEY_LENGTH = 4
        private const val BDN_TRAILER_LENGTH = 15
        private const val CMI_MIN_LENGTH = 2
        private const val DCK_LENGTH = 16
        private const val CNL_ENTRY_LENGTH = 6
        private const val START_HFN_LENGTH = 6
        private const val THRESHOLD_LENGTH = 3
        private const val MIN_HPLMN_W_ACT_LENGTH = 5
        private const val MIN_NETPAR_LENGTH = 46
        private const val MIN_PNN_LENGTH = 3
        private const val OPL_ENTRY_LENGTH = 8
        private const val MIN_MBI_LENGTH = 4
        private const val MIN_MWIS_LENGTH = 5
        private const val CFIS_LENGTH = 16
        private const val MIN_MMSN_LENGTH = 5
        private const val MIN_EXT8_LENGTH = 2
        private const val MIN_NIA_LENGTH = 2
        private const val GROUP_ID_LENGTH = 4
        private const val GROUP_STATUS_LENGTH = 7
        private const val CIPHERING_ALGORITHM_PAIR_LENGTH = 2
        private const val MIN_GBABP_LENGTH = 3
        private const val MIN_MSK_LENGTH = 20
        private const val MSK_TRAILING_ENTRY_LENGTH = 8
        private const val EHPLMN_ENTRY_LENGTH = 3
        private const val ONE_BYTE_FILE_LENGTH = 1
        private const val EPSLOCI_LENGTH = 18
        private const val EPSNSC_MIN_LENGTH = 54
        private const val PWS_MIN_LENGTH = 1
        private const val IAL_ENTRY_LENGTH = 8
        private const val IPS_LENGTH = 4
        private const val IPD_LENGTH = 8
        private const val EPDG_SELECTION_ENTRY_LENGTH = 7
        private const val THREE_GPP_PS_DATA_OFF_LENGTH = 4
        private const val TVCONFIG_PLMN_LENGTH = 3
        private const val TVCONFIG_TMGI_ENTRY_LENGTH = 9
        private const val TVCONFIG_EARFCN_ENTRY_LENGTH = 4
        private const val EARFCN_LENGTH = 4
        private const val GEOGRAPHICAL_POINT_LENGTH = 6
        private const val OCST_ENTRY_LENGTH = 3
        private const val OPLMN_W_ACT_LSP_MIN_LENGTH = 7
        private const val OPLMN_W_ACT_LSP_ENTRY_LENGTH = 6
        private const val TAG_PNN_FULL_NAME = 0x43
        private const val TAG_PNN_SHORT_NAME = 0x45
        private const val TAG_PNN_ADDITIONAL_INFORMATION = 0x80
        private const val TAG_SPDI_TEMPLATE = 0xA3
        private const val TAG_SPDI_PLMN_LIST = 0x80
        private const val TAG_NETPAR_GSM_CELL_INFORMATION = 0xA0
        private const val TAG_NETPAR_FDD_CELL_INFORMATION = 0xA1
        private const val TAG_NETPAR_TDD_CELL_INFORMATION = 0xA2
        private const val TAG_NETPAR_INFORMATION_1 = 0x80
        private const val TAG_NETPAR_INFORMATION_2 = 0x81
        private const val TAG_NETPAR_CORE_NETWORK_BEARER = 0x82
        private const val TAG_MMS_CONNECTIVITY_PARAMETERS = 0xAB
        private const val TAG_MMS_IMPLEMENTATION = 0x80
        private const val TAG_MMS_RELAY_SERVER = 0x81
        private const val TAG_INTERFACE_CORE_NETWORK_BEARER = 0x82
        private const val TAG_GATEWAY = 0x83
        private const val TAG_MMS_AUTHENTICATION_MECHANISM = 0x84
        private const val TAG_MMS_AUTHENTICATION_USER_NAME = 0x85
        private const val TAG_MMS_USER_PREFERENCES = 0xA0
        private const val TAG_MMS_USER_PROFILE_NAME = 0x81
        private const val TAG_MMS_USER_PREFERENCE_INFORMATION = 0x82
        private const val TAG_MUK_ID = 0xA0
        private const val TAG_NAF_ID = 0x80
        private const val TAG_B_TID = 0x81
        private const val TAG_MUK_IDR = 0x80
        private const val TAG_MUK_IDI = 0x82
        private const val TAG_NAF_KEY_CENTRE_ADDRESS = 0x80
        private const val TAG_DATA_DESTINATION_ADDRESS_RANGE = 0x83
        private const val TAG_ACCESS_POINT_NAME = 0x80
        private const val TAG_LOGIN = 0x81
        private const val TAG_PASSWORD = 0x82
        private const val TAG_BEARER_DESCRIPTION = 0x84
        private const val TAG_EPS_NAS_SECURITY_CONTEXT = 0xA0
        private const val TAG_KSIASME = 0x80
        private const val TAG_KASME = 0x81
        private const val TAG_UPLINK_NAS_COUNT = 0x82
        private const val TAG_DOWNLINK_NAS_COUNT = 0x83
        private const val TAG_NAS_SECURITY_ALGORITHMS = 0x84
        private const val TAG_NAS_SIGNALLING_PRIORITY = 0x80
        private const val TAG_URI = 0x80
        private const val TAG_ICSI = 0x80
        private const val TAG_EPDG_IDENTIFIER = 0x80
        private const val TAG_TVCONFIG_TMGI_LIST = 0xA0
        private const val TAG_TVCONFIG_EARFCN_LIST = 0xA1
        private const val TAG_EARFCN_LIST = 0xA0
        private const val TAG_EARFCN = 0x80
        private const val TAG_GEOGRAPHICAL_AREA_POLYGON = 0x81
        private const val TAG_OCST_PARAMETERS = 0x80
        private const val TAG_APPLET_NAF_ACCESS_CONTROL = 0x80

        private val netparCellInformationTags = setOf(
                TAG_NETPAR_GSM_CELL_INFORMATION,
                TAG_NETPAR_FDD_CELL_INFORMATION,
                TAG_NETPAR_TDD_CELL_INFORMATION
        )

        /**
         * Decodes EF LI into ordered two-byte ISO 639 language entries. Unused entries are
         * represented by FF FF and displayed without passing through string decoding.
         */
        fun decodeLi(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty() || bytes.size % LI_ENTRY_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_li_label)
                    .decoder(::liDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF IMSI into length and IMSI bytes. The IMSI bytes keep their raw hex value and
         * also display the decimal IMSI digits encoded in swapped BCD nibbles.
         */
        fun decodeImsi(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != IMSI_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_imsi_label)
                    .decoder(::imsiDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF Keys into KSI, ciphering key, and integrity key.
         */
        fun decodeKeys(resources: Resources, bytes: ByteArray): Element? =
                decodeKeysFile(
                        resources,
                        bytes,
                        R.string.ef_keys_label,
                        R.string.key_set_identifier_label,
                        R.string.ciphering_key_label,
                        R.string.integrity_key_label
                )

        /**
         * Decodes EF KeysPS into KSIPS, packet-switched ciphering key, and integrity key.
         */
        fun decodeKeysPs(resources: Resources, bytes: ByteArray): Element? =
                decodeKeysFile(
                        resources,
                        bytes,
                        R.string.ef_keys_ps_label,
                        R.string.key_set_identifier_ps_label,
                        R.string.ciphering_key_ps_label,
                        R.string.integrity_key_ps_label
                )

        /**
         * Decodes EF PLMNwAcT into five-byte priority entries containing PLMN and access
         * technology masks.
         */
        fun decodePlmnWAct(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < MIN_PLMN_W_ACT_LENGTH ||
                    bytes.size % PLMN_W_ACT_ENTRY_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_plmn_w_act_label)
                    .decoder(::plmnWActDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF HPPLMN into the configured higher priority PLMN search interval.
         */
        fun decodeHpplmn(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != HPPLMN_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_hpplmn_label)
                    .decoder(::hpplmnDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF ACMmax into the three-byte binary maximum accumulated call meter value.
         */
        fun decodeAcmMax(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != ACM_MAX_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_acm_max_label)
                    .decoder(::acmMaxDecoder)
                    .build(resources)
        }

        /** Decodes one EF ACM cyclic record into its accumulated unit count. */
        fun decodeAcm(resources: Resources, bytes: ByteArray): Element? =
                decodeThreeByteCounter(
                        resources,
                        bytes,
                        ACM_LENGTH,
                        R.string.ef_acm_label,
                        R.string.accumulated_units_label
                )

        /**
         * Decodes EF UST into one child for each advertised USIM service bit.
         */
        fun decodeUst(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty()) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_ust_label)
                    .decoder(::ustDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF GID1 into the operator-defined USIM group identifier bytes.
         */
        fun decodeGid1(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleElementFile(
                        resources,
                        bytes,
                        R.string.ef_gid1_label,
                        R.string.usim_group_identifiers_label
                )

        /**
         * Decodes EF GID2 into the operator-defined USIM group identifier bytes.
         */
        fun decodeGid2(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleElementFile(
                        resources,
                        bytes,
                        R.string.ef_gid2_label,
                        R.string.usim_group_identifiers_label
                )

        /**
         * Decodes EF SPN into display conditions and the service provider name string.
         */
        fun decodeSpn(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != SPN_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_spn_label)
                    .decoder(::spnDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF PUCT into the three-character currency code and price-per-unit data.
         */
        fun decodePuct(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != PUCT_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_puct_label)
                    .decoder(::puctDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF CBMI into two-byte Cell Broadcast message identifiers.
         */
        fun decodeCbmi(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty() || bytes.size % CBMI_ENTRY_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_cbmi_label)
                    .decoder(::cbmiDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF ACC into allocated access control classes.
         */
        fun decodeAcc(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != ACC_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_acc_label)
                    .decoder(::accDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF FPLMN into forbidden PLMN entries.
         */
        fun decodeFplmn(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < MIN_FPLMN_LENGTH || bytes.size % FPLMN_ENTRY_LENGTH != 0) {
                return null
            }

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_fplmn_label)
                    .decoder(::fplmnDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF LOCI into TMSI, LAI, RFU, and location update status.
         */
        fun decodeLoci(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != LOCI_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_loci_label)
                    .decoder(::lociDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF AD into UE operation mode, additional information, MNC length, and RFU bytes.
         */
        fun decodeAd(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < MIN_AD_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_ad_label)
                    .decoder(::adDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF CBMID into two-byte Cell Broadcast message identifiers.
         */
        fun decodeCbmid(resources: Resources, bytes: ByteArray): Element? =
                decodeCbMessageIdentifierFile(resources, bytes, R.string.ef_cbmid_label)

        /**
         * Decodes EF ECC into emergency call code, alpha identifier, and service category.
         */
        fun decodeEcc(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size <= ECC_MIN_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_ecc_label)
                    .decoder(::eccDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF CBMIR into lower and upper Cell Broadcast message identifier ranges.
         */
        fun decodeCbmir(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty() || bytes.size % CBMIR_ENTRY_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_cbmir_label)
                    .decoder(::cbmirDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF PSLOCI into packet-switched location information.
         */
        fun decodePsloci(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != PSLOCI_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_psloci_label)
                    .decoder(::pslociDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF FDN into alpha identifier, dialling number, CCP2, and EXT2 references.
         */
        fun decodeFdn(resources: Resources, bytes: ByteArray): Element? =
                decodeDiallingNumberFile(
                        resources,
                        bytes,
                        R.string.ef_fdn_label,
                        R.string.ccp2_record_identifier_label,
                        R.string.extension2_record_identifier_label
                )

        /**
         * Decodes EF SMS into status and TPDU bytes.
         */
        fun decodeSms(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != SMS_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_sms_label)
                    .decoder(::smsDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF MSISDN into alpha identifier, dialling number, CCP2, and EXT5 references.
         */
        fun decodeMsisdn(resources: Resources, bytes: ByteArray): Element? =
                decodeDiallingNumberFile(
                        resources,
                        bytes,
                        R.string.ef_msisdn_label,
                        R.string.ccp2_record_identifier_label,
                        R.string.extension5_record_identifier_label
                )

        /**
         * Decodes EF SMSP into alpha identifier and SMS parameter bytes.
         */
        fun decodeSmsp(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size <= SMSP_TRAILER_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_smsp_label)
                    .decoder(::smspDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF SMSS into message reference and memory capacity exceeded flag.
         */
        fun decodeSmss(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != SMSS_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_smss_label)
                    .decoder(::smssDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF SDN into alpha identifier, dialling number, CCP2, and EXT3 references.
         */
        fun decodeSdn(resources: Resources, bytes: ByteArray): Element? =
                decodeDiallingNumberFile(
                        resources,
                        bytes,
                        R.string.ef_sdn_label,
                        R.string.ccp2_record_identifier_label,
                        R.string.extension3_record_identifier_label
                )

        /**
         * Decodes EF EXT2 into one extension record.
         */
        fun decodeExt2(resources: Resources, bytes: ByteArray): Element? =
                decodeExtensionRecordFile(resources, bytes, R.string.ef_ext2_label)

        /**
         * Decodes EF EXT3 into one extension record.
         */
        fun decodeExt3(resources: Resources, bytes: ByteArray): Element? =
                decodeExtensionRecordFile(resources, bytes, R.string.ef_ext3_label)

        /**
         * Decodes EF SMSR into SMS record identifier and status report bytes.
         */
        fun decodeSmsr(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < SMSR_MIN_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_smsr_label)
                    .decoder(::smsrDecoder)
                    .build(resources)
        }

        /** Decodes one EF ICI cyclic record containing incoming call information. */
        fun decodeIci(resources: Resources, bytes: ByteArray): Element? =
                decodeCallInformation(resources, bytes, R.string.ef_ici_label, incoming = true)

        /** Decodes one EF OCI cyclic record containing outgoing call information. */
        fun decodeOci(resources: Resources, bytes: ByteArray): Element? =
                decodeCallInformation(resources, bytes, R.string.ef_oci_label, incoming = false)

        /** Decodes one EF ICT cyclic record into its accumulated incoming call duration. */
        fun decodeIct(resources: Resources, bytes: ByteArray): Element? =
                decodeThreeByteCounter(
                        resources,
                        bytes,
                        CALL_TIMER_LENGTH,
                        R.string.ef_ict_label,
                        R.string.accumulated_call_timer_label,
                        ::secondsInterpreter
                )

        /** Decodes one EF OCT cyclic record into its accumulated outgoing call duration. */
        fun decodeOct(resources: Resources, bytes: ByteArray): Element? =
                decodeThreeByteCounter(
                        resources,
                        bytes,
                        CALL_TIMER_LENGTH,
                        R.string.ef_oct_label,
                        R.string.accumulated_call_timer_label,
                        ::secondsInterpreter
                )

        /**
         * Decodes EF EXT5 into one extension record.
         */
        fun decodeExt5(resources: Resources, bytes: ByteArray): Element? =
                decodeExtensionRecordFile(resources, bytes, R.string.ef_ext5_label)

        /**
         * Decodes EF CCP2 into the bearer capability information element.
         */
        fun decodeCcp2(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < CCP2_MIN_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_ccp2_label)
                    .decoder(::ccp2Decoder)
                    .build(resources)
        }

        /**
         * Decodes EF eMLPP into subscribed priority levels and fast call setup conditions.
         */
        fun decodeEmlpp(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != EMLPP_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_emlpp_label)
                    .decoder(::emlppDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF AaeM into automatic-answer priority levels.
         */
        fun decodeAaem(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleElementFile(
                        resources,
                        bytes,
                        R.string.ef_aaem_label,
                        R.string.automatic_answer_priority_levels_label,
                        AAEM_LENGTH,
                        ::emlppPriorityMaskInterpreter
                )

        /**
         * Decodes EF Hiddenkey into the non-swapped BCD hidden key.
         */
        fun decodeHiddenkey(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleElementFile(
                        resources,
                        bytes,
                        R.string.ef_hiddenkey_label,
                        R.string.hidden_key_label,
                        HIDDEN_KEY_LENGTH,
                        ::nonSwappedBcdStringInterpreter
                )

        /**
         * Decodes EF BDN into alpha identifier, dialling number, CCP2, EXT4, and CMI pointer.
         */
        fun decodeBdn(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size <= BDN_TRAILER_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_bdn_label)
                    .decoder(::bdnDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF EXT4 into one extension record.
         */
        fun decodeExt4(resources: Resources, bytes: ByteArray): Element? =
                decodeExtensionRecordFile(resources, bytes, R.string.ef_ext4_label)

        /**
         * Decodes EF CMI into alpha identifier and comparison method identifier.
         */
        fun decodeCmi(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < CMI_MIN_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_cmi_label)
                    .decoder(::cmiDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF EST into enabled service bits.
         */
        fun decodeEst(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty()) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_est_label)
                    .decoder(::estDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF ACL into APN/DNN count and TLV objects.
         */
        fun decodeAcl(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size <= 1) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_acl_label)
                    .decoder(::aclDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF DCK into four de-personalisation control keys.
         */
        fun decodeDck(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != DCK_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_dck_label)
                    .decoder(::dckDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF CNL into co-operative network list entries.
         */
        fun decodeCnl(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty() || bytes.size % CNL_ENTRY_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_cnl_label)
                    .decoder(::cnlDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF START-HFN into CS and PS START values.
         */
        fun decodeStartHfn(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != START_HFN_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_start_hfn_label)
                    .decoder(::startHfnDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF THRESHOLD into the maximum START value.
         */
        fun decodeThreshold(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleElementFile(
                        resources,
                        bytes,
                        R.string.ef_threshold_label,
                        R.string.maximum_start_value_label,
                        THRESHOLD_LENGTH,
                        ::unsignedIntegerInterpreter
                )

        /**
         * Decodes EF OPLMNwACT into operator-controlled PLMN entries.
         */
        fun decodeOplmnWAct(resources: Resources, bytes: ByteArray): Element? =
                decodePlmnWActFile(resources, bytes, R.string.ef_oplmn_w_act_label,
                        MIN_PLMN_W_ACT_LENGTH)

        /**
         * Decodes EF HPLMNwAcT into HPLMN entries.
         */
        fun decodeHplmnWAct(resources: Resources, bytes: ByteArray): Element? =
                decodePlmnWActFile(resources, bytes, R.string.ef_hplmn_w_act_label,
                        MIN_HPLMN_W_ACT_LENGTH)

        /**
         * Decodes EF NETPAR into BER-TLV cell information objects.
         */
        fun decodeNetpar(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < MIN_NETPAR_LENGTH) return null
            if (BerTlv.listFrom(bytes).none { it.tag in netparCellInformationTags }) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_netpar_label)
                    .decoder(::netparDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF PNN into network name TLV objects.
         */
        fun decodePnn(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < MIN_PNN_LENGTH) return null
            if (BerTlv.listFrom(bytes).none { it.tag == TAG_PNN_FULL_NAME }) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_pnn_label)
                    .decoder(::pnnDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF OPL into LAI/TAI and PLMN network name record identifier.
         */
        fun decodeOpl(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < OPL_ENTRY_LENGTH || bytes.size % OPL_ENTRY_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_opl_label)
                    .decoder(::oplDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF MBDN into alpha identifier, dialling number, CCP2, and EXT6 references.
         */
        fun decodeMbdn(resources: Resources, bytes: ByteArray): Element? =
                decodeDiallingNumberFile(
                        resources,
                        bytes,
                        R.string.ef_mbdn_label,
                        R.string.ccp2_record_identifier_label,
                        R.string.extension6_record_identifier_label
                )

        /**
         * Decodes EF EXT6 into one extension record.
         */
        fun decodeExt6(resources: Resources, bytes: ByteArray): Element? =
                decodeExtensionRecordFile(resources, bytes, R.string.ef_ext6_label)

        /**
         * Decodes EF MBI into mailbox dialling number identifiers.
         */
        fun decodeMbi(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < MIN_MBI_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_mbi_label)
                    .decoder(::mbiDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF MWIS into message waiting status and message counts.
         */
        fun decodeMwis(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < MIN_MWIS_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_mwis_label)
                    .decoder(::mwisDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF CFIS into call forwarding indication fields.
         */
        fun decodeCfis(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != CFIS_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_cfis_label)
                    .decoder(::cfisDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF EXT7 into one extension record.
         */
        fun decodeExt7(resources: Resources, bytes: ByteArray): Element? =
                decodeExtensionRecordFile(resources, bytes, R.string.ef_ext7_label)

        /**
         * Decodes EF SPDI into BER-TLV service provider display information.
         */
        fun decodeSpdi(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty()) return null
            if (BerTlv.listFrom(bytes).none { it.tag == TAG_SPDI_TEMPLATE }) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_spdi_label)
                    .decoder(::spdiDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF MMSN into MMS status, implementation, notification, and extension record.
         */
        fun decodeMmsn(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < MIN_MMSN_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_mmsn_label)
                    .decoder(::mmsnDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF EXT8 into one MMS notification extension record.
         */
        fun decodeExt8(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < MIN_EXT8_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_ext8_label)
                    .decoder(::ext8Decoder)
                    .build(resources)
        }

        /**
         * Decodes EF MMSICP into MMS connectivity parameter TLV objects.
         */
        fun decodeMmsicp(resources: Resources, bytes: ByteArray): Element? =
                decodeMmsConnectivityFile(resources, bytes, R.string.ef_mmsicp_label)

        /**
         * Decodes EF MMSUP into MMS user preference TLV objects.
         */
        fun decodeMmsup(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty()) return null
            if (BerTlv.listFrom(bytes).none { it.tag == TAG_MMS_USER_PREFERENCES }) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_mmsup_label)
                    .decoder(::mmsupDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF MMSUCP into user-defined MMS connectivity parameter TLV objects.
         */
        fun decodeMmsucp(resources: Resources, bytes: ByteArray): Element? =
                decodeMmsConnectivityFile(resources, bytes, R.string.ef_mmsucp_label)

        /**
         * Decodes EF NIA into alerting category and informative text.
         */
        fun decodeNia(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < MIN_NIA_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_nia_label)
                    .decoder(::niaDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF VGCS into four-byte group identifiers.
         */
        fun decodeVgcs(resources: Resources, bytes: ByteArray): Element? =
                decodeGroupIdFile(resources, bytes, R.string.ef_vgcs_label)

        /**
         * Decodes EF VGCSS into activation flags for VGCS group identifiers.
         */
        fun decodeVgcss(resources: Resources, bytes: ByteArray): Element? =
                decodeGroupStatusFile(resources, bytes, R.string.ef_vgcss_label)

        /**
         * Decodes EF VBS into four-byte group identifiers.
         */
        fun decodeVbs(resources: Resources, bytes: ByteArray): Element? =
                decodeGroupIdFile(resources, bytes, R.string.ef_vbs_label)

        /**
         * Decodes EF VBSS into activation flags for VBS group identifiers.
         */
        fun decodeVbss(resources: Resources, bytes: ByteArray): Element? =
                decodeGroupStatusFile(resources, bytes, R.string.ef_vbss_label)

        /**
         * Decodes EF VGCSCA into ciphering algorithm identifiers.
         */
        fun decodeVgcsca(resources: Resources, bytes: ByteArray): Element? =
                decodeCipheringAlgorithmFile(resources, bytes, R.string.ef_vgcsca_label)

        /**
         * Decodes EF VBSCA into ciphering algorithm identifiers.
         */
        fun decodeVbsca(resources: Resources, bytes: ByteArray): Element? =
                decodeCipheringAlgorithmFile(resources, bytes, R.string.ef_vbsca_label)

        /**
         * Decodes EF GBABP into RAND, B-TID, and key lifetime.
         */
        fun decodeGbabp(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < MIN_GBABP_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_gbabp_label)
                    .decoder(::gbabpDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF MSK into MBMS service key list fields.
         */
        fun decodeMsk(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < MIN_MSK_LENGTH ||
                    (bytes.size - MIN_MSK_LENGTH) % MSK_TRAILING_ENTRY_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_msk_label)
                    .decoder(::mskDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF MUK into MBMS user key TLV objects.
         */
        fun decodeMuk(resources: Resources, bytes: ByteArray): Element? =
                decodeTlvFile(resources, bytes, R.string.ef_muk_label, 1, ::mukDecoder)

        /**
         * Decodes EF GBANL into NAF key identifier TLV objects.
         */
        fun decodeGbanl(resources: Resources, bytes: ByteArray): Element? =
                decodeTlvFile(resources, bytes, R.string.ef_gbanl_label, 1, ::gbanlDecoder)

        /**
         * Decodes EF EHPLMN into priority-ordered equivalent HPLMN entries.
         */
        fun decodeEhplmn(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty() || bytes.size % EHPLMN_ENTRY_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_ehplmn_label)
                    .decoder(::ehplmnDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF EHPLMNPI into the EHPLMN display mode.
         */
        fun decodeEhplmnpi(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleElementFile(
                        resources,
                        bytes,
                        R.string.ef_ehplmnpi_label,
                        R.string.ehplmn_presentation_indication_label,
                        ONE_BYTE_FILE_LENGTH,
                        ::ehplmnPresentationIndicationInterpreter
                )

        /**
         * Decodes EF LRPLMNSI into the last RPLMN selection policy.
         */
        fun decodeLrplmnsi(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleElementFile(
                        resources,
                        bytes,
                        R.string.ef_lrplmnsi_label,
                        R.string.last_rplmn_selection_indication_label,
                        ONE_BYTE_FILE_LENGTH,
                        ::lastRplmnSelectionIndicationInterpreter
                )

        /**
         * Decodes EF NAFKCA into NAF key centre TLV objects.
         */
        fun decodeNafkca(resources: Resources, bytes: ByteArray): Element? =
                decodeTlvFile(resources, bytes, R.string.ef_nafkca_label, 1, ::nafkcaDecoder)

        /**
         * Decodes EF SPNI into service provider name icon TLV objects.
         */
        fun decodeSpni(resources: Resources, bytes: ByteArray): Element? =
                decodeTlvFile(resources, bytes, R.string.ef_spni_label, 1, ::iconTlvDecoder)

        /**
         * Decodes EF PNNI into PLMN network name icon TLV objects.
         */
        fun decodePnni(resources: Resources, bytes: ByteArray): Element? =
                decodeTlvFile(resources, bytes, R.string.ef_pnni_label, 1, ::iconTlvDecoder)

        /**
         * Decodes EF NCP-IP into network connectivity parameter TLV objects.
         */
        fun decodeNcpIp(resources: Resources, bytes: ByteArray): Element? =
                decodeTlvFile(resources, bytes, R.string.ef_ncp_ip_label, 1, ::ncpIpDecoder)

        /**
         * Decodes EF EPSLOCI into EPS location information.
         */
        fun decodeEpsloci(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != EPSLOCI_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_epsloci_label)
                    .decoder(::epslociDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF EPSNSC into EPS NAS security context TLV objects.
         */
        fun decodeEpsnsc(resources: Resources, bytes: ByteArray): Element? =
                decodeTlvFile(
                        resources,
                        bytes,
                        R.string.ef_epsnsc_label,
                        EPSNSC_MIN_LENGTH,
                        ::epsnscDecoder
                )

        /**
         * Decodes EF UFC into the USAT facility list.
         */
        fun decodeUfc(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleElementFile(
                        resources,
                        bytes,
                        R.string.ef_ufc_label,
                        R.string.facility_list_label
                )

        /**
         * Decodes EF NASCONFIG into NAS signalling priority configuration.
         */
        fun decodeNasconfig(resources: Resources, bytes: ByteArray): Element? =
                decodeTlvFile(resources, bytes, R.string.ef_nasconfig_label, 1,
                        ::nasconfigDecoder)

        /**
         * Decodes EF UICCIARI into the IARI TLV defined by ETSI TS 131 103.
         */
        fun decodeUicciari(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleStringTlvFile(resources, bytes, R.string.ef_uicciari_label,
                        R.string.uicc_iari_label)

        /**
         * Decodes EF PWS into public warning system configuration.
         */
        fun decodePws(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < PWS_MIN_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_pws_label)
                    .decoder(::pwsDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF FDNURI into URI and alpha identifier fields.
         */
        fun decodeFdnuri(resources: Resources, bytes: ByteArray): Element? =
                decodeUriRecordFile(resources, bytes, R.string.ef_fdnuri_label)

        /**
         * Decodes EF BDNURI into URI and alpha identifier fields.
         */
        fun decodeBdnuri(resources: Resources, bytes: ByteArray): Element? =
                decodeUriRecordFile(resources, bytes, R.string.ef_bdnuri_label)

        /**
         * Decodes EF SDNURI into URI and alpha identifier fields.
         */
        fun decodeSdnuri(resources: Resources, bytes: ByteArray): Element? =
                decodeUriRecordFile(resources, bytes, R.string.ef_sdnuri_label)

        /**
         * Decodes EF IAL into IMEI(SV) entries.
         */
        fun decodeIal(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty() || bytes.size % IAL_ENTRY_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_ial_label)
                    .decoder(::ialDecoder)
                    .build(resources)
        }

        /** Decodes one EF IPS cyclic record containing the latest pairing result. */
        fun decodeIps(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != IPS_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_ips_label)
                    .decoder(::ipsDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF IPD into the paired device IMEI(SV).
         */
        fun decodeIpd(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleElementFile(
                        resources,
                        bytes,
                        R.string.ef_ipd_label,
                        R.string.pairing_device_imeisv_label,
                        IPD_LENGTH,
                        ::swappedBcdStringInterpreter
                )

        /**
         * Decodes EF ePDGId into ePDG identifier TLVs.
         */
        fun decodeEpdgid(resources: Resources, bytes: ByteArray): Element? =
                decodeEpdgIdentifierFile(resources, bytes, R.string.ef_epdgid_label)

        /**
         * Decodes EF ePDGSelection into PLMN and ePDG identifier configuration entries.
         */
        fun decodeEpdgselection(resources: Resources, bytes: ByteArray): Element? =
                decodeEpdgSelectionFile(resources, bytes, R.string.ef_epdgselection_label)

        /**
         * Decodes EF ePDGIdEm into emergency ePDG identifier TLVs.
         */
        fun decodeEpdgidem(resources: Resources, bytes: ByteArray): Element? =
                decodeEpdgIdentifierFile(resources, bytes, R.string.ef_epdgidem_label)

        /**
         * Decodes EF ePDGSelectionEm into emergency ePDG selection entries.
         */
        fun decodeEpdgselectionem(resources: Resources, bytes: ByteArray): Element? =
                decodeEpdgSelectionFile(resources, bytes, R.string.ef_epdgselectionem_label)

        /**
         * Decodes EF FromPreferred into the IMS From header preference.
         */
        fun decodeFrompreferred(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleElementFile(
                        resources,
                        bytes,
                        R.string.ef_frompreferred_label,
                        R.string.from_preferred_label,
                        ONE_BYTE_FILE_LENGTH,
                        ::activatedStateInterpreter
                )

        fun decodeTvconfig(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < TVCONFIG_PLMN_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_tvconfig_label)
                    .decoder(::tvconfigDecoder)
                    .build(resources)
        }

        fun decodeThreeGppPsDataOff(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != THREE_GPP_PS_DATA_OFF_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_3gpppsdataoff_label)
                    .decoder(::threeGppPsDataOffDecoder)
                    .build(resources)
        }

        fun decodeThreeGppPsDataOffServiceList(
            resources: Resources,
            bytes: ByteArray
        ): Element? {
            if (bytes.isEmpty()) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_3gpppsdataoff_service_list_label)
                    .decoder(::threeGppPsDataOffServiceListDecoder)
                    .build(resources)
        }

        fun decodeEarfcnList(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty() || BerTlv.listFrom(bytes).none { it.tag == TAG_EARFCN_LIST }) {
                return null
            }

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_earfcn_list_label)
                    .decoder(::earfcnListDecoder)
                    .build(resources)
        }

        fun decodeEaka(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty()) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_eaka_label)
                    .decoder(::eakaDecoder)
                    .build(resources)
        }

        fun decodeOcst(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty()) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_ocst_label)
                    .decoder(::ocstDecoder)
                    .build(resources)
        }

        fun decodeAcGbauapi(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty()) return null
            if (BerTlv.listFrom(bytes).none { it.tag == TAG_APPLET_NAF_ACCESS_CONTROL }) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_ac_gbauapi_label)
                    .decoder(::acGbauapiDecoder)
                    .build(resources)
        }

        fun decodeImsdci(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleElementFile(
                        resources,
                        bytes,
                        R.string.ef_imsdci_label,
                        R.string.ims_data_channel_indication_label,
                        ONE_BYTE_FILE_LENGTH,
                        ::imsDataChannelIndicationInterpreter
                )

        fun decodeOplmnWActLsp(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < OPLMN_W_ACT_LSP_MIN_LENGTH) return null
            if ((bytes.size - 1) % OPLMN_W_ACT_LSP_ENTRY_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_oplmn_w_act_lsp_label)
                    .decoder(::oplmnWActLspDecoder)
                    .build(resources)
        }

        fun decodeLspplmn(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleElementFile(
                        resources,
                        bytes,
                        R.string.ef_lspplmn_label,
                        R.string.time_interval_label,
                        HPPLMN_LENGTH,
                        ::hpplmnInterpreter
                )

        private fun liDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.asIterable().chunked(LI_ENTRY_LENGTH).mapIndexed { index, entry ->
                PrimitiveElement.Builder(entry.toByteArray())
                        .labelId(R.string.language_code_label)
                        .labelArgs(index + 1)
                        .parent(parent)
                        .interpreter(::languageCodeInterpreter)
                        .build(resources)
            }
        }

        private fun imsiDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                        .labelId(R.string.length_of_imsi_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(1, IMSI_LENGTH))
                        .labelId(R.string.imsi_label)
                        .parent(parent)
                        .interpreter(::imsiInterpreter)
                        .build(resources)
        )

        private fun decodeKeysFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int,
            ksiLabelId: Int,
            ckLabelId: Int,
            ikLabelId: Int
        ): Element? {
            if (bytes.size != KEYS_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder { innerResources, rawData, parent ->
                        keyFileDecoder(innerResources, rawData, parent, ksiLabelId, ckLabelId,
                                ikLabelId)
                    }
                    .build(resources)
        }

        private fun keyFileDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?,
            ksiLabelId: Int,
            ckLabelId: Int,
            ikLabelId: Int
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                        .labelId(ksiLabelId)
                        .parent(parent)
                        .interpreter(::keySetIdentifierInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(1, 17))
                        .labelId(ckLabelId)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(17, KEYS_LENGTH))
                        .labelId(ikLabelId)
                        .parent(parent)
                        .build(resources)
        )

        private fun plmnWActDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.asIterable().chunked(PLMN_W_ACT_ENTRY_LENGTH).mapIndexed {
                index, entry ->
                    ConstructedElement.Builder(entry.toByteArray())
                            .labelId(R.string.plmn_w_act_entry_label)
                            .labelArgs(index + 1)
                            .parent(parent)
                            .decoder { innerResources, entryData, entryParent ->
                                plmnWActEntryDecoder(innerResources, entryData, entryParent)
                            }
                            .interpreter { _, _ -> (index + 1).toString() }
                            .build(resources)
            }
        }

        private fun plmnWActEntryDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val plmn = rawData.copyOfRange(0, 3)
            val accessTechnology = rawData.copyOfRange(3, PLMN_W_ACT_ENTRY_LENGTH)
            val accessTechnologyBuilder = PrimitiveElement.Builder(accessTechnology)
                    .labelId(R.string.access_technology_identifier_label)
                    .parent(parent)
            if (!isUnusedPlmn(plmn)) {
                accessTechnologyBuilder.interpreter(::accessTechnologyInterpreter)
            }
            return listOf(
                PrimitiveElement.Builder(plmn)
                        .labelId(R.string.plmn_label)
                        .parent(parent)
                        .interpreter(::plmnInterpreter)
                        .build(resources),
                accessTechnologyBuilder.build(resources)
            )
        }

        private fun hpplmnDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData)
                        .labelId(R.string.time_interval_label)
                        .parent(parent)
                        .interpreter(::hpplmnInterpreter)
                        .build(resources)
        )

        private fun acmMaxDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData)
                        .labelId(R.string.maximum_value_label)
                        .parent(parent)
                        .interpreter(::unsignedIntegerInterpreter)
                        .build(resources)
        )

        private fun ustDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.mapIndexed { byteIndex, byte ->
                ConstructedElement.Builder(byteArrayOf(byte))
                        .labelId(R.string.byte_number_label)
                        .labelArgs(byteIndex + 1)
                        .parent(parent)
                        .decoder { innerResources, byteData, byteParent ->
                            ustByteDecoder(innerResources, byteData, byteParent, byteIndex)
                        }
                        .dataComposer(::ustByteDataComposer)
                        .build(resources)
            }
        }

        private fun ustByteDataComposer(elements: List<Element>): ByteArray {
            val value = elements.take(8).foldIndexed(0) { bitIndex, acc, element ->
                if ((element.data.firstOrNull()?.toInt()?.and(0x01) ?: 0) == 1) {
                    acc or (1 shl bitIndex)
                } else {
                    acc
                }
            }
            return byteArrayOf(value.toByte())
        }

        private fun ustByteDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?,
            byteIndex: Int
        ): List<Element> {
            val byte = rawData.firstOrNull() ?: return emptyList()
            return (0 until 8).map { bitIndex ->
                val serviceNumber = byteIndex * 8 + bitIndex + 1
                val value = if (byte.toInt() and (1 shl bitIndex) != 0) 1 else 0
                PrimitiveElement.Builder(byteArrayOf(value.toByte()))
                        .labelId(R.string.service_number_label)
                        .labelArgs(serviceNumber, usimServiceName(resources, serviceNumber))
                        .parent(parent)
                        .interpreter(::usimServiceStateInterpreter)
                        .build(resources)
            }
        }

        private fun decodeSingleElementFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int,
            childLabelId: Int
        ): Element? {
            if (bytes.isEmpty()) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder { innerResources, rawData, parent ->
                        listOf(
                                PrimitiveElement.Builder(rawData)
                                        .labelId(childLabelId)
                                        .parent(parent)
                                        .build(innerResources)
                        )
                    }
                    .build(resources)
        }

        private fun decodeSingleElementFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int,
            childLabelId: Int,
            interpreter: (Resources, ByteArray) -> String
        ): Element? {
            if (bytes.isEmpty()) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder { innerResources, rawData, parent ->
                        listOf(
                                PrimitiveElement.Builder(rawData)
                                        .labelId(childLabelId)
                                        .parent(parent)
                                        .interpreter(interpreter)
                                        .build(innerResources)
                        )
                    }
                    .build(resources)
        }

        private fun decodeSingleElementFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int,
            childLabelId: Int,
            expectedLength: Int,
            interpreter: (Resources, ByteArray) -> String
        ): Element? {
            if (bytes.size != expectedLength) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder { innerResources, rawData, parent ->
                        listOf(
                                PrimitiveElement.Builder(rawData)
                                        .labelId(childLabelId)
                                        .parent(parent)
                                        .interpreter(interpreter)
                                        .build(innerResources)
                        )
                    }
                    .build(resources)
        }

        private fun spnDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                        .labelId(R.string.display_condition_label)
                        .parent(parent)
                        .interpreter(::spnDisplayConditionInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(1, SPN_LENGTH))
                        .labelId(R.string.service_provider_name_label)
                        .parent(parent)
                        .interpreter(::alphaIdentifierInterpreter)
                        .build(resources)
        )

        private fun puctDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 3))
                        .labelId(R.string.currency_code_label)
                        .parent(parent)
                        .interpreter(::alphaIdentifierInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(3, PUCT_LENGTH))
                        .labelId(R.string.price_per_unit_label)
                        .parent(parent)
                        .interpreter(::pricePerUnitInterpreter)
                        .build(resources)
        )

        private fun cbmiDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.asIterable().chunked(CBMI_ENTRY_LENGTH).map {
                PrimitiveElement.Builder(it.toByteArray())
                        .labelId(R.string.cb_message_identifier_label)
                        .parent(parent)
                        .interpreter(::cbMessageIdentifierInterpreter)
                        .build(resources)
            }
        }

        private fun accDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData)
                        .labelId(R.string.access_control_classes_label)
                        .parent(parent)
                        .interpreter(::accessControlClassesInterpreter)
                        .build(resources)
        )

        private fun fplmnDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.asIterable().chunked(FPLMN_ENTRY_LENGTH).mapIndexed { index, entry ->
                PrimitiveElement.Builder(entry.toByteArray())
                        .labelId(R.string.plmn_number_label)
                        .labelArgs(index + 1)
                        .parent(parent)
                        .interpreter(::plmnInterpreter)
                        .build(resources)
            }
        }

        private fun lociDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 4))
                        .labelId(R.string.tmsi_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(4, 9))
                        .labelId(R.string.location_area_information_label)
                        .parent(parent)
                        .interpreter(::locationAreaInformationInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(9, 10))
                        .labelId(R.string.rfu_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(10, LOCI_LENGTH))
                        .labelId(R.string.location_update_status_label)
                        .parent(parent)
                        .interpreter(::locationUpdateStatusInterpreter)
                        .build(resources)
        )

        private fun adDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val elements = mutableListOf(
                    PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                            .labelId(R.string.ue_operation_mode_label)
                            .parent(parent)
                            .interpreter(::ueOperationModeInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(1, 3))
                            .labelId(R.string.additional_information_label)
                            .parent(parent)
                            .interpreter(::additionalInformationInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(3, 4))
                            .labelId(R.string.length_of_mnc_in_imsi_label)
                            .parent(parent)
                            .interpreter(::mncLengthInterpreter)
                            .build(resources)
            )
            if (rawData.size > MIN_AD_LENGTH) {
                elements.add(
                        PrimitiveElement.Builder(rawData.copyOfRange(MIN_AD_LENGTH, rawData.size))
                                .labelId(R.string.rfu_label)
                                .parent(parent)
                                .build(resources)
                )
            }
            return elements
        }

        private fun decodeCbMessageIdentifierFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int
        ): Element? {
            if (bytes.isEmpty() || bytes.size % CBMI_ENTRY_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder(::cbmiDecoder)
                    .build(resources)
        }

        private fun eccDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, ECC_MIN_LENGTH))
                        .labelId(R.string.emergency_call_code_label)
                        .parent(parent)
                        .interpreter(::emergencyCallCodeInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(
                        rawData.copyOfRange(ECC_MIN_LENGTH, rawData.lastIndex))
                        .labelId(R.string.alpha_identifier_label)
                        .parent(parent)
                        .interpreter(::alphaIdentifierInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(rawData.lastIndex, rawData.size))
                        .labelId(R.string.service_category_label)
                        .parent(parent)
                        .build(resources)
        )

        private fun cbmirDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.asIterable().chunked(CBMIR_ENTRY_LENGTH).mapIndexed { index, entry ->
                ConstructedElement.Builder(entry.toByteArray())
                        .labelId(R.string.cb_message_identifier_range_label)
                        .labelArgs(index + 1)
                        .parent(parent)
                        .decoder { innerResources, entryData, entryParent ->
                            cbmirEntryDecoder(innerResources, entryData, entryParent)
                        }
                        .interpreter { _, _ -> (index + 1).toString() }
                        .build(resources)
            }
        }

        private fun cbmirEntryDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 2))
                        .labelId(R.string.lower_cb_message_identifier_label)
                        .parent(parent)
                        .interpreter(::cbMessageIdentifierInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(2, CBMIR_ENTRY_LENGTH))
                        .labelId(R.string.upper_cb_message_identifier_label)
                        .parent(parent)
                        .interpreter(::cbMessageIdentifierInterpreter)
                        .build(resources)
        )

        private fun pslociDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 4))
                        .labelId(R.string.p_tmsi_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(4, 7))
                        .labelId(R.string.p_tmsi_signature_value_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(7, 13))
                        .labelId(R.string.routing_area_information_label)
                        .parent(parent)
                        .interpreter(::routingAreaInformationInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(13, PSLOCI_LENGTH))
                        .labelId(R.string.routing_area_update_status_label)
                        .parent(parent)
                        .interpreter(::routingAreaUpdateStatusInterpreter)
                        .build(resources)
        )

        private fun decodeDiallingNumberFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int,
            ccpLabelId: Int,
            extensionLabelId: Int
        ): Element? {
            if (bytes.size <= DIALING_NUMBER_TRAILER_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder { innerResources, rawData, parent ->
                        diallingNumberDecoder(
                                innerResources,
                                rawData,
                                parent,
                                ccpLabelId,
                                extensionLabelId
                        )
                    }
                    .build(resources)
        }

        private fun decodeThreeByteCounter(
            resources: Resources,
            bytes: ByteArray,
            requiredLength: Int,
            rootLabelId: Int,
            valueLabelId: Int,
            interpreter: (Resources, ByteArray) -> String = ::unsignedIntegerInterpreter
        ): Element? {
            if (bytes.size != requiredLength) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder { innerResources, rawData, parent ->
                        listOf(
                                PrimitiveElement.Builder(rawData)
                                        .labelId(valueLabelId)
                                        .parent(parent)
                                        .interpreter(interpreter)
                                        .build(innerResources)
                        )
                    }
                    .build(resources)
        }

        private fun decodeCallInformation(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int,
            incoming: Boolean
        ): Element? {
            val trailerLength = if (incoming) ICI_TRAILER_LENGTH else OCI_TRAILER_LENGTH
            if (bytes.size < trailerLength) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder { innerResources, rawData, parent ->
                        callInformationDecoder(
                                innerResources,
                                rawData,
                                parent,
                                incoming
                        )
                    }
                    .build(resources)
        }

        private fun callInformationDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?,
            incoming: Boolean
        ): List<Element> {
            val trailerLength = if (incoming) ICI_TRAILER_LENGTH else OCI_TRAILER_LENGTH
            val alphaLength = rawData.size - trailerLength
            val elements = mutableListOf<Element>()
            elements += PrimitiveElement.Builder(rawData.copyOfRange(0, alphaLength))
                    .labelId(R.string.alpha_identifier_label)
                    .parent(parent)
                    .interpreter(::alphaIdentifierInterpreter)
                    .build(resources)
            elements += PrimitiveElement.Builder(rawData.copyOfRange(alphaLength, alphaLength + 1))
                    .labelId(R.string.bcd_number_length_label)
                    .parent(parent)
                    .interpreter(::unsignedIntegerInterpreter)
                    .build(resources)
            elements += PrimitiveElement.Builder(
                    rawData.copyOfRange(alphaLength + 1, alphaLength + 2))
                    .labelId(R.string.ton_npi_label)
                    .parent(parent)
                    .build(resources)
            elements += PrimitiveElement.Builder(
                    rawData.copyOfRange(alphaLength + 2, alphaLength + 12))
                    .labelId(R.string.dialling_number_label)
                    .parent(parent)
                    .interpreter(::swappedBcdStringInterpreter)
                    .build(resources)
            elements += PrimitiveElement.Builder(
                    rawData.copyOfRange(alphaLength + 12, alphaLength + 13))
                    .labelId(R.string.ccp2_record_identifier_label)
                    .parent(parent)
                    .build(resources)
            elements += PrimitiveElement.Builder(
                    rawData.copyOfRange(alphaLength + 13, alphaLength + 14))
                    .labelId(R.string.extension5_record_identifier_label)
                    .parent(parent)
                    .build(resources)
            elements += PrimitiveElement.Builder(
                    rawData.copyOfRange(alphaLength + 14, alphaLength + 21))
                    .labelId(R.string.call_date_time_label)
                    .parent(parent)
                    .interpreter(::callDateTimeInterpreter)
                    .build(resources)
            elements += PrimitiveElement.Builder(
                    rawData.copyOfRange(alphaLength + 21, alphaLength + 24))
                    .labelId(R.string.call_duration_label)
                    .parent(parent)
                    .interpreter(::secondsInterpreter)
                    .build(resources)
            var linkOffset = alphaLength + 24
            if (incoming) {
                elements += PrimitiveElement.Builder(
                        rawData.copyOfRange(linkOffset, linkOffset + 1))
                        .labelId(R.string.call_status_label)
                        .parent(parent)
                        .interpreter(::callStatusInterpreter)
                        .build(resources)
                linkOffset++
            }
            elements += PrimitiveElement.Builder(rawData.copyOfRange(linkOffset, linkOffset + 3))
                    .labelId(R.string.phone_book_link_label)
                    .parent(parent)
                    .interpreter(::phoneBookLinkInterpreter)
                    .build(resources)
            return elements
        }

        private fun ipsDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 2))
                        .labelId(R.string.pairing_status_label)
                        .parent(parent)
                        .interpreter(::pairingStatusInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(2, 3))
                        .labelId(R.string.pairing_device_record_label)
                        .parent(parent)
                        .interpreter(::recordIdentifierInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(3, IPS_LENGTH))
                        .labelId(R.string.rfu_label)
                        .parent(parent)
                        .build(resources)
        )

        private fun diallingNumberDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?,
            ccpLabelId: Int,
            extensionLabelId: Int
        ): List<Element> {
            val alphaLength = rawData.size - DIALING_NUMBER_TRAILER_LENGTH
            return listOf(
                    PrimitiveElement.Builder(rawData.copyOfRange(0, alphaLength))
                            .labelId(R.string.alpha_identifier_label)
                            .parent(parent)
                            .interpreter(::alphaIdentifierInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(alphaLength, alphaLength + 1))
                            .labelId(R.string.bcd_number_length_label)
                            .parent(parent)
                            .interpreter(::unsignedIntegerInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(alphaLength + 1, alphaLength + 2))
                            .labelId(R.string.ton_npi_label)
                            .parent(parent)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(alphaLength + 2, alphaLength + 12))
                            .labelId(R.string.dialling_number_label)
                            .parent(parent)
                            .interpreter(::swappedBcdStringInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(
                            rawData.copyOfRange(alphaLength + 12, alphaLength + 13))
                            .labelId(ccpLabelId)
                            .parent(parent)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(alphaLength + 13, rawData.size))
                            .labelId(extensionLabelId)
                            .parent(parent)
                            .build(resources)
            )
        }

        private fun smsDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                        .labelId(R.string.sms_status_label)
                        .parent(parent)
                        .interpreter(::smsStatusInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(1, SMS_LENGTH))
                        .labelId(R.string.sms_tpdu_label)
                        .parent(parent)
                        .build(resources)
        )

        private fun smspDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val alphaLength = rawData.size - SMSP_TRAILER_LENGTH
            return listOf(
                    PrimitiveElement.Builder(rawData.copyOfRange(0, alphaLength))
                            .labelId(R.string.alpha_identifier_label)
                            .parent(parent)
                            .interpreter(::alphaIdentifierInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(alphaLength, alphaLength + 1))
                            .labelId(R.string.sms_parameters_indicator_label)
                            .parent(parent)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(alphaLength + 1, alphaLength + 13))
                            .labelId(R.string.destination_address_label)
                            .parent(parent)
                            .build(resources),
                    PrimitiveElement.Builder(
                            rawData.copyOfRange(alphaLength + 13, alphaLength + 25))
                            .labelId(R.string.service_centre_address_label)
                            .parent(parent)
                            .build(resources),
                    PrimitiveElement.Builder(
                            rawData.copyOfRange(alphaLength + 25, alphaLength + 26))
                            .labelId(R.string.protocol_identifier_label)
                            .parent(parent)
                            .build(resources),
                    PrimitiveElement.Builder(
                            rawData.copyOfRange(alphaLength + 26, alphaLength + 27))
                            .labelId(R.string.data_coding_scheme_label)
                            .parent(parent)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(alphaLength + 27, rawData.size))
                            .labelId(R.string.validity_period_label)
                            .parent(parent)
                            .build(resources)
            )
        }

        private fun smssDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                        .labelId(R.string.message_reference_label)
                        .parent(parent)
                        .interpreter(::unsignedIntegerInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(1, SMSS_LENGTH))
                        .labelId(R.string.memory_capacity_exceeded_flag_label)
                        .parent(parent)
                        .interpreter(::memoryCapacityExceededInterpreter)
                        .build(resources)
        )

        private fun decodeExtensionRecordFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int
        ): Element? {
            if (bytes.size != EXTENSION_RECORD_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder(::extensionRecordDecoder)
                    .build(resources)
        }

        private fun extensionRecordDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                        .labelId(R.string.record_type_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(1, 12))
                        .labelId(R.string.extension_data_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(12, EXTENSION_RECORD_LENGTH))
                        .labelId(R.string.identifier_label)
                        .parent(parent)
                        .build(resources)
        )

        private fun smsrDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                        .labelId(R.string.sms_record_identifier_label)
                        .parent(parent)
                        .interpreter(::smsRecordIdentifierInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(1, rawData.size))
                        .labelId(R.string.sms_status_report_label)
                        .parent(parent)
                        .build(resources)
        )

        private fun ccp2Decoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData)
                        .labelId(R.string.bearer_capability_information_element_label)
                        .parent(parent)
                        .build(resources)
        )

        private fun emlppDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                        .labelId(R.string.priority_levels_label)
                        .parent(parent)
                        .interpreter(::emlppPriorityMaskInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(1, EMLPP_LENGTH))
                        .labelId(R.string.fast_call_setup_conditions_label)
                        .parent(parent)
                        .interpreter(::emlppPriorityMaskInterpreter)
                        .build(resources)
        )

        private fun bdnDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return diallingNumberDecoder(
                    resources,
                    rawData.copyOfRange(0, rawData.lastIndex),
                    parent,
                    R.string.ccp2_record_identifier_label,
                    R.string.extension4_record_identifier_label
            ) + PrimitiveElement.Builder(rawData.copyOfRange(rawData.lastIndex, rawData.size))
                    .labelId(R.string.comparison_method_pointer_label)
                    .parent(parent)
                    .interpreter(::recordIdentifierInterpreter)
                    .build(resources)
        }

        private fun cmiDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, rawData.lastIndex))
                        .labelId(R.string.alpha_identifier_label)
                        .parent(parent)
                        .interpreter(::alphaIdentifierInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(rawData.lastIndex, rawData.size))
                        .labelId(R.string.comparison_method_identifier_label)
                        .parent(parent)
                        .interpreter(::emptyOrUnsignedIntegerInterpreter)
                        .build(resources)
        )

        private fun estDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.mapIndexed { byteIndex, byte ->
                ConstructedElement.Builder(byteArrayOf(byte))
                        .labelId(R.string.byte_number_label)
                        .labelArgs(byteIndex + 1)
                        .parent(parent)
                        .decoder { innerResources, byteData, byteParent ->
                            estByteDecoder(innerResources, byteData, byteParent, byteIndex)
                        }
                        .dataComposer(::ustByteDataComposer)
                        .build(resources)
            }
        }

        private fun estByteDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?,
            byteIndex: Int
        ): List<Element> {
            val byte = rawData.firstOrNull() ?: return emptyList()
            return (0 until 8).map { bitIndex ->
                val serviceNumber = byteIndex * 8 + bitIndex + 1
                val value = if (byte.toInt() and (1 shl bitIndex) != 0) 1 else 0
                PrimitiveElement.Builder(byteArrayOf(value.toByte()))
                        .labelId(R.string.est_service_number_label)
                        .labelArgs(serviceNumber, estServiceName(resources, serviceNumber))
                        .parent(parent)
                        .interpreter(::activatedStateInterpreter)
                        .build(resources)
            }
        }

        private fun aclDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                        .labelId(R.string.number_of_apns_dnns_label)
                        .parent(parent)
                        .interpreter(::unsignedIntegerInterpreter)
                        .build(resources),
                tlvContainerElement(
                        resources,
                        rawData.copyOfRange(1, rawData.size),
                        parent,
                        R.string.apn_dnn_tlvs_label
                )
        )

        private fun dckDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                dckElement(resources, rawData, parent, 0,
                        R.string.network_depersonalisation_control_key_label),
                dckElement(resources, rawData, parent, 4,
                        R.string.network_subset_depersonalisation_control_key_label),
                dckElement(resources, rawData, parent, 8,
                        R.string.service_provider_depersonalisation_control_key_label),
                dckElement(resources, rawData, parent, 12,
                        R.string.corporate_depersonalisation_control_key_label)
        )

        private fun dckElement(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?,
            offset: Int,
            labelId: Int
        ): Element = PrimitiveElement.Builder(rawData.copyOfRange(offset, offset + 4))
                .labelId(labelId)
                .parent(parent)
                .interpreter(::nonSwappedBcdStringInterpreter)
                .build(resources)

        private fun cnlDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.asIterable().chunked(CNL_ENTRY_LENGTH).mapIndexed { index, entry ->
                ConstructedElement.Builder(entry.toByteArray())
                        .labelId(R.string.cooperative_network_entry_label)
                        .labelArgs(index + 1)
                        .parent(parent)
                        .decoder(::cnlEntryDecoder)
                        .interpreter { _, _ -> (index + 1).toString() }
                        .build(resources)
            }
        }

        private fun cnlEntryDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 3))
                        .labelId(R.string.plmn_label)
                        .parent(parent)
                        .interpreter(::plmnInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(3, 4))
                        .labelId(R.string.network_subset_digits_label)
                        .parent(parent)
                        .interpreter(::swappedBcdStringInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(4, 5))
                        .labelId(R.string.service_provider_digits_label)
                        .parent(parent)
                        .interpreter(::swappedBcdStringInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(5, CNL_ENTRY_LENGTH))
                        .labelId(R.string.corporate_digits_label)
                        .parent(parent)
                        .interpreter(::swappedBcdStringInterpreter)
                        .build(resources)
        )

        private fun startHfnDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 3))
                        .labelId(R.string.start_cs_label)
                        .parent(parent)
                        .interpreter(::unsignedIntegerInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(3, START_HFN_LENGTH))
                        .labelId(R.string.start_ps_label)
                        .parent(parent)
                        .interpreter(::unsignedIntegerInterpreter)
                        .build(resources)
        )

        private fun decodePlmnWActFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int,
            minLength: Int
        ): Element? {
            if (bytes.size < minLength || bytes.size % PLMN_W_ACT_ENTRY_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder(::plmnWActDecoder)
                    .build(resources)
        }

        private fun decodeTlvFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int,
            minLength: Int
        ): Element? {
            return decodeTlvFile(resources, bytes, rootLabelId, minLength, ::genericTlvDecoder)
        }

        private fun decodeTlvFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int,
            minLength: Int,
            decoder: (Resources, ByteArray, Element?) -> List<Element>
        ): Element? {
            if (bytes.size < minLength) return null
            if (BerTlv.listFrom(bytes).isEmpty()) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder(decoder)
                    .build(resources)
        }

        private fun genericTlvDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return BerTlv.listFrom(rawData).map {
                BerTlvElement.Builder(it)
                        .parent(parent)
                        .decoder(::genericBerTlvDecoder)
                        .build(resources)
            }
        }

        private fun genericBerTlvDecoder(
            resources: Resources,
            tlvs: List<Tlv>,
            parent: Element?
        ): List<Element> {
            return tlvs.map {
                BerTlvElement.Builder(it)
                        .parent(parent)
                        .decoder(::genericBerTlvDecoder)
                        .build(resources)
            }
        }

        private fun tlvContainerElement(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?,
            labelId: Int
        ): Element {
            return ConstructedElement.Builder(rawData)
                    .labelId(labelId)
                    .parent(parent)
                    .decoder(::genericTlvDecoder)
                    .interpreter { _, _ -> "" }
                    .build(resources)
        }

        private fun netparDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return BerTlv.listFrom(rawData).map { tlv ->
                val builder = BerTlvElement.Builder(tlv).parent(parent)
                when (tlv.tag) {
                    TAG_NETPAR_GSM_CELL_INFORMATION -> builder
                            .labelId(R.string.gsm_cell_information_label)
                            .decoder(::netparCellInformationDecoder)
                    TAG_NETPAR_FDD_CELL_INFORMATION -> builder
                            .labelId(R.string.fdd_cell_information_label)
                            .decoder(::netparCellInformationDecoder)
                    TAG_NETPAR_TDD_CELL_INFORMATION -> builder
                            .labelId(R.string.tdd_cell_information_label)
                            .decoder(::netparCellInformationDecoder)
                    else -> builder
                            .decoder(::genericBerTlvDecoder)
                }.build(resources)
            }
        }

        private fun netparCellInformationDecoder(
            resources: Resources,
            tlvs: List<Tlv>,
            parent: Element?
        ): List<Element> {
            return tlvs.map { tlv ->
                val parentTag = parentTag(parent)
                val builder = BerTlvElement.Builder(tlv).parent(parent)
                when (tlv.tag) {
                    TAG_NETPAR_INFORMATION_1 -> builder
                            .labelId(netparInformation1Label(parentTag))
                    TAG_NETPAR_INFORMATION_2 -> builder
                            .labelId(netparInformation2Label(parentTag))
                    TAG_NETPAR_CORE_NETWORK_BEARER -> builder
                            .labelId(R.string.core_network_and_bearer_label)
                    else -> builder
                            .decoder(::genericBerTlvDecoder)
                }.build(resources)
            }
        }

        private fun parentTag(parent: Element?): Int? {
            return (parent as? BerTlvElement)?.tag
        }

        private fun netparInformation1Label(parentTag: Int?): Int = when (parentTag) {
            TAG_NETPAR_GSM_CELL_INFORMATION -> R.string.current_camped_bcch_frequency_label
            TAG_NETPAR_FDD_CELL_INFORMATION -> R.string.intra_frequency_information_label
            TAG_NETPAR_TDD_CELL_INFORMATION -> R.string.intra_frequency_information_label
            else -> R.string.unknown_label
        }

        private fun netparInformation2Label(parentTag: Int?): Int = when (parentTag) {
            TAG_NETPAR_GSM_CELL_INFORMATION -> R.string.neighbour_bcch_frequencies_label
            TAG_NETPAR_FDD_CELL_INFORMATION -> R.string.inter_frequency_information_label
            TAG_NETPAR_TDD_CELL_INFORMATION -> R.string.inter_frequency_information_label
            else -> R.string.unknown_label
        }

        private fun pnnDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return BerTlv.listFrom(rawData).map { tlv ->
                val builder = BerTlvElement.Builder(tlv).parent(parent)
                when (tlv.tag) {
                    TAG_PNN_FULL_NAME -> builder
                            .labelId(R.string.full_name_for_network_label)
                            .interpreter(::networkNameInterpreter)
                    TAG_PNN_SHORT_NAME -> builder
                            .labelId(R.string.short_name_for_network_label)
                            .interpreter(::networkNameInterpreter)
                    TAG_PNN_ADDITIONAL_INFORMATION -> builder
                            .labelId(R.string.plmn_additional_information_label)
                            .interpreter(PrimitiveElement::defaultStringInterpreter)
                    else -> builder
                }.build(resources)
            }
        }

        private fun spdiDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return BerTlv.listFrom(rawData).map { tlv ->
                val builder = BerTlvElement.Builder(tlv).parent(parent)
                when (tlv.tag) {
                    TAG_SPDI_TEMPLATE -> builder
                            .labelId(R.string.service_provider_display_information_label)
                            .decoder(::spdiTemplateDecoder)
                    else -> builder
                            .decoder(::genericBerTlvDecoder)
                }.build(resources)
            }
        }

        private fun spdiTemplateDecoder(
            resources: Resources,
            tlvs: List<Tlv>,
            parent: Element?
        ): List<Element> {
            return tlvs.map { tlv ->
                val builder = BerTlvElement.Builder(tlv).parent(parent)
                when (tlv.tag) {
                    TAG_SPDI_PLMN_LIST -> builder
                            .labelId(R.string.service_provider_plmn_list_label)
                            .separator(::spdiPlmnListSeparator)
                    else -> builder
                            .decoder(::genericBerTlvDecoder)
                }.build(resources)
            }
        }

        private fun spdiPlmnListSeparator(
            resources: Resources,
            value: ByteArray,
            parent: Element?
        ): List<Element> {
            val trimmed = value.dropLastWhile { it.toInt() and 0xFF == 0xFF }.toByteArray()
            if (trimmed.isEmpty()) return listOf()

            val plmns = trimmed.asIterable().chunked(FPLMN_ENTRY_LENGTH).filter {
                it.size == FPLMN_ENTRY_LENGTH
            }
            return plmns.mapIndexed { index, entry ->
                PrimitiveElement.Builder(entry.toByteArray())
                        .labelId(R.string.service_provider_plmn_label)
                        .labelArgs(index + 1)
                        .parent(parent)
                        .interpreter(::plmnInterpreter)
                        .build(resources)
            }
        }

        private fun oplDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.asIterable().chunked(OPL_ENTRY_LENGTH).mapIndexed { index, entry ->
                ConstructedElement.Builder(entry.toByteArray())
                        .labelId(R.string.operator_plmn_list_entry_label)
                        .labelArgs(index + 1)
                        .parent(parent)
                        .decoder(::oplEntryDecoder)
                        .interpreter { _, _ -> (index + 1).toString() }
                        .build(resources)
            }
        }

        private fun oplEntryDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 3))
                        .labelId(R.string.plmn_label)
                        .parent(parent)
                        .interpreter(::plmnInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(3, 5))
                        .labelId(R.string.start_lac_tac_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(5, 7))
                        .labelId(R.string.end_lac_tac_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(7, OPL_ENTRY_LENGTH))
                        .labelId(R.string.plmn_network_name_record_identifier_label)
                        .parent(parent)
                        .interpreter(::recordIdentifierInterpreter)
                        .build(resources)
        )

        private fun mbiDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val labels = listOf(
                    R.string.mailbox_identifier_voicemail_label,
                    R.string.mailbox_identifier_fax_label,
                    R.string.mailbox_identifier_email_label,
                    R.string.mailbox_identifier_other_label,
                    R.string.mailbox_identifier_videomail_label
            )
            return rawData.mapIndexed { index, byte ->
                PrimitiveElement.Builder(byteArrayOf(byte))
                        .labelId(labels.getOrElse(index) { R.string.mailbox_identifier_label })
                        .parent(parent)
                        .interpreter(::recordIdentifierInterpreter)
                        .build(resources)
            }
        }

        private fun mwisDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val labels = listOf(
                    R.string.number_of_voicemail_messages_waiting_label,
                    R.string.number_of_fax_messages_waiting_label,
                    R.string.number_of_email_messages_waiting_label,
                    R.string.number_of_other_messages_waiting_label,
                    R.string.number_of_videomail_messages_waiting_label
            )
            return listOf(
                    PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                            .labelId(R.string.message_waiting_indicator_status_label)
                            .parent(parent)
                            .interpreter(::messageWaitingStatusInterpreter)
                            .build(resources)
            ) + rawData.copyOfRange(1, rawData.size).mapIndexed { index, byte ->
                PrimitiveElement.Builder(byteArrayOf(byte))
                        .labelId(labels.getOrElse(index) { R.string.message_count_label })
                        .parent(parent)
                        .interpreter(::unsignedIntegerInterpreter)
                        .build(resources)
            }
        }

        private fun cfisDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                        .labelId(R.string.msp_number_label)
                        .parent(parent)
                        .interpreter(::unsignedIntegerInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(1, 2))
                        .labelId(R.string.cfu_indicator_status_label)
                        .parent(parent)
                        .interpreter(::cfuIndicatorStatusInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(2, 3))
                        .labelId(R.string.bcd_number_length_label)
                        .parent(parent)
                        .interpreter(::unsignedIntegerInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(3, 4))
                        .labelId(R.string.ton_npi_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(4, 14))
                        .labelId(R.string.dialling_number_label)
                        .parent(parent)
                        .interpreter(::swappedBcdStringInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(14, 15))
                        .labelId(R.string.ccp2_record_identifier_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(15, CFIS_LENGTH))
                        .labelId(R.string.extension7_record_identifier_label)
                        .parent(parent)
                        .build(resources)
        )

        private fun mmsnDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 2))
                        .labelId(R.string.mms_status_label)
                        .parent(parent)
                        .interpreter(::mmsStatusInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(2, 3))
                        .labelId(R.string.mms_implementation_label)
                        .parent(parent)
                        .interpreter(::mmsImplementationInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(3, rawData.lastIndex))
                        .labelId(R.string.mms_notification_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(rawData.lastIndex, rawData.size))
                        .labelId(R.string.extension_record_identifier_label)
                        .parent(parent)
                        .interpreter(::recordIdentifierInterpreter)
                        .build(resources)
        )

        private fun ext8Decoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                        .labelId(R.string.record_type_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(1, rawData.lastIndex))
                        .labelId(R.string.extension_data_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(rawData.lastIndex, rawData.size))
                        .labelId(R.string.identifier_label)
                        .parent(parent)
                        .interpreter(::recordIdentifierInterpreter)
                        .build(resources)
        )

        private fun decodeMmsConnectivityFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int
        ): Element? {
            if (bytes.isEmpty()) return null
            if (BerTlv.listFrom(bytes).none {
                        it.tag == TAG_MMS_CONNECTIVITY_PARAMETERS
                    }) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder(::mmsConnectivityDecoder)
                    .build(resources)
        }

        private fun mmsConnectivityDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = BerTlv.listFrom(rawData).map { tlv ->
            val builder = BerTlvElement.Builder(tlv).parent(parent)
            when (tlv.tag) {
                TAG_MMS_CONNECTIVITY_PARAMETERS -> builder
                        .labelId(R.string.mms_connectivity_parameters_label)
                        .decoder(::mmsConnectivityParameterDecoder)
                else -> builder.decoder(::genericBerTlvDecoder)
            }.build(resources)
        }

        private fun mmsConnectivityParameterDecoder(
            resources: Resources,
            tlvs: List<Tlv>,
            parent: Element?
        ): List<Element> = tlvs.map { tlv ->
            val builder = BerTlvElement.Builder(tlv).parent(parent)
            when (tlv.tag) {
                TAG_MMS_IMPLEMENTATION -> builder
                        .labelId(R.string.mms_implementation_label)
                        .interpreter(::mmsImplementationInterpreter)
                TAG_MMS_RELAY_SERVER -> builder.labelId(R.string.mms_relay_server_label)
                TAG_INTERFACE_CORE_NETWORK_BEARER -> builder
                        .labelId(R.string.interface_to_core_network_bearer_label)
                TAG_GATEWAY -> builder.labelId(R.string.gateway_label)
                TAG_MMS_AUTHENTICATION_MECHANISM -> builder
                        .labelId(R.string.mms_authentication_mechanism_label)
                TAG_MMS_AUTHENTICATION_USER_NAME -> builder
                        .labelId(R.string.mms_authentication_user_name_label)
                        .interpreter(PrimitiveElement::defaultStringInterpreter)
                else -> builder.decoder(::genericBerTlvDecoder)
            }.build(resources)
        }

        private fun mmsupDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = BerTlv.listFrom(rawData).map { tlv ->
            val builder = BerTlvElement.Builder(tlv).parent(parent)
            when (tlv.tag) {
                TAG_MMS_USER_PREFERENCES -> builder
                        .labelId(R.string.mms_user_preferences_label)
                        .decoder(::mmsupPreferenceDecoder)
                else -> builder.decoder(::genericBerTlvDecoder)
            }.build(resources)
        }

        private fun mmsupPreferenceDecoder(
            resources: Resources,
            tlvs: List<Tlv>,
            parent: Element?
        ): List<Element> = tlvs.map { tlv ->
            val builder = BerTlvElement.Builder(tlv).parent(parent)
            when (tlv.tag) {
                TAG_MMS_IMPLEMENTATION -> builder
                        .labelId(R.string.mms_implementation_label)
                        .interpreter(::mmsImplementationInterpreter)
                TAG_MMS_USER_PROFILE_NAME -> builder
                        .labelId(R.string.mms_user_preference_profile_name_label)
                        .interpreter(::alphaIdentifierInterpreter)
                TAG_MMS_USER_PREFERENCE_INFORMATION -> builder
                        .labelId(R.string.mms_user_preference_information_label)
                else -> builder.decoder(::genericBerTlvDecoder)
            }.build(resources)
        }

        private fun niaDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                        .labelId(R.string.alerting_category_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(1, rawData.size))
                        .labelId(R.string.informative_text_label)
                        .parent(parent)
                        .interpreter(::alphaIdentifierInterpreter)
                        .build(resources)
        )

        private fun decodeGroupIdFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int
        ): Element? {
            if (bytes.isEmpty() || bytes.size % GROUP_ID_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder(::groupIdDecoder)
                    .build(resources)
        }

        private fun groupIdDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = rawData.asIterable().chunked(GROUP_ID_LENGTH).mapIndexed {
            index, entry ->
                PrimitiveElement.Builder(entry.toByteArray())
                        .labelId(R.string.group_id_label)
                        .labelArgs(index + 1)
                        .parent(parent)
                        .interpreter(::swappedBcdStringInterpreter)
                        .build(resources)
        }

        private fun decodeGroupStatusFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int
        ): Element? {
            if (bytes.size != GROUP_STATUS_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder(::groupStatusDecoder)
                    .build(resources)
        }

        private fun groupStatusDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.flatMapIndexed { byteIndex, byte ->
                (0 until 8).mapNotNull { bitIndex ->
                    val groupNumber = byteIndex * 8 + bitIndex + 1
                    if (groupNumber > 50) return@mapNotNull null
                    val value = if (byte.toInt() and (1 shl bitIndex) != 0) 1 else 0
                    PrimitiveElement.Builder(byteArrayOf(value.toByte()))
                            .labelId(R.string.group_activation_status_label)
                            .labelArgs(groupNumber)
                            .parent(parent)
                            .interpreter(::activatedStateInterpreter)
                            .build(resources)
                }
            }
        }

        private fun decodeCipheringAlgorithmFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int
        ): Element? {
            if (bytes.isEmpty() ||
                    bytes.size % CIPHERING_ALGORITHM_PAIR_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder(::cipheringAlgorithmDecoder)
                    .build(resources)
        }

        private fun cipheringAlgorithmDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = rawData.mapIndexed { index, byte ->
            PrimitiveElement.Builder(byteArrayOf(byte))
                    .labelId(R.string.group_ciphering_algorithm_label)
                    .labelArgs(index / 2 + 1, index % 2 + 1)
                    .parent(parent)
                    .interpreter(::cipheringAlgorithmInterpreter)
                    .build(resources)
        }

        private fun gbabpDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val randLength = rawData[0].toInt() and 0xFF
            if (rawData.size < randLength + 3) return emptyList()
            val bTidLengthOffset = randLength + 1
            val bTidLength = rawData[bTidLengthOffset].toInt() and 0xFF
            if (rawData.size < randLength + bTidLength + 3) return emptyList()
            val lifetimeLengthOffset = randLength + bTidLength + 2
            val lifetimeLength = rawData[lifetimeLengthOffset].toInt() and 0xFF
            if (rawData.size != randLength + bTidLength + lifetimeLength + 3) return emptyList()

            return listOf(
                    PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                            .labelId(R.string.length_of_rand_label)
                            .parent(parent)
                            .interpreter(::unsignedIntegerInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(1, bTidLengthOffset))
                            .labelId(R.string.rand_label)
                            .parent(parent)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(bTidLengthOffset,
                            bTidLengthOffset + 1))
                            .labelId(R.string.length_of_b_tid_label)
                            .parent(parent)
                            .interpreter(::unsignedIntegerInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(bTidLengthOffset + 1,
                            lifetimeLengthOffset))
                            .labelId(R.string.b_tid_label)
                            .parent(parent)
                            .interpreter(PrimitiveElement::defaultStringInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(lifetimeLengthOffset,
                            lifetimeLengthOffset + 1))
                            .labelId(R.string.length_of_key_lifetime_label)
                            .parent(parent)
                            .interpreter(::unsignedIntegerInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(lifetimeLengthOffset + 1,
                            rawData.size))
                            .labelId(R.string.key_lifetime_label)
                            .parent(parent)
                            .interpreter(PrimitiveElement::defaultStringInterpreter)
                            .build(resources)
            )
        }

        private fun mskDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val elements = mutableListOf<Element>()
            elements.add(PrimitiveElement.Builder(rawData.copyOfRange(0, 16))
                    .labelId(R.string.key_domain_id_label)
                    .parent(parent)
                    .build(resources))
            elements.add(PrimitiveElement.Builder(rawData.copyOfRange(16, 17))
                    .labelId(R.string.number_of_stored_msk_ids_label)
                    .parent(parent)
                    .interpreter(::unsignedIntegerInterpreter)
                    .build(resources))
            elements.add(PrimitiveElement.Builder(rawData.copyOfRange(17, 20))
                    .labelId(R.string.rfu_label)
                    .parent(parent)
                    .build(resources))
            rawData.copyOfRange(20, rawData.size).asIterable()
                    .chunked(MSK_TRAILING_ENTRY_LENGTH)
                    .forEachIndexed { index, entry ->
                        val entryData = entry.toByteArray()
                        elements.add(PrimitiveElement.Builder(entryData.copyOfRange(0, 4))
                                .labelId(R.string.msk_id_label)
                                .labelArgs(index + 1)
                                .parent(parent)
                                .build(resources))
                        elements.add(PrimitiveElement.Builder(entryData.copyOfRange(4, 8))
                                .labelId(R.string.time_stamp_counter_label)
                                .labelArgs(index + 1)
                                .parent(parent)
                                .build(resources))
            }
            return elements
        }

        private fun mukDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = BerTlv.listFrom(rawData).map { tlv ->
            val builder = BerTlvElement.Builder(tlv).parent(parent)
            when (tlv.tag) {
                TAG_MUK_ID -> builder
                        .labelId(R.string.muk_id_label)
                        .decoder(::mukIdDecoder)
                TAG_B_TID -> builder.labelId(R.string.time_stamp_counter_label)
                        .labelArgs(1)
                else -> builder.decoder(::genericBerTlvDecoder)
            }.build(resources)
        }

        private fun mukIdDecoder(
            resources: Resources,
            tlvs: List<Tlv>,
            parent: Element?
        ): List<Element> = tlvs.map { tlv ->
            val builder = BerTlvElement.Builder(tlv).parent(parent)
            when (tlv.tag) {
                TAG_MUK_IDR -> builder.labelId(R.string.muk_idr_label)
                TAG_MUK_IDI -> builder.labelId(R.string.muk_idi_label)
                else -> builder.decoder(::genericBerTlvDecoder)
            }.build(resources)
        }

        private fun gbanlDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = BerTlv.listFrom(rawData).map { tlv ->
            val builder = BerTlvElement.Builder(tlv).parent(parent)
            when (tlv.tag) {
                TAG_NAF_ID -> builder.labelId(R.string.naf_id_label)
                TAG_B_TID -> builder
                        .labelId(R.string.b_tid_label)
                        .interpreter(PrimitiveElement::defaultStringInterpreter)
                else -> builder.decoder(::genericBerTlvDecoder)
            }.build(resources)
        }

        private fun ehplmnDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.asIterable().chunked(EHPLMN_ENTRY_LENGTH).mapIndexed { index, entry ->
                PrimitiveElement.Builder(entry.toByteArray())
                        .labelId(R.string.ehplmn_label)
                        .labelArgs(index + 1)
                        .parent(parent)
                        .interpreter(::plmnInterpreter)
                        .build(resources)
            }
        }

        private fun nafkcaDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = BerTlv.listFrom(rawData).map { tlv ->
            val builder = BerTlvElement.Builder(tlv).parent(parent)
            when (tlv.tag) {
                TAG_NAF_KEY_CENTRE_ADDRESS -> builder
                        .labelId(R.string.naf_key_centre_address_label)
                        .interpreter(::utf8StringInterpreter)
                else -> builder.decoder(::genericBerTlvDecoder)
            }.build(resources)
        }

        private fun iconTlvDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = BerTlv.listFrom(rawData).map { tlv ->
            BerTlvElement.Builder(tlv)
                    .labelId(R.string.icon_label)
                    .parent(parent)
                    .separator(::iconSeparator)
                    .build(resources)
        }

        private fun iconSeparator(
            resources: Resources,
            value: ByteArray,
            parent: Element?
        ): List<Element> {
            if (value.isEmpty()) return emptyList()
            return listOf(
                    PrimitiveElement.Builder(value.copyOfRange(0, 1))
                            .labelId(R.string.icon_qualifier_label)
                            .parent(parent)
                            .build(resources),
                    PrimitiveElement.Builder(value.copyOfRange(1, value.size))
                            .labelId(R.string.icon_link_label)
                            .parent(parent)
                            .interpreter(PrimitiveElement::defaultStringInterpreter)
                            .build(resources)
            )
        }

        private fun ncpIpDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = BerTlv.listFrom(rawData).map { tlv ->
            val builder = BerTlvElement.Builder(tlv).parent(parent)
            when (tlv.tag) {
                TAG_DATA_DESTINATION_ADDRESS_RANGE -> builder
                        .labelId(R.string.data_destination_address_range_label)
                        .separator(::dataDestinationAddressRangeSeparator)
                TAG_ACCESS_POINT_NAME -> builder.labelId(R.string.access_point_name_label)
                TAG_LOGIN -> builder
                        .labelId(R.string.login_label)
                        .interpreter(PrimitiveElement::defaultStringInterpreter)
                TAG_PASSWORD -> builder
                        .labelId(R.string.password_label)
                        .interpreter(PrimitiveElement::defaultStringInterpreter)
                TAG_BEARER_DESCRIPTION -> builder.labelId(R.string.bearer_description_label)
                else -> builder.decoder(::genericBerTlvDecoder)
            }.build(resources)
        }

        private fun dataDestinationAddressRangeSeparator(
            resources: Resources,
            value: ByteArray,
            parent: Element?
        ): List<Element> {
            if (value.size < 2) return emptyList()
            return listOf(
                    PrimitiveElement.Builder(value.copyOfRange(0, 1))
                            .labelId(R.string.type_of_address_label)
                            .parent(parent)
                            .interpreter(::typeOfAddressInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(value.copyOfRange(1, 2))
                            .labelId(R.string.prefix_length_label)
                            .parent(parent)
                            .interpreter(::unsignedIntegerInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(value.copyOfRange(2, value.size))
                            .labelId(R.string.prefix_label)
                            .parent(parent)
                            .build(resources)
            )
        }

        private fun epslociDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 12))
                        .labelId(R.string.guti_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(12, 17))
                        .labelId(R.string.last_visited_registered_tai_label)
                        .parent(parent)
                        .interpreter(::trackingAreaIdentityInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(17, EPSLOCI_LENGTH))
                        .labelId(R.string.eps_update_status_label)
                        .parent(parent)
                        .interpreter(::epsUpdateStatusInterpreter)
                        .build(resources)
        )

        private fun epsnscDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = BerTlv.listFrom(rawData).map { tlv ->
            val builder = BerTlvElement.Builder(tlv).parent(parent)
            when (tlv.tag) {
                TAG_EPS_NAS_SECURITY_CONTEXT -> builder
                        .labelId(R.string.eps_nas_security_context_label)
                        .decoder(::epsNasSecurityContextDecoder)
                else -> builder.decoder(::genericBerTlvDecoder)
            }.build(resources)
        }

        private fun epsNasSecurityContextDecoder(
            resources: Resources,
            tlvs: List<Tlv>,
            parent: Element?
        ): List<Element> = tlvs.map { tlv ->
            val builder = BerTlvElement.Builder(tlv).parent(parent)
            when (tlv.tag) {
                TAG_KSIASME -> builder
                        .labelId(R.string.key_set_identifier_ksiasme_label)
                        .interpreter(::keySetIdentifierInterpreter)
                TAG_KASME -> builder.labelId(R.string.asme_key_label)
                TAG_UPLINK_NAS_COUNT -> builder
                        .labelId(R.string.uplink_nas_count_label)
                        .interpreter(::unsignedIntegerInterpreter)
                TAG_DOWNLINK_NAS_COUNT -> builder
                        .labelId(R.string.downlink_nas_count_label)
                        .interpreter(::unsignedIntegerInterpreter)
                TAG_NAS_SECURITY_ALGORITHMS -> builder
                        .labelId(R.string.nas_security_algorithms_label)
                else -> builder.decoder(::genericBerTlvDecoder)
            }.build(resources)
        }

        private fun nasconfigDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = BerTlv.listFrom(rawData).map { tlv ->
            val builder = BerTlvElement.Builder(tlv).parent(parent)
            when (tlv.tag) {
                in 0x80..0x9B -> builder
                        .labelId(nasconfigTagLabel(tlv.tag))
                        .interpreter(::enabledInterpreter)
                else -> builder.decoder(::genericBerTlvDecoder)
            }.build(resources)
        }

        private fun nasconfigTagLabel(tag: Int): Int = when (tag) {
            0x80 -> R.string.nas_signalling_priority_config_label
            0x81 -> R.string.nmo_i_behaviour_label
            0x82 -> R.string.attach_with_imsi_label
            0x83 -> R.string.minimum_periodic_search_timer_label
            0x84 -> R.string.extended_access_barring_label
            0x85 -> R.string.timer_t3245_behaviour_label
            0x86 -> R.string.override_nas_signalling_low_priority_label
            0x87 -> R.string.override_extended_access_barring_label
            0x88 -> R.string.fast_first_higher_priority_plmn_search_label
            0x89 -> R.string.eab_override_other_rats_label
            0x8A -> R.string.non_ip_data_delivery_mechanisms_label
            0x8B -> R.string.eab_override_ciot_eps_optimizations_label
            0x8C -> R.string.unavailability_period_support_label
            0x8D -> R.string.release_assistance_indication_support_label
            0x8E -> R.string.service_gap_control_label
            0x8F -> R.string.control_plane_ciot_eps_optimization_label
            0x90 -> R.string.user_plane_ciot_eps_optimization_label
            0x91 -> R.string.sor_cmci_support_label
            0x92 -> R.string.sor_transparent_container_support_label
            0x93 -> R.string.paging_restriction_support_label
            0x94 -> R.string.n3_data_delivery_support_label
            0x95 -> R.string.aerial_ue_subscription_information_label
            0x96 -> R.string.disaster_roaming_wait_range_label
            0x97 -> R.string.disaster_return_wait_range_label
            0x98 -> R.string.extended_rejected_nssai_indication_support_label
            0x99 -> R.string.extended_n3_data_delivery_support_label
            0x9A -> R.string.musim_assistance_support_label
            0x9B -> R.string.musim_paging_restriction_support_label
            else -> R.string.unknown_label
        }

        private fun decodeSingleStringTlvFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int,
            valueLabelId: Int
        ): Element? {
            if (bytes.isEmpty()) return null
            if (BerTlv.listFrom(bytes).none { it.tag == TAG_URI }) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder { innerResources, rawData, parent ->
                        stringTlvDecoder(innerResources, rawData, parent, valueLabelId)
                    }
                    .build(resources)
        }

        private fun stringTlvDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?,
            valueLabelId: Int
        ): List<Element> = BerTlv.listFrom(rawData).map { tlv ->
            val builder = BerTlvElement.Builder(tlv).parent(parent)
            when (tlv.tag) {
                TAG_URI -> builder
                        .labelId(valueLabelId)
                        .interpreter(::utf8StringInterpreter)
                else -> builder.decoder(::genericBerTlvDecoder)
            }.build(resources)
        }

        private fun decodeUriRecordFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int
        ): Element? {
            if (bytes.isEmpty()) return null
            val uriLength = firstBerTlvByteLength(bytes, TAG_URI) ?: return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder { innerResources, rawData, parent ->
                        uriRecordDecoder(innerResources, rawData, parent, uriLength)
                    }
                    .build(resources)
        }

        private fun firstBerTlvByteLength(rawData: ByteArray, expectedTag: Int): Int? {
            if (rawData.size < 2) return null
            if ((rawData[0].toInt() and 0xFF) != expectedTag) return null

            var lengthOffset = 1
            var length = rawData[lengthOffset++].toInt() and 0xFF
            if (length > 0x7F) {
                val lengthBytes = length and 0x7F
                if (lengthBytes !in 1..3 || rawData.size < lengthOffset + lengthBytes) {
                    return null
                }
                length = rawData.copyOfRange(lengthOffset, lengthOffset + lengthBytes)
                        .fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) }
                lengthOffset += lengthBytes
            }

            val totalLength = lengthOffset + length
            return if (rawData.size >= totalLength) totalLength else null
        }

        private fun uriRecordDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?,
            uriLength: Int
        ): List<Element> {
            val uriPart = rawData.copyOfRange(0, uriLength)
            val alphaPart = rawData.copyOfRange(uriLength, rawData.size)
            return listOf(
                    ConstructedElement.Builder(uriPart)
                            .labelId(R.string.uri_record_label)
                            .parent(parent)
                            .decoder { innerResources, entryData, entryParent ->
                                stringTlvDecoder(innerResources, entryData, entryParent,
                                        R.string.uri_label)
                            }
                            .interpreter { _, _ -> "" }
                            .build(resources),
                    PrimitiveElement.Builder(alphaPart)
                            .labelId(R.string.alpha_identifier_label)
                            .parent(parent)
                            .interpreter(::alphaIdentifierInterpreter)
                            .build(resources)
            )
        }

        private fun pwsDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val elements = mutableListOf<Element>()
            elements += PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                    .labelId(R.string.pws_configuration_label)
                    .parent(parent)
                    .interpreter(::pwsConfigurationInterpreter)
                    .build(resources)
            if (rawData.size > 1) {
                elements += PrimitiveElement.Builder(rawData.copyOfRange(1, rawData.size))
                        .labelId(R.string.rfu_label)
                        .parent(parent)
                        .build(resources)
            }
            return elements
        }

        private fun ialDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.asIterable().chunked(IAL_ENTRY_LENGTH).mapIndexed { index, entry ->
                PrimitiveElement.Builder(entry.toByteArray())
                        .labelId(R.string.imeisv_label)
                        .labelArgs(index + 1)
                        .parent(parent)
                        .interpreter(::swappedBcdStringInterpreter)
                        .build(resources)
            }
        }

        private fun decodeEpdgSelectionFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int
        ): Element? {
            if (bytes.isEmpty() || bytes.size % EPDG_SELECTION_ENTRY_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder(::epdgSelectionDecoder)
                    .build(resources)
        }

        private fun epdgSelectionDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = rawData.asIterable().chunked(EPDG_SELECTION_ENTRY_LENGTH).mapIndexed {
            index, entry ->
                ConstructedElement.Builder(entry.toByteArray())
                        .labelId(R.string.epdg_selection_entry_label)
                        .labelArgs(index + 1)
                        .parent(parent)
                        .decoder(::epdgSelectionEntryDecoder)
                        .interpreter { _, _ -> (index + 1).toString() }
                        .build(resources)
        }

        private fun epdgSelectionEntryDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 3))
                        .labelId(R.string.plmn_label)
                        .parent(parent)
                        .interpreter(::plmnInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(3, 5))
                        .labelId(R.string.epdg_identifier_priority_label)
                        .parent(parent)
                        .interpreter(::unsignedIntegerInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(5, 6))
                        .labelId(R.string.epdg_fqdn_format_label)
                        .parent(parent)
                        .interpreter(::epdgFqdnFormatInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(6, EPDG_SELECTION_ENTRY_LENGTH))
                        .labelId(R.string.epdg_identifier_configuration_label)
                        .parent(parent)
                        .interpreter(::recordIdentifierInterpreter)
                        .build(resources)
        )

        private fun decodeEpdgIdentifierFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int
        ): Element? {
            if (bytes.isEmpty()) return null
            if (BerTlv.listFrom(bytes).none { it.tag == TAG_EPDG_IDENTIFIER }) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder(::epdgIdentifierDecoder)
                    .build(resources)
        }

        private fun epdgIdentifierDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = BerTlv.listFrom(rawData).map { tlv ->
            val builder = BerTlvElement.Builder(tlv).parent(parent)
            when (tlv.tag) {
                TAG_EPDG_IDENTIFIER -> builder
                        .labelId(R.string.epdg_identifier_label)
                        .separator(::epdgIdentifierSeparator)
                else -> builder.decoder(::genericBerTlvDecoder)
            }.build(resources)
        }

        private fun epdgIdentifierSeparator(
            resources: Resources,
            value: ByteArray,
            parent: Element?
        ): List<Element> {
            if (value.isEmpty()) return emptyList()
            val addressType = value[0].toInt() and 0xFF
            return listOf(
                    PrimitiveElement.Builder(value.copyOfRange(0, 1))
                            .labelId(R.string.epdg_address_type_label)
                            .parent(parent)
                            .interpreter(::epdgAddressTypeInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(value.copyOfRange(1, value.size))
                            .labelId(R.string.epdg_address_label)
                            .parent(parent)
                            .interpreter { innerResources, rawData ->
                                epdgAddressInterpreter(innerResources, addressType, rawData)
                            }
                            .build(resources)
            )
        }

        private fun tvconfigDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val elements = mutableListOf<Element>()
            elements += PrimitiveElement.Builder(rawData.copyOfRange(0, TVCONFIG_PLMN_LENGTH))
                    .labelId(R.string.plmn_label)
                    .parent(parent)
                    .interpreter(::plmnInterpreter)
                    .build(resources)

            var index = TVCONFIG_PLMN_LENGTH
            while (index < rawData.size) {
                val tag = rawData[index++].toInt() and 0xFF
                val lengthInfo = readTlvLength(rawData, index) ?: break
                index = lengthInfo.first
                if (rawData.size < index + lengthInfo.second) break
                val value = rawData.copyOfRange(index, index + lengthInfo.second)
                index += lengthInfo.second
                elements += when (tag) {
                    TAG_TVCONFIG_TMGI_LIST -> ConstructedElement.Builder(value)
                            .labelId(R.string.one_byte_tag_label)
                            .labelArgs(resources.getString(R.string.tmgi_list_label), tag)
                            .parent(parent)
                            .decoder(::tvconfigTmgiListSeparator)
                            .interpreter { _, _ -> "" }
                            .build(resources)
                    TAG_TVCONFIG_EARFCN_LIST -> ConstructedElement.Builder(value)
                            .labelId(R.string.one_byte_tag_label)
                            .labelArgs(resources.getString(R.string.tv_earfcn_list_label),
                                    tag)
                            .parent(parent)
                            .decoder(::tvconfigEarfcnListSeparator)
                            .interpreter { _, _ -> "" }
                            .build(resources)
                    else -> PrimitiveElement.Builder(value)
                            .labelId(R.string.unknown_label)
                            .parent(parent)
                            .build(resources)
                }
            }
            return elements
        }

        private fun readTlvLength(rawData: ByteArray, startIndex: Int): Pair<Int, Int>? {
            if (startIndex >= rawData.size) return null
            var index = startIndex
            var length = rawData[index++].toInt() and 0xFF
            if (length > 0x7F) {
                val numberOfLengthBytes = length and 0x7F
                if (numberOfLengthBytes !in 1..3 || rawData.size < index + numberOfLengthBytes) {
                    return null
                }
                length = rawData.copyOfRange(index, index + numberOfLengthBytes)
                        .fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) }
                index += numberOfLengthBytes
            }
            return Pair(index, length)
        }

        private fun tvconfigTmgiListSeparator(
            resources: Resources,
            value: ByteArray,
            parent: Element?
        ): List<Element> {
            if (value.size % TVCONFIG_TMGI_ENTRY_LENGTH != 0) return emptyList()
            return value.asIterable().chunked(TVCONFIG_TMGI_ENTRY_LENGTH).mapIndexed {
                index, entry ->
                    ConstructedElement.Builder(entry.toByteArray())
                            .labelId(R.string.tmgi_entry_label)
                            .labelArgs(index + 1)
                            .parent(parent)
                            .decoder(::tvconfigTmgiEntryDecoder)
                            .interpreter { _, _ -> (index + 1).toString() }
                            .build(resources)
            }
        }

        private fun tvconfigTmgiEntryDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 6))
                        .labelId(R.string.tmgi_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(6, 8))
                        .labelId(R.string.usd_file_id_label)
                        .parent(parent)
                        .interpreter(::recordIdentifierInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(8, TVCONFIG_TMGI_ENTRY_LENGTH))
                        .labelId(R.string.service_type_label)
                        .parent(parent)
                        .interpreter(::unsignedIntegerInterpreter)
                        .build(resources)
        )

        private fun tvconfigEarfcnListSeparator(
            resources: Resources,
            value: ByteArray,
            parent: Element?
        ): List<Element> {
            if (value.size % TVCONFIG_EARFCN_ENTRY_LENGTH != 0) return emptyList()
            return value.asIterable().chunked(TVCONFIG_EARFCN_ENTRY_LENGTH).mapIndexed {
                index, entry ->
                    PrimitiveElement.Builder(entry.toByteArray())
                            .labelId(R.string.tv_earfcn_label)
                            .labelArgs(index + 1)
                            .parent(parent)
                            .interpreter(::unsignedIntegerInterpreter)
                            .build(resources)
            }
        }

        private fun threeGppPsDataOffDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                        .labelId(R.string.ps_data_off_home_services_label)
                        .parent(parent)
                        .interpreter(::psDataOffServicesInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(1, 2))
                        .labelId(R.string.ps_data_off_roaming_services_label)
                        .parent(parent)
                        .interpreter(::psDataOffServicesInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(2, THREE_GPP_PS_DATA_OFF_LENGTH))
                        .labelId(R.string.rfu_label)
                        .parent(parent)
                        .build(resources)
        )

        private fun threeGppPsDataOffServiceListDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = BerTlv.listFrom(rawData).mapIndexed { index, tlv ->
            val builder = BerTlvElement.Builder(tlv).parent(parent)
            when (tlv.tag) {
                TAG_ICSI -> builder
                        .labelId(R.string.ps_data_off_service_label)
                        .labelArgs(index + 1)
                        .interpreter(::utf8StringInterpreter)
                else -> builder.decoder(::genericBerTlvDecoder)
            }.build(resources)
        }

        private fun earfcnListDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = BerTlv.listFrom(rawData).mapIndexed { index, tlv ->
            val builder = BerTlvElement.Builder(tlv).parent(parent)
            when (tlv.tag) {
                TAG_EARFCN_LIST -> builder
                        .labelId(R.string.earfcn_list_entry_label)
                        .labelArgs(index + 1)
                        .decoder(::earfcnListTlvDecoder)
                else -> builder.decoder(::genericBerTlvDecoder)
            }.build(resources)
        }

        private fun earfcnListTlvDecoder(
            resources: Resources,
            tlvs: List<Tlv>,
            parent: Element?
        ): List<Element> = tlvs.map { tlv ->
            val builder = BerTlvElement.Builder(tlv).parent(parent)
            when (tlv.tag) {
                TAG_EARFCN -> builder
                        .labelId(R.string.earfcn_label)
                        .interpreter(::unsignedIntegerInterpreter)
                TAG_GEOGRAPHICAL_AREA_POLYGON -> builder
                        .labelId(R.string.geographical_area_polygon_label)
                        .separator(::geographicalAreaPolygonSeparator)
                else -> builder.decoder(::genericBerTlvDecoder)
            }.build(resources)
        }

        private fun geographicalAreaPolygonSeparator(
            resources: Resources,
            value: ByteArray,
            parent: Element?
        ): List<Element> {
            if (value.size < GEOGRAPHICAL_POINT_LENGTH * 3 ||
                    value.size % GEOGRAPHICAL_POINT_LENGTH != 0) {
                return emptyList()
            }
            return value.asIterable().chunked(GEOGRAPHICAL_POINT_LENGTH).mapIndexed {
                index, point ->
                    ConstructedElement.Builder(point.toByteArray())
                            .labelId(R.string.geographical_point_label)
                            .labelArgs(index + 1)
                            .parent(parent)
                            .decoder(::geographicalPointDecoder)
                            .interpreter { _, _ -> (index + 1).toString() }
                            .build(resources)
            }
        }

        private fun geographicalPointDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 3))
                        .labelId(R.string.latitude_label)
                        .parent(parent)
                        .interpreter(::unsignedIntegerInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(3, GEOGRAPHICAL_POINT_LENGTH))
                        .labelId(R.string.longitude_label)
                        .parent(parent)
                        .interpreter(::unsignedIntegerInterpreter)
                        .build(resources)
        )

        private fun eakaDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val elements = mutableListOf<Element>()
            elements += PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                    .labelId(R.string.enhanced_aka_support_label)
                    .parent(parent)
                    .interpreter(::enhancedAkaSupportInterpreter)
                    .build(resources)
            if (rawData.size > 1) {
                elements += PrimitiveElement.Builder(rawData.copyOfRange(1, rawData.size))
                        .labelId(R.string.rfu_label)
                        .parent(parent)
                        .build(resources)
            }
            return elements
        }

        private fun ocstDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val elements = mutableListOf<Element>()
            elements += PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                    .labelId(R.string.sense_enabled_by_operator_label)
                    .parent(parent)
                    .interpreter(::enabledInterpreter)
                    .build(resources)
            BerTlv.listFrom(rawData.copyOfRange(1, rawData.size)).forEach { tlv ->
                val builder = BerTlvElement.Builder(tlv).parent(parent)
                elements += when (tlv.tag) {
                    TAG_OCST_PARAMETERS -> builder
                            .labelId(R.string.ocst_parameters_label)
                            .separator(::ocstParametersSeparator)
                    else -> builder.decoder(::genericBerTlvDecoder)
                }.build(resources)
            }
            return elements
        }

        private fun ocstParametersSeparator(
            resources: Resources,
            value: ByteArray,
            parent: Element?
        ): List<Element> {
            if (value.size % OCST_ENTRY_LENGTH != 0) return emptyList()
            return value.asIterable().chunked(OCST_ENTRY_LENGTH).mapIndexed { index, entry ->
                ConstructedElement.Builder(entry.toByteArray())
                        .labelId(R.string.ocst_entry_label)
                        .labelArgs(index + 1)
                        .parent(parent)
                        .decoder(::ocstEntryDecoder)
                        .interpreter { _, _ -> (index + 1).toString() }
                        .build(resources)
            }
        }

        private fun ocstEntryDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 2))
                        .labelId(R.string.access_technology_identifier_label)
                        .parent(parent)
                        .interpreter(::accessTechnologyInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(2, OCST_ENTRY_LENGTH))
                        .labelId(R.string.operator_signal_threshold_label)
                        .parent(parent)
                        .interpreter(::signalThresholdInterpreter)
                        .build(resources)
        )

        private fun acGbauapiDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = BerTlv.listFrom(rawData).map { tlv ->
            val builder = BerTlvElement.Builder(tlv).parent(parent)
            when (tlv.tag) {
                TAG_APPLET_NAF_ACCESS_CONTROL -> builder
                        .labelId(R.string.applet_naf_access_control_label)
                        .separator(::appletNafAccessControlSeparator)
                else -> builder.decoder(::genericBerTlvDecoder)
            }.build(resources)
        }

        private fun appletNafAccessControlSeparator(
            resources: Resources,
            value: ByteArray,
            parent: Element?
        ): List<Element> {
            var index = 0
            val aidLength = value.getOrNull(index++)?.toInt()?.and(0xFF) ?: return emptyList()
            if (aidLength !in 5..16 || value.size < index + aidLength + 1) return emptyList()
            val aid = value.copyOfRange(index, index + aidLength)
            index += aidLength
            val nafIdLength = value.getOrNull(index++)?.toInt()?.and(0xFF) ?: return emptyList()
            if (value.size < index + nafIdLength) return emptyList()
            val nafId = value.copyOfRange(index, index + nafIdLength)
            return listOf(
                    PrimitiveElement.Builder(byteArrayOf(aidLength.toByte()))
                            .labelId(R.string.length_of_aid_label)
                            .parent(parent)
                            .interpreter(::unsignedIntegerInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(aid)
                            .labelId(R.string.applet_aid_label)
                            .parent(parent)
                            .build(resources),
                    PrimitiveElement.Builder(byteArrayOf(nafIdLength.toByte()))
                            .labelId(R.string.length_of_naf_id_label)
                            .parent(parent)
                            .interpreter(::unsignedIntegerInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(nafId)
                            .labelId(R.string.naf_id_label)
                            .parent(parent)
                            .interpreter(PrimitiveElement::defaultStringInterpreter)
                            .build(resources)
            )
        }

        private fun oplmnWActLspDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val elements = mutableListOf<Element>()
            elements += PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                    .labelId(R.string.priority_label)
                    .parent(parent)
                    .interpreter(::enabledInterpreter)
                    .build(resources)
            elements += rawData.copyOfRange(1, rawData.size).asIterable()
                    .chunked(OPLMN_W_ACT_LSP_ENTRY_LENGTH).mapIndexed { index, entry ->
                        ConstructedElement.Builder(entry.toByteArray())
                                .labelId(R.string.oplmn_w_act_lsp_entry_label)
                                .labelArgs(index + 1)
                                .parent(parent)
                                .decoder(::oplmnWActLspEntryDecoder)
                                .interpreter { _, _ -> (index + 1).toString() }
                                .build(resources)
                    }
            return elements
        }

        private fun oplmnWActLspEntryDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 3))
                        .labelId(R.string.plmn_label)
                        .parent(parent)
                        .interpreter(::plmnInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(3, 5))
                        .labelId(R.string.access_technology_identifier_label)
                        .parent(parent)
                        .interpreter(::accessTechnologyInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(5, OPLMN_W_ACT_LSP_ENTRY_LENGTH))
                        .labelId(R.string.selected_sat_feature_label)
                        .parent(parent)
                        .interpreter(::enabledInterpreter)
                        .build(resources)
        )

        private fun languageCodeInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size == LI_ENTRY_LENGTH && rawData.all { it.toInt() and 0xFF == 0xFF }) {
                return byteArrayToHexString(rawData) + " (" +
                        resources.getString(R.string.unused_label) + ")"
            }
            return PrimitiveElement.defaultStringInterpreter(resources, rawData)
        }

        private fun imsiInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val digits = mutableListOf<Char>()
            rawData.forEachIndexed { index, byte ->
                val value = byte.toInt() and 0xFF
                if (index == 0) {
                    digits.add(nibbleToDigit((value ushr 4) and 0x0F))
                } else {
                    digits.add(nibbleToDigit(value and 0x0F))
                    digits.add(nibbleToDigit((value ushr 4) and 0x0F))
                }
            }
            val imsi = digits.filter { it != 'F' }.joinToString("")
            return byteArrayToHexString(rawData) + " ($imsi)"
        }

        private fun keySetIdentifierInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            return byteArrayToHexString(rawData) + " (${value and 0x07})"
        }

        private fun plmnInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size != 3) return byteArrayToHexString(rawData)
            if (isUnusedPlmn(rawData)) {
                return byteArrayToHexString(rawData) + " (" +
                        resources.getString(R.string.unused_label) + ")"
            }

            val b1 = rawData[0].toInt() and 0xFF
            val b2 = rawData[1].toInt() and 0xFF
            val b3 = rawData[2].toInt() and 0xFF
            val mcc = "${b1 and 0x0F}${(b1 ushr 4) and 0x0F}${b2 and 0x0F}"
            val mncDigit3 = (b2 ushr 4) and 0x0F
            val mnc = "${b3 and 0x0F}${(b3 ushr 4) and 0x0F}" +
                    if (mncDigit3 == 0x0F) "" else mncDigit3.toString()
            return hexWithDescription(
                    resources,
                    rawData,
                    resources.getString(R.string.plmn_interpretation, mcc, mnc)
            )
        }

        private fun accessTechnologyInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size != 2) return byteArrayToHexString(rawData)

            val first = rawData[0].toInt() and 0xFF
            val second = rawData[1].toInt() and 0xFF
            val names = linkedSetOf<String>()
            if (first and 0x80 != 0) names.add(
                    resources.getString(R.string.access_tech_satellite_e_utran_nb_s1))
            if (first and 0x40 != 0) names.add(
                    resources.getString(R.string.access_tech_satellite_e_utran_wb_s1))
            if (first and 0x20 != 0) names.add(
                    resources.getString(R.string.access_tech_satellite_ng_ran))
            if (first and 0x10 != 0) names.add(resources.getString(R.string.access_tech_ng_ran))
            if (first and 0x08 != 0) names.add(
                    resources.getString(R.string.access_tech_e_utran_nb_s1))
            if (first and 0x04 != 0) names.add(
                    resources.getString(R.string.access_tech_e_utran_wb_s1))
            if (first and 0x02 != 0) names.add(resources.getString(R.string.access_tech_e_utran))
            if (first and 0x01 != 0) names.add(resources.getString(R.string.access_tech_utran))
            if (second and 0x20 != 0) names.add(resources.getString(R.string.access_tech_gsm))
            if (second and 0x10 != 0) names.add(
                    resources.getString(R.string.access_tech_ec_gsm_iot))
            if (second and 0x08 != 0) names.add(
                    resources.getString(R.string.access_tech_cdma2000_1xrtt))
            if (second and 0x04 != 0) names.add(
                    resources.getString(R.string.access_tech_cdma2000_hrpd))
            if (second and 0x02 != 0) names.add(
                    resources.getString(R.string.access_tech_gsm_compact))
            if (second and 0x01 != 0) names.add(resources.getString(R.string.access_tech_gsm))

            val suffix = if (names.isEmpty()) {
                resources.getString(R.string.no_access_technology_selected)
            } else {
                names.joinToString(", ")
            }
            return hexWithDescription(resources, rawData, suffix)
        }

        private fun hpplmnInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val hex = byteArrayToHexString(rawData)
            if (value == 0) {
                return "$hex (${resources.getString(R.string.no_hpplmn_search_attempts)})"
            }

            val iotText = when (value) {
                1 -> resources.getString(R.string.hpplmn_iot_hours, 2)
                in 2..40 -> resources.getString(R.string.hpplmn_iot_hours, value * 2)
                in 41..80 -> resources.getString(R.string.hpplmn_iot_hours, value * 4 - 80)
                else -> resources.getString(R.string.hpplmn_iot_default_period)
            }
            return resources.getString(
                    R.string.hex_with_description,
                    hex,
                    resources.getString(R.string.hpplmn_interval_interpretation, value, iotText)
            )
        }

        private fun unsignedIntegerInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) }
            return byteArrayToHexString(rawData) + " ($value)"
        }

        private fun secondsInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val seconds = rawData.fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) }
            return hexWithDescription(
                    resources,
                    rawData,
                    resources.getString(R.string.timeout_seconds, seconds)
            )
        }

        private fun callDateTimeInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size != 7 || rawData.all { it.toInt() and 0xFF == 0xFF }) {
                return byteArrayToHexString(rawData)
            }
            val values = rawData.take(6).map { swappedBcdByteValue(it) }
            if (values.any { it == null }) return byteArrayToHexString(rawData)
            val timezoneByte = rawData[6].toInt() and 0xFF
            val timezone = if (timezoneByte == 0xFF) {
                ""
            } else {
                val negative = timezoneByte and 0x08 != 0
                val quarters = swappedBcdByteValue((timezoneByte and 0xF7).toByte())
                        ?: return byteArrayToHexString(rawData)
                val minutes = quarters * 15
                " %s%02d:%02d".format(
                        Locale.US,
                        if (negative) "-" else "+",
                        minutes / 60,
                        minutes % 60
                )
            }
            val description = "%02d-%02d-%02d %02d:%02d:%02d%s".format(
                    Locale.US,
                    values[0]!!,
                    values[1]!!,
                    values[2]!!,
                    values[3]!!,
                    values[4]!!,
                    values[5]!!,
                    timezone
            )
            return hexWithDescription(resources, rawData, description)
        }

        private fun callStatusInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0x01)
                    ?: return byteArrayToHexString(rawData)
            val status = if (value == 0) {
                resources.getString(R.string.call_answered)
            } else {
                resources.getString(R.string.call_not_answered)
            }
            return hexWithDescription(resources, rawData, status)
        }

        private fun phoneBookLinkInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size != 3 || rawData.all { it.toInt() and 0xFF == 0xFF }) {
                return byteArrayToHexString(rawData)
            }
            val local = rawData[0].toInt() and 0x01 != 0
            val pbrRecord = rawData[1].toInt() and 0xFF
            val adnRecord = rawData[2].toInt() and 0xFF
            val description = resources.getString(
                    if (local) R.string.local_phone_book_link else R.string.global_phone_book_link,
                    pbrRecord,
                    adnRecord
            )
            return hexWithDescription(resources, rawData, description)
        }

        private fun pairingStatusInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val status = when (rawData.toString(Charsets.US_ASCII)) {
                "OK" -> resources.getString(R.string.pairing_successful)
                "KO" -> resources.getString(R.string.pairing_unsuccessful)
                else -> return byteArrayToHexString(rawData)
            }
            return hexWithDescription(resources, rawData, status)
        }

        private fun swappedBcdByteValue(byte: Byte): Int? {
            val value = byte.toInt() and 0xFF
            val first = value and 0x0F
            val second = value ushr 4
            if (first > 9 || second > 9) return null
            return first * 10 + second
        }

        private fun usimServiceName(
            resources: Resources,
            serviceNumber: Int
        ): String = resources.getStringArray(R.array.usim_service_names).getOrElse(
                serviceNumber - 1) {
            resources.getString(R.string.rfu_label)
        }

        private fun usimServiceStateInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val available = rawData.firstOrNull()?.toInt()?.and(0x01) == 1
            return if (available) {
                resources.getString(R.string.available_label)
            } else {
                resources.getString(R.string.not_available_label)
            }
        }

        private fun enabledInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val enabled = rawData.firstOrNull()?.toInt()?.and(0x01) == 1
            val label = if (enabled) {
                resources.getString(R.string.enabled_label)
            } else {
                resources.getString(R.string.disabled_label)
            }
            return hexWithDescription(resources, rawData, label)
        }

        private fun enhancedAkaSupportInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val supported = rawData.firstOrNull()?.toInt()?.and(0x01) == 1
            val label = if (supported) {
                resources.getString(R.string.enhanced_sqn_calculation_supported)
            } else {
                resources.getString(R.string.enhanced_sqn_calculation_not_supported)
            }
            return hexWithDescription(resources, rawData, label)
        }

        private fun pwsConfigurationInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val hplmnState = if (value and 0x01 == 0) {
                resources.getString(R.string.pws_messages_accepted)
            } else {
                resources.getString(R.string.pws_messages_ignored)
            }
            val vplmnState = if (value and 0x02 == 0) {
                resources.getString(R.string.pws_messages_accepted)
            } else {
                resources.getString(R.string.pws_messages_ignored)
            }
            val description = resources.getString(
                    R.string.pws_configuration_interpretation,
                    hplmnState,
                    vplmnState
            )
            return hexWithDescription(resources, rawData, description)
        }

        private fun estServiceName(
            resources: Resources,
            serviceNumber: Int
        ): String = when (serviceNumber) {
            1 -> resources.getString(R.string.est_service_fdn)
            2 -> resources.getString(R.string.est_service_bdn)
            3 -> resources.getString(R.string.est_service_acl)
            else -> resources.getString(R.string.rfu_label)
        }

        private fun activatedStateInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val activated = rawData.firstOrNull()?.toInt()?.and(0x01) == 1
            return if (activated) {
                resources.getString(R.string.activated_label)
            } else {
                resources.getString(R.string.deactivated_label)
            }
        }

        private fun emlppPriorityMaskInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val levels = listOf("A", "B", "0", "1", "2", "3", "4")
                    .filterIndexed { index, _ -> value and (1 shl index) != 0 }
            val suffix = if (levels.isEmpty()) {
                resources.getString(R.string.no_priority_level_selected)
            } else {
                resources.getString(R.string.priority_levels_interpretation,
                        levels.joinToString(", "))
            }
            return hexWithDescription(resources, rawData, suffix)
        }

        private fun nonSwappedBcdStringInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val text = rawData.flatMap { byte ->
                val value = byte.toInt() and 0xFF
                listOf((value ushr 4) and 0x0F, value and 0x0F)
            }.map(::nibbleToDiallingChar)
                    .filter { it != 'F' }
                    .joinToString("")
            return hexWithDescription(resources, rawData, text)
        }

        private fun emptyOrUnsignedIntegerInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.all { it.toInt() and 0xFF == 0xFF }) {
                return hexWithDescription(
                        resources,
                        rawData,
                        resources.getString(R.string.empty_label)
                )
            }
            return unsignedIntegerInterpreter(resources, rawData)
        }

        private fun recordIdentifierInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val label = when (value) {
                0x00 -> resources.getString(R.string.no_record_associated)
                0xFF -> resources.getString(R.string.unused_label)
                else -> resources.getString(R.string.record_number, value)
            }
            return hexWithDescription(resources, rawData, label)
        }

        private fun messageWaitingStatusInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String = bitStatusInterpreter(
                resources,
                rawData,
                listOf(
                        R.string.voicemail_label,
                        R.string.fax_label,
                        R.string.email_label,
                        R.string.other_label,
                        R.string.videomail_label
                )
        )

        private fun cfuIndicatorStatusInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String = bitStatusInterpreter(
                resources,
                rawData,
                listOf(
                        R.string.voice_label,
                        R.string.fax_label,
                        R.string.all_data_teleservices_label,
                        R.string.sms_label,
                        R.string.all_bearer_services_label
                )
        )

        private fun bitStatusInterpreter(
            resources: Resources,
            rawData: ByteArray,
            labelIds: List<Int>
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val active = labelIds.mapIndexedNotNull { index, labelId ->
                if (value and (1 shl index) != 0) resources.getString(labelId) else null
            }
            val suffix = if (active.isEmpty()) {
                resources.getString(R.string.no_indicator_active)
            } else {
                active.joinToString(", ")
            }
            return hexWithDescription(resources, rawData, suffix)
        }

        private fun mmsStatusInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val first = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val space = if (first and 0x01 == 0) {
                resources.getString(R.string.mms_status_free_space)
            } else {
                resources.getString(R.string.mms_status_used_space)
            }
            val read = if (first and 0x02 == 0) {
                resources.getString(R.string.mms_status_notification_not_read)
            } else {
                resources.getString(R.string.mms_status_notification_read)
            }
            val retrieval = when ((first ushr 2) and 0x03) {
                0 -> resources.getString(R.string.mms_status_not_retrieved)
                1 -> resources.getString(R.string.mms_status_retrieved)
                2 -> resources.getString(R.string.mms_status_rejected)
                else -> resources.getString(R.string.mms_status_forwarded)
            }
            return hexWithDescription(resources, rawData, listOf(space, read, retrieval)
                    .joinToString(", "))
        }

        private fun mmsImplementationInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val supported = mutableListOf<String>()
            if (value and 0x01 != 0) supported.add(resources.getString(R.string.mms_impl_wap))
            if (value and 0x02 != 0) supported.add(resources.getString(R.string.mms_impl_m_imap))
            if (value and 0x04 != 0) supported.add(resources.getString(R.string.mms_impl_sip))
            val suffix = if (supported.isEmpty()) {
                resources.getString(R.string.no_mms_implementation_supported)
            } else {
                supported.joinToString(", ")
            }
            return hexWithDescription(resources, rawData, suffix)
        }

        private fun cipheringAlgorithmInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val suffix = when (value) {
                0x00 -> resources.getString(R.string.no_ciphering_label)
                in 0x01..0x07 -> resources.getString(R.string.gsm_a5_algorithm, value)
                else -> resources.getString(R.string.rfu_label)
            }
            return hexWithDescription(resources, rawData, suffix)
        }

        private fun ehplmnPresentationIndicationInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val suffix = when (rawData.firstOrNull()?.toInt()?.and(0xFF)) {
                0x00 -> resources.getString(R.string.no_preference_display_mode)
                0x01 -> resources.getString(R.string.display_highest_priority_ehplmn)
                0x02 -> resources.getString(R.string.display_all_ehplmns)
                else -> resources.getString(R.string.rfu_label)
            }
            return hexWithDescription(resources, rawData, suffix)
        }

        private fun lastRplmnSelectionIndicationInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val suffix = when (rawData.firstOrNull()?.toInt()?.and(0xFF)) {
                0x00 -> resources.getString(R.string.select_last_rplmn)
                0x01 -> resources.getString(R.string.select_hplmn_or_last_rplmn)
                else -> resources.getString(R.string.rfu_label)
            }
            return hexWithDescription(resources, rawData, suffix)
        }

        private fun utf8StringInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val text = rawData.dropLastWhile { it.toInt() and 0xFF == 0xFF }
                    .toByteArray()
                    .toString(Charsets.UTF_8)
            return hexWithDescription(resources, rawData, text)
        }

        private fun typeOfAddressInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val suffix = when (rawData.firstOrNull()?.toInt()?.and(0xFF)) {
                0x21 -> resources.getString(R.string.ipv4_address_range)
                0x57 -> resources.getString(R.string.ipv6_address_range)
                else -> resources.getString(R.string.rfu_label)
            }
            return hexWithDescription(resources, rawData, suffix)
        }

        private fun epdgFqdnFormatInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val suffix = when (rawData.firstOrNull()?.toInt()?.and(0xFF)) {
                0x00 -> resources.getString(R.string.fqdn_format_operator_identifier)
                0x01 -> resources.getString(R.string.fqdn_format_location_based)
                else -> resources.getString(R.string.rfu_label)
            }
            return hexWithDescription(resources, rawData, suffix)
        }

        private fun epdgAddressTypeInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val suffix = when (rawData.firstOrNull()?.toInt()?.and(0xFF)) {
                0x00 -> resources.getString(R.string.epdg_address_type_fqdn)
                0x01 -> resources.getString(R.string.epdg_address_type_ipv4)
                0x02 -> resources.getString(R.string.epdg_address_type_ipv6)
                else -> resources.getString(R.string.rfu_label)
            }
            return hexWithDescription(resources, rawData, suffix)
        }

        private fun epdgAddressInterpreter(
            resources: Resources,
            addressType: Int,
            rawData: ByteArray
        ): String {
            val suffix = when (addressType) {
                0x00 -> rawData.toString(Charsets.UTF_8)
                0x01 -> if (rawData.size == 4) {
                    rawData.joinToString(".") { (it.toInt() and 0xFF).toString() }
                } else {
                    resources.getString(R.string.invalid_length_label)
                }
                0x02 -> if (rawData.size == 16) {
                    rawData.asIterable().chunked(2)
                            .joinToString(":") { chunk ->
                                chunk.joinToString("") { byte ->
                                    "%02X".format(byte.toInt() and 0xFF)
                                }
                            }
                } else {
                    resources.getString(R.string.invalid_length_label)
                }
                else -> resources.getString(R.string.rfu_label)
            }
            return hexWithDescription(resources, rawData, suffix)
        }

        private fun psDataOffServicesInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val services = listOf(
                    R.string.ps_data_off_service_ussi,
                    R.string.ps_data_off_service_mmtel_voice,
                    R.string.ps_data_off_service_mmtel_video,
                    R.string.ps_data_off_service_ssp_xcap,
                    R.string.ps_data_off_service_sms_over_ip,
                    R.string.ps_data_off_service_bip,
                    R.string.ps_data_off_service_device_management
            ).mapIndexedNotNull { index, stringId ->
                if (value and (1 shl index) != 0) resources.getString(stringId) else null
            }
            val suffix = if (services.isEmpty()) {
                resources.getString(R.string.no_exempt_service_label)
            } else {
                services.joinToString(", ")
            }
            return hexWithDescription(resources, rawData, suffix)
        }

        private fun signalThresholdInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val label = if (value == 0xFF) {
                resources.getString(R.string.unused_label)
            } else {
                resources.getString(R.string.dbm_label, -value)
            }
            return hexWithDescription(resources, rawData, label)
        }

        private fun imsDataChannelIndicationInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val label = when (value) {
                0x00 -> resources.getString(R.string.ims_data_channel_not_allowed)
                0x01 -> resources.getString(R.string.ims_data_channel_allowed_not_simultaneous)
                0x02 -> resources.getString(R.string.ims_data_channel_allowed_simultaneous)
                else -> resources.getString(R.string.rfu_label)
            }
            return hexWithDescription(resources, rawData, label)
        }

        private fun trackingAreaIdentityInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size != 5) return byteArrayToHexString(rawData)

            val plmn = plmnInterpreter(resources, rawData.copyOfRange(0, 3))
            val tac = byteArrayToHexString(rawData.copyOfRange(3, 5))
            return hexWithDescription(resources, rawData, "$plmn, TAC $tac")
        }

        private fun epsUpdateStatusInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val status = when (rawData.firstOrNull()?.toInt()?.and(0x07)) {
                0x00 -> resources.getString(R.string.eps_update_status_updated)
                0x01 -> resources.getString(R.string.eps_update_status_not_updated)
                0x02 -> resources.getString(R.string.eps_update_status_roaming_not_allowed)
                else -> resources.getString(R.string.reserved_label)
            }
            return hexWithDescription(resources, rawData, status)
        }

        private fun networkNameInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val header = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val codingScheme = (header ushr 4) and 0x07
            val addCi = header and 0x08 != 0
            val spareBits = header and 0x07
            val nameBytes = rawData.copyOfRange(1, rawData.size)
            val coding = when (codingScheme) {
                0 -> resources.getString(R.string.gsm_default_alphabet_label)
                1 -> resources.getString(R.string.ucs2_label)
                else -> resources.getString(R.string.unknown_label)
            }
            val text = when (codingScheme) {
                0 -> decodeGsm7Packed(nameBytes, spareBits)
                1 -> StringUtils.decode(byteArrayOf(0x80.toByte()) + nameBytes)
                else -> ""
            }
            val ci = if (addCi) {
                resources.getString(R.string.country_initials_added)
            } else {
                resources.getString(R.string.country_initials_not_added)
            }
            val description = if (text.isEmpty()) {
                resources.getString(R.string.network_name_interpretation_without_text, coding, ci)
            } else {
                resources.getString(R.string.network_name_interpretation, text, coding, ci)
            }
            return hexWithDescription(resources, rawData, description)
        }

        private fun alphaIdentifierInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val textData = rawData.dropLastWhile { it.toInt() and 0xFF == 0xFF }.toByteArray()
            return hexWithDescription(resources, rawData, StringUtils.decode(textData))
        }

        private fun spnDisplayConditionInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val registeredPlmn = if (value and 0x01 == 0) {
                resources.getString(R.string.spn_registered_plmn_name_not_required)
            } else {
                resources.getString(R.string.spn_registered_plmn_name_required)
            }
            val spn = if (value and 0x02 == 0) {
                resources.getString(R.string.spn_required_outside_hplmn_or_spdi_plmn)
            } else {
                resources.getString(R.string.spn_not_required_outside_hplmn_or_spdi_plmn)
            }
            return hexWithDescription(
                    resources,
                    rawData,
                    resources.getString(R.string.semicolon_joined_descriptions, registeredPlmn, spn)
            )
        }

        private fun pricePerUnitInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size != 2) return byteArrayToHexString(rawData)

            val first = rawData[0].toInt() and 0xFF
            val second = rawData[1].toInt() and 0xFF
            val eppu = (first shl 4) or (second and 0x0F)
            val exponentMagnitude = (second ushr 5) and 0x07
            val exponent = if (second and 0x10 == 0) exponentMagnitude else -exponentMagnitude
            val price = eppu * 10.0.pow(exponent)
            return hexWithDescription(
                    resources,
                    rawData,
                    resources.getString(
                            R.string.price_per_unit_interpretation,
                            eppu,
                            exponent,
                            price.toString()
                    )
            )
        }

        private fun cbMessageIdentifierInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.all { it.toInt() and 0xFF == 0xFF }) {
                return byteArrayToHexString(rawData) + " (" +
                        resources.getString(R.string.unused_label) + ")"
            }
            return unsignedIntegerInterpreter(resources, rawData)
        }

        private fun accessControlClassesInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size != ACC_LENGTH) return byteArrayToHexString(rawData)

            val allocated = mutableListOf<Int>()
            val first = rawData[0].toInt() and 0xFF
            val second = rawData[1].toInt() and 0xFF
            (0 until 8).forEach { bit ->
                if (first and (1 shl bit) != 0 && bit != 2) allocated.add(bit + 8)
                if (second and (1 shl bit) != 0) allocated.add(bit)
            }
            val suffix = if (allocated.isEmpty()) {
                resources.getString(R.string.no_access_control_class_allocated)
            } else {
                allocated.sorted().joinToString(", ") { it.toString().padStart(2, '0') }
            }
            return byteArrayToHexString(rawData) + " ($suffix)"
        }

        private fun plmnEntryInterpreter(
            resources: Resources,
            index: Int,
            rawData: ByteArray
        ): String = resources.getString(
                R.string.plmn_entry_interpretation,
                index,
                plmnInterpreter(resources, rawData)
        )

        private fun locationAreaInformationInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size != 5) return byteArrayToHexString(rawData)

            val plmn = plmnInterpreter(resources, rawData.copyOfRange(0, 3))
            val lac = byteArrayToHexString(rawData.copyOfRange(3, 5))
            return hexWithDescription(
                    resources,
                    rawData,
                    resources.getString(R.string.location_area_interpretation, plmn, lac)
            )
        }

        private fun routingAreaInformationInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size != 6) return byteArrayToHexString(rawData)

            val lai = locationAreaInformationInterpreter(resources, rawData.copyOfRange(0, 5))
            val rac = byteArrayToHexString(rawData.copyOfRange(5, 6))
            return hexWithDescription(resources, rawData, "$lai, RAC $rac")
        }

        private fun locationUpdateStatusInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val status = when (rawData.firstOrNull()?.toInt()?.and(0x07)) {
                0x00 -> resources.getString(R.string.location_update_status_updated)
                0x01 -> resources.getString(R.string.location_update_status_not_updated)
                0x02 -> resources.getString(R.string.location_update_status_plmn_not_allowed)
                0x03 -> resources.getString(R.string.location_update_status_lai_not_allowed)
                0x07 -> resources.getString(R.string.reserved_label)
                else -> resources.getString(R.string.rfu_label)
            }
            return hexWithDescription(resources, rawData, status)
        }

        private fun routingAreaUpdateStatusInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val status = when (rawData.firstOrNull()?.toInt()?.and(0x07)) {
                0x00 -> resources.getString(R.string.location_update_status_updated)
                0x01 -> resources.getString(R.string.location_update_status_not_updated)
                0x02 -> resources.getString(R.string.location_update_status_plmn_not_allowed)
                0x03 -> resources.getString(R.string.routing_area_update_status_rai_not_allowed)
                0x07 -> resources.getString(R.string.reserved_label)
                else -> resources.getString(R.string.rfu_label)
            }
            return hexWithDescription(resources, rawData, status)
        }

        private fun swappedBcdStringInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val text = rawData.flatMap { byte ->
                val value = byte.toInt() and 0xFF
                listOf(value and 0x0F, (value ushr 4) and 0x0F)
            }.map(::nibbleToDiallingChar)
                    .filter { it != 'F' }
                    .joinToString("")
            return hexWithDescription(resources, rawData, text)
        }

        private fun emergencyCallCodeInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String = swappedBcdStringInterpreter(resources, rawData)

        private fun smsStatusInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val status = when {
                value == 0x00 || value == 0xFF -> resources.getString(
                        R.string.sms_status_free_space)
                value and 0x01 == 0 -> resources.getString(R.string.sms_record_empty)
                value and 0x07 == 0x01 -> resources.getString(R.string.sms_status_received_read)
                value and 0x07 == 0x03 -> resources.getString(R.string.sms_status_received_unread)
                value and 0x07 == 0x05 -> resources.getString(R.string.sms_status_mo_to_be_sent)
                value and 0x07 == 0x07 -> resources.getString(R.string.sms_status_used_space)
                value == 0x09 -> resources.getString(R.string.sms_status_mo_sent_no_status_report)
                value == 0x0D -> resources.getString(
                        R.string.sms_status_mo_sent_status_report_not_received)
                value == 0x15 -> resources.getString(
                        R.string.sms_status_mo_sent_status_report_received_not_stored)
                else -> resources.getString(R.string.sms_status_used_space)
            }
            return hexWithDescription(resources, rawData, status)
        }

        private fun memoryCapacityExceededInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val exceeded = rawData.firstOrNull()?.toInt()?.and(0x01) == 1
            val label = if (exceeded) {
                resources.getString(R.string.memory_capacity_exceeded)
            } else {
                resources.getString(R.string.memory_capacity_not_exceeded)
            }
            return hexWithDescription(resources, rawData, label)
        }

        private fun smsRecordIdentifierInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val label = if (value == 0x00 || value == 0xFF) {
                resources.getString(R.string.sms_record_empty)
            } else {
                resources.getString(R.string.sms_record_number, value)
            }
            return hexWithDescription(resources, rawData, label)
        }

        private fun ueOperationModeInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val mode = when (rawData.firstOrNull()?.toInt()?.and(0xFF)) {
                0x00 -> resources.getString(R.string.ue_operation_mode_normal)
                0x80 -> resources.getString(R.string.ue_operation_mode_type_approval)
                0x01 -> resources.getString(R.string.ue_operation_mode_normal_specific)
                0x81 -> resources.getString(R.string.ue_operation_mode_type_approval_specific)
                0x02 -> resources.getString(R.string.ue_operation_mode_maintenance_off_line)
                0x04 -> resources.getString(R.string.ue_operation_mode_cell_test)
                else -> resources.getString(R.string.rfu_label)
            }
            return hexWithDescription(resources, rawData, mode)
        }

        private fun additionalInformationInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size != 2) return byteArrayToHexString(rawData)

            val second = rawData[1].toInt() and 0xFF
            val flags = listOf(
                    resources.getString(R.string.additional_info_ciphering_indicator) to
                            (second and 0x01 != 0),
                    resources.getString(R.string.additional_info_csg_display_control) to
                            (second and 0x02 != 0),
                    resources.getString(R.string.additional_info_prose_public_safety) to
                            (second and 0x04 != 0),
                    resources.getString(R.string.additional_info_extended_drx_cycle) to
                            (second and 0x08 != 0),
                    resources.getString(R.string.additional_info_5g_prose) to
                            (second and 0x10 != 0)
            ).filter { it.second }.map { it.first }
            val suffix = if (flags.isEmpty()) {
                resources.getString(R.string.no_additional_information_enabled)
            } else {
                flags.joinToString(", ")
            }
            return hexWithDescription(resources, rawData, suffix)
        }

        private fun mncLengthInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0x0F) ?: return ""
            val suffix = when (value) {
                0 -> resources.getString(R.string.mnc_length_zero_digits_service_130_available)
                2 -> resources.getString(R.string.mnc_length_digits, 2)
                3 -> resources.getString(R.string.mnc_length_digits, 3)
                else -> resources.getString(R.string.rfu_label)
            }
            return hexWithDescription(resources, rawData, suffix)
        }

        private fun hexWithDescription(
            resources: Resources,
            rawData: ByteArray,
            description: String
        ): String {
            val hex = byteArrayToHexString(rawData)
            if (description.isEmpty()) return hex

            return resources.getString(R.string.hex_with_description, hex, description)
        }

        private fun isUnusedPlmn(rawData: ByteArray): Boolean {
            return rawData.size == 3 && rawData.all { it.toInt() and 0xFF == 0xFF }
        }

        private fun nibbleToDigit(nibble: Int): Char {
            return if (nibble in 0..9) {
                ('0'.code + nibble).toChar()
            } else {
                nibble.toString(16).uppercase(Locale.US)[0]
            }
        }

        private fun nibbleToDiallingChar(nibble: Int): Char {
            return when (nibble) {
                in 0..9 -> ('0'.code + nibble).toChar()
                0x0A -> '*'
                0x0B -> '#'
                0x0C -> 'a'
                0x0D -> 'b'
                0x0E -> 'c'
                else -> 'F'
            }
        }

        private fun decodeGsm7Packed(
            rawData: ByteArray,
            spareBits: Int
        ): String {
            if (rawData.isEmpty()) return ""

            val actualSeptetCount = ((rawData.size * 8 - spareBits) / 7).coerceAtLeast(0)
            val chars = mutableListOf<Char>()
            var escaped = false
            (0 until actualSeptetCount).forEach { index ->
                val bitOffset = index * 7
                val byteIndex = bitOffset / 8
                val shift = bitOffset % 8
                val first = rawData.getOrNull(byteIndex)?.toInt()?.and(0xFF) ?: return@forEach
                val second = rawData.getOrNull(byteIndex + 1)?.toInt()?.and(0xFF) ?: 0
                val septet = ((first ushr shift) or (second shl (8 - shift))) and 0x7F
                if (escaped) {
                    chars.add(gsm7ExtensionChar(septet))
                    escaped = false
                } else if (septet == 0x1B) {
                    escaped = true
                } else {
                    chars.add(gsm7DefaultChar(septet))
                }
            }
            return chars.joinToString("").trimEnd('@')
        }

        private fun gsm7DefaultChar(value: Int): Char {
            val table = charArrayOf(
                    '@', '\u00A3', '$', '\u00A5', '\u00E8', '\u00E9', '\u00F9', '\u00EC',
                    '\u00F2', '\u00C7', '\n', '\u00D8', '\u00F8', '\r', '\u00C5',
                    '\u00E5', '\u0394', '_', '\u03A6', '\u0393', '\u039B', '\u03A9',
                    '\u03A0', '\u03A8', '\u03A3', '\u0398', '\u039E', '\u001B',
                    '\u00C6', '\u00E6', '\u00DF', '\u00C9', ' ', '!', '"', '#',
                    '\u00A4', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '/',
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ':', ';', '<',
                    '=', '>', '?', '\u00A1', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
                    'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
                    'V', 'W', 'X', 'Y', 'Z', '\u00C4', '\u00D6', '\u00D1', '\u00DC',
                    '\u00A7', '\u00BF', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
                    'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
                    'w', 'x', 'y', 'z', '\u00E4', '\u00F6', '\u00F1', '\u00FC',
                    '\u00E0'
            )
            return table.getOrElse(value) { '?' }
        }

        private fun gsm7ExtensionChar(value: Int): Char = when (value) {
            0x0A -> '\u000C'
            0x14 -> '^'
            0x28 -> '{'
            0x29 -> '}'
            0x2F -> '\\'
            0x3C -> '['
            0x3D -> '~'
            0x3E -> ']'
            0x40 -> '|'
            0x65 -> '\u20AC'
            else -> '?'
        }

    }
}
