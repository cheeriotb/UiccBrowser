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
import com.github.cheeriotb.uiccbrowser.element.Element

// ETSI TS 131 102, clause 4.6.5.
class V2xEfDecoders {
    companion object {
        private val pc5Tags = mapOf(
                0xA0 to R.string.v2x_pc5_policy_data_label,
                0x80 to R.string.served_by_eutra_or_nr_label,
                0x81 to R.string.not_served_by_eutra_or_nr_label,
                0x82 to R.string.tx_profiles_mapping_rules_label,
                0x83 to R.string.privacy_config_label,
                0x84 to R.string.v2x_eutra_pc5_label,
                0x85 to R.string.v2x_nr_pc5_label)
        private val uuTags = mapOf(
                0xA0 to R.string.v2x_uu_policy_data_label,
                0x80 to R.string.pdu_session_mapping_rules_label,
                0x81 to R.string.plmn_infos_label)

        /** Decodes EFVST coding and service availability bits. */
        fun decodeVst(resources: Resources, bytes: ByteArray): Element? =
                TelecomSubDfDecoderUtils.decodeServiceTable(
                        resources, bytes, R.string.ef_vst_label,
                        listOf(
                                R.string.v2x_config_service_label,
                                R.string.v2x_pc5_policy_service_label,
                                R.string.v2x_uu_policy_service_label),
                        R.string.v2x_data_coding_label)

        /** Decodes EFV2XP_PC5 into its outer fields and nested TLV objects. */
        fun decodeV2xpPc5(resources: Resources, bytes: ByteArray): Element? =
                decodePolicy(resources, bytes, R.string.ef_v2xp_pc5_label, pc5Tags)

        /** Decodes EFV2XP_Uu into its outer fields and nested TLV objects. */
        fun decodeV2xpUu(resources: Resources, bytes: ByteArray): Element? =
                decodePolicy(resources, bytes, R.string.ef_v2xp_uu_label, uuTags)

        private fun decodePolicy(
            resources: Resources,
            bytes: ByteArray,
            labelId: Int,
            tagLabels: Map<Int, Int>
        ): Element? =
                TelecomSubDfDecoderUtils.decodeTlvFile(
                        resources, bytes, labelId, 3,
                        listOf(
                                TelecomSubDfDecoderUtils.PrefixField(
                                        -1, R.string.validity_timer_label),
                                TelecomSubDfDecoderUtils.PrefixField(
                                        1, R.string.indicator_bits_label)),
                        tagLabels)
    }
}
