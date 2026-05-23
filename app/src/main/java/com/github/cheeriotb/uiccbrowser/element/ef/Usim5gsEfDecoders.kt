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
import com.github.cheeriotb.uiccbrowser.element.ConstructedElement
import com.github.cheeriotb.uiccbrowser.element.Element
import com.github.cheeriotb.uiccbrowser.element.PrimitiveElement
import com.github.cheeriotb.uiccbrowser.util.StringUtils
import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString
import java.util.Locale

// ETSI TS 131 102, clause 4.4.11.
class Usim5gsEfDecoders {
    companion object {
        private const val FIVEGS_LOCI_LENGTH = 20
        private const val FIVEGS_NSC_MIN_LENGTH = 57
        private const val FIVEG_AUTH_KEYS_LENGTH = 110
        private const val UAC_AIC_LENGTH = 4
        private const val ROUTING_INDICATOR_LENGTH = 4
        private const val DRI_MIN_LENGTH = 7
        private const val EDRX_MIN_LENGTH = 2
        private const val NSWO_CONF_MIN_LENGTH = 1
        private const val MCHPPLMN_MIN_LENGTH = 1
        private const val KAUSF_DERIVATION_MIN_LENGTH = 1
        private const val CAG_RANGE_ENTRY_LENGTH = 13
        private const val TAG_PROTECTION_SCHEME_LIST = 0xA0
        private const val TAG_PUBLIC_KEY_LIST = 0xA1
        private const val TAG_PUBLIC_KEY_IDENTIFIER = 0x80
        private const val TAG_PUBLIC_KEY = 0x81
        private const val TAG_5GS_NAS_SECURITY_CONTEXT = 0xA0
        private const val TAG_NGKSI = 0x80
        private const val TAG_KAMF = 0x81
        private const val TAG_UPLINK_NAS_COUNT = 0x82
        private const val TAG_DOWNLINK_NAS_COUNT = 0x83
        private const val TAG_5GS_NAS_SECURITY_ALGORITHMS = 0x84
        private const val TAG_UE_SECURITY_CAPABILITY = 0x85
        private const val TAG_PLMN_IDENTIFIER = 0x86
        private const val TAG_KAUSF = 0x80
        private const val TAG_KSEAF_3GPP = 0x81
        private const val TAG_KSEAF_NON_3GPP = 0x82
        private const val TAG_SOR_COUNTER = 0x83
        private const val TAG_UE_PARAMETER_UPDATE_COUNTER = 0x84
        private const val TAG_NAI = 0x80
        private const val TAG_SERVING_NETWORK_NAME = 0x80
        private const val TAG_SOR_CMCI = 0x80
        private const val TAG_DISASTER_PLMN_LIST = 0x80
        private const val PROTECTION_SCHEME_ENTRY_LENGTH = 2
        private const val MIN_SUCI_CALC_INFO_LENGTH = 2
        private const val OPL5G_ENTRY_LENGTH = 10

        private data class RawTlv(
            val tag: Int,
            val value: ByteArray,
            val encoded: ByteArray
        )

        /**
         * Decodes EF 5GS3GPPLOCI into 5GS location information fields.
         */
        fun decode5gs3gppLoci(resources: Resources, bytes: ByteArray): Element? =
                decode5gsLoci(resources, bytes, R.string.ef_5gs3gpp_loci_label)

        /**
         * Decodes EF 5GSN3GPPLOCI into 5GS location information fields.
         */
        fun decode5gsn3gppLoci(resources: Resources, bytes: ByteArray): Element? =
                decode5gsLoci(resources, bytes, R.string.ef_5gsn3gpp_loci_label)

        /**
         * Decodes EF 5GS3GPPNSC into 5GS NAS security context TLV objects.
         */
        fun decode5gs3gppNsc(resources: Resources, bytes: ByteArray): Element? =
                decode5gsNsc(resources, bytes, R.string.ef_5gs3gpp_nsc_label)

        /**
         * Decodes EF 5GSN3GPPNSC into 5GS NAS security context TLV objects.
         */
        fun decode5gsn3gppNsc(resources: Resources, bytes: ByteArray): Element? =
                decode5gsNsc(resources, bytes, R.string.ef_5gsn3gpp_nsc_label)

        /**
         * Decodes EF 5GAUTHKEYS into KAUSF, KSEAF, and counter TLV objects.
         */
        fun decode5gAuthKeys(resources: Resources, bytes: ByteArray): Element? =
                decodeTlvFile(resources, bytes, R.string.ef_5g_auth_keys_label,
                        FIVEG_AUTH_KEYS_LENGTH, ::authKeysDecoder)

        /**
         * Decodes EF UAC_AIC into access identity bits.
         */
        fun decodeUacAic(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != UAC_AIC_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_uac_aic_label)
                    .decoder(::uacAicDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF SUCI_Calc_Info into protection scheme and public key list data objects.
         */
        fun decodeSuciCalcInfo(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < MIN_SUCI_CALC_INFO_LENGTH) return null
            if (parseTlvs(bytes).none { it.tag == TAG_PROTECTION_SCHEME_LIST }) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_suci_calc_info_label)
                    .decoder(::suciCalcInfoDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF SUPI_NAI into the SUPI Network Access Identifier TLV data object.
         */
        fun decodeSupiNai(resources: Resources, bytes: ByteArray): Element? =
                decodeTlvFile(resources, bytes, R.string.ef_supi_nai_label, 1, ::supiNaiDecoder)

        /**
         * Decodes EF Routing_Indicator into routing indicator and RFU fields.
         */
        fun decodeRoutingIndicator(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != ROUTING_INDICATOR_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_routing_indicator_label)
                    .decoder(::routingIndicatorDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF TN3GPPSNN into serving network name TLV data objects.
         */
        fun decodeTn3gppsnn(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty()) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_tn3gppsnn_label)
                    .decoder(::tn3gppsnnDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF CAG into CAG information list entries.
         */
        fun decodeCag(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < 2) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_cag_label)
                    .decoder(::cagDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF SOR-CMCI into SOR-CMCI TLV data objects.
         */
        fun decodeSorCmci(resources: Resources, bytes: ByteArray): Element? =
                decodeTlvFile(resources, bytes, R.string.ef_sor_cmci_label, 1,
                        ::sorCmciDecoder)

        /**
         * Decodes EF DRI into disaster roaming information fields.
         */
        fun decodeDri(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < DRI_MIN_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_dri_label)
                    .decoder(::driDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF 5GSEDRX into 5GS eDRX parameter fields.
         */
        fun decode5gsEdrx(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < EDRX_MIN_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_5gs_edrx_label)
                    .decoder(::edrxDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF 5GNSWO_CONF into the NSWO usage indicator.
         */
        fun decode5gNswoConf(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleElementFile(resources, bytes, R.string.ef_5g_nswo_conf_label,
                        R.string.fiveg_nswo_usage_indicator_label, NSWO_CONF_MIN_LENGTH,
                        ::nswoUsageInterpreter)

        /**
         * Decodes EF MCHPPLMN into a multiplier coefficient.
         */
        fun decodeMchpplmn(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleElementFile(resources, bytes, R.string.ef_mchpplmn_label,
                        R.string.multiplier_coefficient_label, MCHPPLMN_MIN_LENGTH,
                        ::multiplierCoefficientInterpreter)

        /**
         * Decodes EF KAUSF_DERIVATION into KAUSF derivation configuration.
         */
        fun decodeKausfDerivation(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < KAUSF_DERIVATION_MIN_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_kausf_derivation_label)
                    .decoder(::kausfDerivationDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF OPL5G records into TAI and PLMN network name record identifier fields.
         */
        fun decodeOpl5g(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < OPL5G_ENTRY_LENGTH ||
                    bytes.size % OPL5G_ENTRY_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_opl5g_label)
                    .decoder(::opl5gDecoder)
                    .build(resources)
        }

        private fun decode5gsLoci(
            resources: Resources,
            bytes: ByteArray,
            labelId: Int
        ): Element? {
            if (bytes.size != FIVEGS_LOCI_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(labelId)
                    .decoder(::fivegsLociDecoder)
                    .build(resources)
        }

        private fun decode5gsNsc(
            resources: Resources,
            bytes: ByteArray,
            labelId: Int
        ): Element? = decodeTlvFile(resources, bytes, labelId, FIVEGS_NSC_MIN_LENGTH,
                ::fivegsNscDecoder)

        private fun decodeTlvFile(
            resources: Resources,
            bytes: ByteArray,
            labelId: Int,
            minimumLength: Int,
            decoder: (Resources, ByteArray, Element?) -> List<Element>
        ): Element? {
            if (bytes.size < minimumLength) return null
            if (parseTlvs(bytes).isEmpty()) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(labelId)
                    .decoder(decoder)
                    .build(resources)
        }

        private fun decodeSingleElementFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int,
            valueLabelId: Int,
            minimumLength: Int,
            interpreter: (Resources, ByteArray) -> String
        ): Element? {
            if (bytes.size < minimumLength) return null

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

        private fun fivegsLociDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 13))
                        .labelId(R.string.fivegs_guti_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(13, 19))
                        .labelId(R.string.last_visited_registered_tai_5gs_label)
                        .parent(parent)
                        .interpreter(::trackingAreaIdentityInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(19, FIVEGS_LOCI_LENGTH))
                        .labelId(R.string.fivegs_update_status_label)
                        .parent(parent)
                        .interpreter(::fivegsUpdateStatusInterpreter)
                        .build(resources)
        )

        private fun fivegsNscDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = parseTlvs(rawData).map { tlv ->
            when (tlv.tag) {
                TAG_5GS_NAS_SECURITY_CONTEXT -> tlvConstructedElement(
                        resources,
                        tlv,
                        R.string.fivegs_nas_security_context_label,
                        parent,
                        ::fivegsNasSecurityContextDecoder
                )
                else -> genericTlvElement(resources, tlv, parent)
            }
        }

        private fun fivegsNasSecurityContextDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = parseTlvs(rawData).map { tlv ->
            val labelId = when (tlv.tag) {
                TAG_NGKSI -> R.string.key_set_identifier_ngksi_label
                TAG_KAMF -> R.string.kamf_label
                TAG_UPLINK_NAS_COUNT -> R.string.uplink_nas_count_5gs_label
                TAG_DOWNLINK_NAS_COUNT -> R.string.downlink_nas_count_5gs_label
                TAG_5GS_NAS_SECURITY_ALGORITHMS ->
                        R.string.fivegs_nas_security_algorithms_label
                TAG_UE_SECURITY_CAPABILITY -> R.string.ue_security_capability_label
                TAG_PLMN_IDENTIFIER -> R.string.plmn_label
                else -> R.string.unknown_label
            }
            val interpreter = when (tlv.tag) {
                TAG_NGKSI, TAG_UPLINK_NAS_COUNT, TAG_DOWNLINK_NAS_COUNT ->
                        ::unsignedIntegerInterpreter
                TAG_PLMN_IDENTIFIER -> ::plmnInterpreter
                else -> PrimitiveElement.Companion::defaultInterpreter
            }
            valueTlvElement(resources, tlv, labelId, parent, interpreter)
        }

        private fun authKeysDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = parseTlvs(rawData).map { tlv ->
            val labelId = when (tlv.tag) {
                TAG_KAUSF -> R.string.kausf_label
                TAG_KSEAF_3GPP -> R.string.kseaf_3gpp_access_label
                TAG_KSEAF_NON_3GPP -> R.string.kseaf_non_3gpp_access_label
                TAG_SOR_COUNTER -> R.string.sor_counter_label
                TAG_UE_PARAMETER_UPDATE_COUNTER -> R.string.ue_parameter_update_counter_label
                else -> R.string.unknown_label
            }
            val interpreter = when (tlv.tag) {
                TAG_SOR_COUNTER, TAG_UE_PARAMETER_UPDATE_COUNTER -> ::unsignedIntegerInterpreter
                else -> PrimitiveElement.Companion::defaultInterpreter
            }
            valueTlvElement(resources, tlv, labelId, parent, interpreter)
        }

        private fun uacAicDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val header = PrimitiveElement.Builder(rawData)
                    .labelId(R.string.uac_access_identities_configuration_label)
                    .parent(parent)
                    .interpreter(::accessIdentityBitmapInterpreter)
                    .build(resources)
            val identities = (1..32).map { number ->
                val bitSet = (rawData[(number - 1) / 8].toInt() and
                        (1 shl ((number - 1) % 8))) != 0
                PrimitiveElement.Builder(byteArrayOf(if (bitSet) 0x01 else 0x00))
                        .labelId(R.string.access_identity_label)
                        .labelArgs(number)
                        .parent(parent)
                        .interpreter(::presentAbsentInterpreter)
                        .build(resources)
            }
            return listOf(header) + identities
        }

        private fun suciCalcInfoDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return parseTlvs(rawData).mapNotNull { tlv ->
                when (tlv.tag) {
                    TAG_PROTECTION_SCHEME_LIST -> protectionSchemeListElement(
                            resources,
                            tlv,
                            parent
                    )
                    TAG_PUBLIC_KEY_LIST -> publicKeyListElement(resources, tlv, parent)
                    else -> null
                }
            }
        }

        private fun protectionSchemeListElement(
            resources: Resources,
            tlv: RawTlv,
            parent: Element?
        ): Element {
            return ConstructedElement.Builder(tlv.encoded)
                    .labelId(R.string.protection_scheme_identifier_list_label)
                    .parent(parent)
                    .decoder { innerResources, encoded, elementParent ->
                        val value = parseTlvs(encoded).firstOrNull()?.value ?: byteArrayOf()
                        protectionSchemeListDecoder(innerResources, value, elementParent)
                    }
                    .dataComposer { elements ->
                        encodeTlv(
                                TAG_PROTECTION_SCHEME_LIST,
                                elements.fold(byteArrayOf()) { acc, element -> acc + element.data }
                        )
                    }
                    .interpreter { _, _ -> "" }
                    .build(resources)
        }

        private fun protectionSchemeListDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.asIterable().chunked(PROTECTION_SCHEME_ENTRY_LENGTH).mapIndexed {
                index, entry ->
                    ConstructedElement.Builder(entry.toByteArray())
                            .labelId(R.string.protection_scheme_entry_label)
                            .labelArgs(index + 1)
                            .parent(parent)
                            .decoder(::protectionSchemeEntryDecoder)
                            .interpreter { _, _ -> (index + 1).toString() }
                            .build(resources)
            }
        }

        private fun protectionSchemeEntryDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            if (rawData.size != PROTECTION_SCHEME_ENTRY_LENGTH) return listOf()

            return listOf(
                    PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                            .labelId(R.string.protection_scheme_identifier_label)
                            .parent(parent)
                            .interpreter(::unsignedIntegerInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(1, PROTECTION_SCHEME_ENTRY_LENGTH))
                            .labelId(R.string.key_index_label)
                            .parent(parent)
                            .interpreter(::unsignedIntegerInterpreter)
                            .build(resources)
            )
        }

        private fun publicKeyListElement(
            resources: Resources,
            tlv: RawTlv,
            parent: Element?
        ): Element {
            return ConstructedElement.Builder(tlv.encoded)
                    .labelId(R.string.home_network_public_key_list_label)
                    .parent(parent)
                    .decoder { innerResources, encoded, elementParent ->
                        val value = parseTlvs(encoded).firstOrNull()?.value ?: byteArrayOf()
                        publicKeyListDecoder(innerResources, value, elementParent)
                    }
                    .dataComposer { elements ->
                        encodeTlv(
                                TAG_PUBLIC_KEY_LIST,
                                elements.fold(byteArrayOf()) { acc, element -> acc + element.data }
                        )
                    }
                    .interpreter { _, _ -> "" }
                    .build(resources)
        }

        private fun publicKeyListDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return parseTlvs(rawData).map { tlv ->
                val labelId = when (tlv.tag) {
                    TAG_PUBLIC_KEY_IDENTIFIER -> R.string.home_network_public_key_identifier_label
                    TAG_PUBLIC_KEY -> R.string.home_network_public_key_label
                    else -> R.string.unknown_label
                }
                ConstructedElement.Builder(tlv.encoded)
                        .labelId(labelId)
                        .parent(parent)
                        .decoder { innerResources, encoded, elementParent ->
                            val value = parseTlvs(encoded).firstOrNull()?.value ?: byteArrayOf()
                            listOf(
                                    PrimitiveElement.Builder(value)
                                            .labelId(labelId)
                                            .parent(elementParent)
                                            .build(innerResources)
                            )
                        }
                        .dataComposer { elements ->
                            encodeTlv(tlv.tag, elements.firstOrNull()?.data ?: byteArrayOf())
                        }
                        .interpreter { _, _ -> "" }
                        .build(resources)
            }
        }

