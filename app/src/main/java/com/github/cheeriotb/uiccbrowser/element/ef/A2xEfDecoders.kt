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

// ETSI TS 131 102, clause 4.6.6.
class A2xEfDecoders {
    companion object {
        private val configTags = mapOf(
                0xA0 to R.string.a2x_config_data_label,
                0x80 to R.string.ue_policy_part_contents_label)
        private val pc5Tags = mapOf(
                0xA0 to R.string.a2x_pc5_policy_data_label,
                0x80 to R.string.served_by_ng_ran_label,
                0x81 to R.string.not_served_by_ng_ran_label,
                0x82 to R.string.tx_profiles_mapping_rules_label,
                0x83 to R.string.privacy_config_label,
                0x84 to R.string.a2x_eutra_pc5_label,
                0x85 to R.string.a2x_nr_pc5_label)
        private val ddaapTags = mapOf(
                0xA0 to R.string.a2x_ddaap_pc5_policy_data_label,
                0x80 to R.string.ddaa_unicast_service_identifiers_label,
                0x81 to R.string.ddaa_broadcast_service_identifiers_label)
        private val dc2pTags = mapOf(
                0xA0 to R.string.a2x_dc2p_pc5_policy_data_label,
                0x80 to R.string.served_by_ng_ran_dc2_label,
                0x81 to R.string.not_served_by_ng_ran_dc2_label)
        private val uuTags = mapOf(
                0xA0 to R.string.a2x_uu_policy_data_label,
                0x80 to R.string.pdu_session_mapping_rules_label,
                0x81 to R.string.plmn_infos_label)

        /** Decodes EFAST service availability bits. */
        fun decodeAst(resources: Resources, bytes: ByteArray): Element? =
                TelecomSubDfDecoderUtils.decodeServiceTable(
                        resources, bytes, R.string.ef_ast_label,
                        listOf(
                                R.string.a2x_config_service_label,
                                R.string.a2x_pc5_policy_service_label,
                                R.string.a2x_ddaap_pc5_service_label,
                                R.string.a2x_dc2p_pc5_service_label,
                                R.string.a2x_uu_policy_service_label))

        /** Decodes EFA2X_CONFIG TLV objects. */
        fun decodeA2xConfig(resources: Resources, bytes: ByteArray): Element? =
                decodePolicy(
                        resources, bytes, R.string.ef_a2x_config_label, 4, 1, configTags, true)

        /** Decodes EFA2XP_PC5 TLV objects. */
        fun decodeA2xpPc5(resources: Resources, bytes: ByteArray): Element? =
                decodePolicy(resources, bytes, R.string.ef_a2xp_pc5_label, 11, 6, pc5Tags)

        /** Decodes EFA2X_DDAAP_PC5 TLV objects. */
        fun decodeA2xDdaapPc5(resources: Resources, bytes: ByteArray): Element? =
                decodePolicy(resources, bytes, R.string.ef_a2x_ddaap_pc5_label, 4, 1, ddaapTags)

        /** Decodes EFA2X_DC2P_PC5 TLV objects. */
        fun decodeA2xDc2pPc5(resources: Resources, bytes: ByteArray): Element? =
                decodePolicy(resources, bytes, R.string.ef_a2x_dc2p_pc5_label, 6, 0, dc2pTags)

        /** Decodes EFA2XP_Uu TLV objects. */
        fun decodeA2xpUu(resources: Resources, bytes: ByteArray): Element? =
                decodePolicy(resources, bytes, R.string.ef_a2xp_uu_label, 12, 6, uuTags)

        private fun decodePolicy(
            resources: Resources,
            bytes: ByteArray,
            labelId: Int,
            minimumLength: Int,
            prefixLength: Int,
            tagLabels: Map<Int, Int>,
            allowEmpty: Boolean = false
        ): Element? {
            val prefixes = when (prefixLength) {
                1 -> listOf(TelecomSubDfDecoderUtils.PrefixField(
                        1, R.string.indicator_bits_label))
                6 -> listOf(
                        TelecomSubDfDecoderUtils.PrefixField(5, R.string.validity_timer_label),
                        TelecomSubDfDecoderUtils.PrefixField(1, R.string.indicator_bits_label))
                else -> emptyList()
            }
            return TelecomSubDfDecoderUtils.decodeTlvFile(
                    resources, bytes, labelId, minimumLength, prefixes, tagLabels, allowEmpty)
        }
    }
}