        private fun supiNaiDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = parseTlvs(rawData).map { tlv ->
            val labelId = if (tlv.tag == TAG_NAI) {
                R.string.network_access_identifier_label
            } else {
                R.string.unknown_label
            }
            valueTlvElement(resources, tlv, labelId, parent, ::utf8StringInterpreter)
        }

        private fun routingIndicatorDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 2))
                        .labelId(R.string.routing_indicator_label)
                        .parent(parent)
                        .interpreter(::routingIndicatorInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(2, ROUTING_INDICATOR_LENGTH))
                        .labelId(R.string.rfu_label)
                        .parent(parent)
                        .build(resources)
        )

        private fun tn3gppsnnDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val count = rawData.first().toInt() and 0xFF
            val elements = mutableListOf<Element>(
                    PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                            .labelId(R.string.serving_network_name_count_label)
                            .parent(parent)
                            .interpreter(::unsignedIntegerInterpreter)
                            .build(resources)
            )
            parseTlvs(rawData.copyOfRange(1, rawData.size)).take(count).forEachIndexed {
                index, tlv ->
                    val labelId = if (tlv.tag == TAG_SERVING_NETWORK_NAME) {
                        R.string.serving_network_name_label
                    } else {
                        R.string.unknown_label
                    }
                    elements.add(valueTlvElement(resources, tlv, labelId, parent,
                            ::utf8StringInterpreter, index + 1))
            }
            return elements
        }

        private fun cagDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val length = ((rawData[0].toInt() and 0xFF) shl 8) or
                    (rawData[1].toInt() and 0xFF)
            val elements = mutableListOf<Element>(
                    PrimitiveElement.Builder(rawData.copyOfRange(0, 2))
                            .labelId(R.string.cag_information_list_length_label)
                            .parent(parent)
                            .interpreter(::unsignedIntegerInterpreter)
                            .build(resources)
            )
            var index = 2
            var entryNumber = 1
            val end = minOf(rawData.size, 2 + length)
            while (index < end) {
                val entryLength = rawData[index].toInt() and 0xFF
                if (entryLength == 0 || index + entryLength > rawData.size) break
                val entry = rawData.copyOfRange(index, index + entryLength)
                elements.add(cagEntryElement(resources, entry, entryNumber++, parent))
                index += entryLength
            }
            if (end < rawData.size) {
                elements.add(
                        PrimitiveElement.Builder(rawData.copyOfRange(end, rawData.size))
                                .labelId(R.string.rfu_label)
                                .parent(parent)
                                .build(resources)
                )
            }
            return elements
        }

        private fun cagEntryElement(
            resources: Resources,
            entry: ByteArray,
            index: Int,
            parent: Element?
        ): Element = ConstructedElement.Builder(entry)
                .labelId(R.string.cag_information_entry_label)
                .labelArgs(index)
                .parent(parent)
                .decoder(::cagEntryDecoder)
                .interpreter { _, _ -> index.toString() }
                .build(resources)

        private fun cagEntryDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val elements = mutableListOf<Element>(
                    PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                            .labelId(R.string.cag_entry_length_label)
                            .parent(parent)
                            .interpreter(::unsignedIntegerInterpreter)
                            .build(resources)
            )
            if (rawData.size == CAG_RANGE_ENTRY_LENGTH) {
                elements.add(
                        PrimitiveElement.Builder(rawData.copyOfRange(1, 4))
                                .labelId(R.string.plmn_label)
                                .parent(parent)
                                .interpreter(::plmnInterpreter)
                                .build(resources)
                )
                elements.add(
                        PrimitiveElement.Builder(rawData.copyOfRange(4, 5))
                                .labelId(R.string.cag_only_indication_label)
                                .parent(parent)
                                .interpreter(::cagOnlyInterpreter)
                                .build(resources)
                )
                elements.add(
                        PrimitiveElement.Builder(rawData.copyOfRange(5, 9))
                                .labelId(R.string.cag_id_start_label)
                                .parent(parent)
                                .interpreter(::unsignedIntegerInterpreter)
                                .build(resources)
                )
                elements.add(
                        PrimitiveElement.Builder(rawData.copyOfRange(9, CAG_RANGE_ENTRY_LENGTH))
                                .labelId(R.string.cag_id_end_label)
                                .parent(parent)
                                .interpreter(::unsignedIntegerInterpreter)
                                .build(resources)
                )
            } else if (rawData.size > 1) {
                elements.add(
                        PrimitiveElement.Builder(rawData.copyOfRange(1, rawData.size))
                                .labelId(R.string.discretionary_data_label)
                                .parent(parent)
                                .build(resources)
                )
            }
            return elements
        }

        private fun sorCmciDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = parseTlvs(rawData).map { tlv ->
            val labelId = if (tlv.tag == TAG_SOR_CMCI) {
                R.string.sor_cmci_parameters_label
            } else {
                R.string.unknown_label
            }
            valueTlvElement(resources, tlv, labelId, parent)
        }

        private fun opl5gDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.asIterable().chunked(OPL5G_ENTRY_LENGTH).mapIndexed { index, entry ->
                ConstructedElement.Builder(entry.toByteArray())
                        .labelId(R.string.operator_plmn_list_entry_label)
                        .labelArgs(index + 1)
                        .parent(parent)
                        .decoder(::opl5gEntryDecoder)
                        .interpreter { _, _ -> (index + 1).toString() }
                        .build(resources)
            }
        }

        private fun opl5gEntryDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 3))
                        .labelId(R.string.plmn_label)
                        .parent(parent)
                        .interpreter(::plmnInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(3, 6))
                        .labelId(R.string.start_tac_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(6, 9))
                        .labelId(R.string.end_tac_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(9, OPL5G_ENTRY_LENGTH))
                        .labelId(R.string.plmn_network_name_record_identifier_label)
                        .parent(parent)
                        .interpreter(::recordIdentifierInterpreter)
                        .build(resources)
        )

        private fun driDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val elements = mutableListOf<Element>(
                    PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                            .labelId(R.string.disaster_roaming_enabled_label)
                            .parent(parent)
                            .interpreter(::enabledInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(1, 2))
                            .labelId(R.string.disaster_roaming_parameters_indicator_label)
                            .parent(parent)
                            .interpreter(::disasterParameterIndicatorInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(2, 4))
                            .labelId(R.string.disaster_roaming_wait_range_label)
                            .parent(parent)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(4, 6))
                            .labelId(R.string.disaster_return_wait_range_label)
                            .parent(parent)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(6, 7))
                            .labelId(R.string.disaster_vplmn_list_applicability_label)
                            .parent(parent)
                            .interpreter(::unsignedIntegerInterpreter)
                            .build(resources)
            )
            if (rawData.size > DRI_MIN_LENGTH) {
                val remaining = rawData.copyOfRange(DRI_MIN_LENGTH, rawData.size)
                val tlvs = parseTlvs(remaining)
                if (tlvs.size == 1 && tlvs.first().tag == TAG_DISASTER_PLMN_LIST) {
                    elements.add(disasterPlmnListElement(resources, tlvs.first(), parent))
                } else {
                    elements.add(
                            PrimitiveElement.Builder(remaining)
                                    .labelId(R.string.disaster_hplmn_plmn_list_label)
                                    .parent(parent)
                                    .build(resources)
                    )
                }
            }
            return elements
        }

        private fun disasterPlmnListElement(
            resources: Resources,
            tlv: RawTlv,
            parent: Element?
        ): Element = tlvConstructedElement(resources, tlv,
                R.string.disaster_hplmn_plmn_list_label, parent) {
            innerResources, value, elementParent ->
                value.asIterable().chunked(3).mapIndexed { index, plmn ->
                    PrimitiveElement.Builder(plmn.toByteArray())
                            .labelId(R.string.plmn_number_label)
                            .labelArgs(index + 1)
                            .parent(elementParent)
                            .interpreter(::plmnInterpreter)
                            .build(innerResources)
                }
        }

        private fun edrxDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val elements = mutableListOf(
                    PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                            .labelId(R.string.fivegs_rat_type_label)
                            .parent(parent)
                            .interpreter(::ratTypeInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(1, 2))
                            .labelId(R.string.edrx_cycle_length_label)
                            .parent(parent)
                            .build(resources)
            )
            if (rawData.size > EDRX_MIN_LENGTH) {
                elements.add(
                        PrimitiveElement.Builder(rawData.copyOfRange(EDRX_MIN_LENGTH, rawData.size))
                                .labelId(R.string.rfu_label)
                                .parent(parent)
                                .build(resources)
                )
            }
            return elements
        }

        private fun kausfDerivationDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val elements = mutableListOf(
                    PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                            .labelId(R.string.kausf_derivation_configuration_label)
                            .parent(parent)
                            .interpreter(::kausfDerivationInterpreter)
                            .build(resources)
            )
            if (rawData.size > KAUSF_DERIVATION_MIN_LENGTH) {
                elements.add(
                        PrimitiveElement.Builder(rawData.copyOfRange(
                                KAUSF_DERIVATION_MIN_LENGTH,
                                rawData.size
                        ))
                                .labelId(R.string.rfu_label)
                                .parent(parent)
                                .build(resources)
                )
            }
            return elements
        }

        private fun unsignedIntegerInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) }
            return resources.getString(
                    R.string.hex_with_description,
                    byteArrayToHexString(rawData),
                    value.toString()
            )
        }

        private fun plmnInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size != 3) return byteArrayToHexString(rawData)

            val b1 = rawData[0].toInt() and 0xFF
            val b2 = rawData[1].toInt() and 0xFF
            val b3 = rawData[2].toInt() and 0xFF
            val mcc = "${nibbleToPlmnDigit(b1 and 0x0F)}" +
                    "${nibbleToPlmnDigit((b1 ushr 4) and 0x0F)}" +
                    nibbleToPlmnDigit(b2 and 0x0F)
            val mncDigit3 = (b2 ushr 4) and 0x0F
            val mnc = "${nibbleToPlmnDigit(b3 and 0x0F)}" +
                    "${nibbleToPlmnDigit((b3 ushr 4) and 0x0F)}" +
                    if (mncDigit3 == 0x0F) "" else nibbleToPlmnDigit(mncDigit3)
            return resources.getString(
                    R.string.hex_with_description,
                    byteArrayToHexString(rawData),
                    resources.getString(R.string.plmn_interpretation, mcc, mnc)
            )
        }

        private fun recordIdentifierInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val description = when (value) {
                0x00 -> resources.getString(R.string.name_taken_from_other_sources)
                0xFF -> resources.getString(R.string.rfu_label)
                else -> value.toString()
            }
            return resources.getString(
                    R.string.hex_with_description,
                    byteArrayToHexString(rawData),
                    description
            )
        }

        private fun tlvConstructedElement(
            resources: Resources,
            tlv: RawTlv,
            labelId: Int,
            parent: Element?,
            decoder: (Resources, ByteArray, Element?) -> List<Element>
        ): Element = ConstructedElement.Builder(tlv.encoded)
                .labelId(labelId)
                .parent(parent)
                .decoder { innerResources, encoded, elementParent ->
                    val value = parseTlvs(encoded).firstOrNull()?.value ?: byteArrayOf()
                    decoder(innerResources, value, elementParent)
                }
                .dataComposer { elements ->
                    encodeTlv(tlv.tag, elements.fold(byteArrayOf()) { acc, element ->
                        acc + element.data
                    })
                }
                .interpreter { _, _ -> "" }
                .build(resources)

        private fun valueTlvElement(
            resources: Resources,
            tlv: RawTlv,
            labelId: Int,
            parent: Element?,
            interpreter: (Resources, ByteArray) -> String =
                    PrimitiveElement.Companion::defaultInterpreter,
            labelArg: Int? = null
        ): Element = ConstructedElement.Builder(tlv.encoded)
                .labelId(labelId)
                .apply { if (labelArg != null) labelArgs(labelArg) }
                .parent(parent)
                .decoder { innerResources, encoded, elementParent ->
                    val value = parseTlvs(encoded).firstOrNull()?.value ?: byteArrayOf()
                    listOf(
                            PrimitiveElement.Builder(value)
                                    .labelId(labelId)
                                    .apply { if (labelArg != null) labelArgs(labelArg) }
                                    .parent(elementParent)
                                    .interpreter(interpreter)
                                    .build(innerResources)
                    )
                }
                .dataComposer { elements ->
                    encodeTlv(tlv.tag, elements.firstOrNull()?.data ?: byteArrayOf())
                }
                .interpreter { _, _ -> "" }
                .build(resources)

        private fun genericTlvElement(
            resources: Resources,
            tlv: RawTlv,
            parent: Element?
        ): Element = valueTlvElement(resources, tlv, R.string.unknown_label, parent)

        private fun trackingAreaIdentityInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size != 6) return byteArrayToHexString(rawData)

            val plmn = plmnInterpreter(resources, rawData.copyOfRange(0, 3))
            val tac = byteArrayToHexString(rawData.copyOfRange(3, 6))
            return resources.getString(
                    R.string.hex_with_description,
                    byteArrayToHexString(rawData),
                    "$plmn, TAC $tac"
            )
        }

        private fun fivegsUpdateStatusInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val status = when (rawData.firstOrNull()?.toInt()?.and(0x07)) {
                0x00 -> resources.getString(R.string.fivegs_update_status_registered)
                0x01 -> resources.getString(R.string.fivegs_update_status_not_registered)
                0x02 -> resources.getString(R.string.fivegs_update_status_registration_not_allowed)
                0x03 -> resources.getString(
                        R.string.fivegs_update_status_registered_non_allowed_area)
                0x07 -> resources.getString(R.string.reserved_label)
                else -> resources.getString(R.string.rfu_label)
            }
            return resources.getString(
                    R.string.hex_with_description,
                    byteArrayToHexString(rawData),
                    status
            )
        }

        private fun utf8StringInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val decoded = StringUtils.decode(rawData)
            return resources.getString(
                    R.string.hex_with_description,
                    byteArrayToHexString(rawData),
                    decoded
            )
        }

        private fun routingIndicatorInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val digits = rawData.flatMap { byte ->
                listOf(byte.toInt() and 0x0F, (byte.toInt() ushr 4) and 0x0F)
            }.filter { it != 0x0F }.joinToString("") { it.toString(16).uppercase(Locale.US) }
            return resources.getString(
                    R.string.hex_with_description,
                    byteArrayToHexString(rawData),
                    digits
            )
        }

        private fun accessIdentityBitmapInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val identities = (1..32).filter { number ->
                (rawData[(number - 1) / 8].toInt() and (1 shl ((number - 1) % 8))) != 0
            }
            val description = if (identities.isEmpty()) {
                resources.getString(R.string.absent_label)
            } else {
                identities.joinToString(", ")
            }
            return resources.getString(
                    R.string.hex_with_description,
                    byteArrayToHexString(rawData),
                    description
            )
        }

        private fun presentAbsentInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val present = (rawData.firstOrNull()?.toInt()?.and(0x01) ?: 0) != 0
            return resources.getString(
                    R.string.hex_with_description,
                    byteArrayToHexString(rawData),
                    resources.getString(
                            if (present) R.string.present_label else R.string.absent_label
                    )
            )
        }

        private fun enabledInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val description = when (value) {
                0x00 -> resources.getString(R.string.disabled_label)
                0x01 -> resources.getString(R.string.enabled_label)
                else -> resources.getString(R.string.rfu_label)
            }
            return resources.getString(
                    R.string.hex_with_description,
                    byteArrayToHexString(rawData),
                    description
            )
        }

        private fun nswoUsageInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String = enabledInterpreter(resources, rawData.copyOfRange(0, 1))

        private fun multiplierCoefficientInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val description = if (value == 0) {
                resources.getString(R.string.not_available_label)
            } else {
                value.toString()
            }
            return resources.getString(
                    R.string.hex_with_description,
                    byteArrayToHexString(rawData),
                    description
            )
        }

        private fun cagOnlyInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val enabled = (rawData.firstOrNull()?.toInt()?.and(0x01) ?: 0) != 0
            return resources.getString(
                    R.string.hex_with_description,
                    byteArrayToHexString(rawData),
                    resources.getString(
                            if (enabled) R.string.enabled_label else R.string.disabled_label
                    )
            )
        }

        private fun disasterParameterIndicatorInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val descriptions = (1..4).map { bit ->
                val present = (value and (1 shl (bit - 1))) == 0
                "$bit:${resources.getString(
                        if (present) R.string.present_label else R.string.absent_label)}"
            }
            return resources.getString(
                    R.string.hex_with_description,
                    byteArrayToHexString(rawData),
                    descriptions.joinToString(", ")
            )
        }

        private fun ratTypeInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val descriptions = mutableListOf<String>()
            if (value and 0x01 != 0) descriptions.add(resources.getString(R.string.ng_ran_label))
            if (value and 0x02 != 0) {
                descriptions.add(resources.getString(R.string.satellite_ng_ran_label))
            }
            if (descriptions.isEmpty()) descriptions.add(resources.getString(R.string.rfu_label))
            return resources.getString(
                    R.string.hex_with_description,
                    byteArrayToHexString(rawData),
                    descriptions.joinToString(", ")
            )
        }

        private fun kausfDerivationInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val useMsk = (rawData.firstOrNull()?.toInt()?.and(0x01) ?: 0) != 0
            return resources.getString(
                    R.string.hex_with_description,
                    byteArrayToHexString(rawData),
                    resources.getString(
                            if (useMsk) R.string.kausf_derivation_use_msk
                            else R.string.kausf_derivation_use_emsk
                    )
            )
        }

        private fun parseTlvs(bytes: ByteArray): List<RawTlv> {
            val tlvs = mutableListOf<RawTlv>()
            var index = 0
            try {
                while (index < bytes.size) {
                    if ((bytes[index].toInt() and 0xFF) == 0xFF &&
                            (index until bytes.size).all { bytes[it].toInt() and 0xFF == 0xFF }) {
                        break
                    }
                    val start = index
                    var tag = bytes[index++].toInt() and 0xFF
                    if (tag and 0x1F == 0x1F) {
                        tag = (tag shl 8) or (bytes[index++].toInt() and 0xFF)
                        while (tag and 0x80 == 0x80) {
                            tag = (tag shl 8) or (bytes[index++].toInt() and 0xFF)
                        }
                    }
                    val length = readLength(bytes, index)
                    index += length.byteCount
                    val value = bytes.copyOfRange(index, index + length.value)
                    index += length.value
                    tlvs.add(RawTlv(tag, value, bytes.copyOfRange(start, index)))
                }
            } catch (_: RuntimeException) {
                return listOf()
            }
            return tlvs
        }

        private data class TlvLength(
            val value: Int,
            val byteCount: Int
        )

        private fun readLength(bytes: ByteArray, offset: Int): TlvLength {
            val first = bytes[offset].toInt() and 0xFF
            if (first <= 0x7F) return TlvLength(first, 1)

            val count = first and 0x7F
            var value = 0
            repeat(count) { index ->
                value = (value shl 8) or (bytes[offset + 1 + index].toInt() and 0xFF)
            }
            return TlvLength(value, count + 1)
        }

        private fun encodeTlv(tag: Int, value: ByteArray): ByteArray {
            return tag.toHexByteArray() + encodeLength(value.size) + value
        }

        private fun Int.toHexByteArray(): ByteArray {
            val hex = when {
                this <= 0xFF -> "%02X"
                this <= 0xFFFF -> "%04X"
                else -> "%06X"
            }.format(this)
            return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }

        private fun encodeLength(length: Int): ByteArray {
            return when (length) {
                in 0x00..0x7F -> byteArrayOf(length.toByte())
                in 0x80..0xFF -> byteArrayOf(0x81.toByte(), length.toByte())
                else -> byteArrayOf(
                        0x82.toByte(),
                        ((length ushr 8) and 0xFF).toByte(),
                        (length and 0xFF).toByte()
                )
            }
        }

        private fun nibbleToPlmnDigit(nibble: Int): Char {
            return if (nibble == 0x0D) {
                'D'
            } else if (nibble in 0..9) {
                ('0'.code + nibble).toChar()
            } else {
                nibble.toString(16).uppercase(Locale.US)[0]
            }
        }
    }
}
