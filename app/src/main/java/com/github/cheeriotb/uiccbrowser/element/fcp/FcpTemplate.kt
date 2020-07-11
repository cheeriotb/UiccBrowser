/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.element.fcp

import android.content.res.Resources
import com.github.cheeriotb.uiccbrowser.R
import com.github.cheeriotb.uiccbrowser.element.BerTlvElement
import com.github.cheeriotb.uiccbrowser.element.Element
import com.github.cheeriotb.uiccbrowser.element.PrimitiveElement
import com.github.cheeriotb.uiccbrowser.util.BerTlv
import com.github.cheeriotb.uiccbrowser.util.Tlv

class FcpTemplate {
    companion object {
        const val TAG_FCP_TEMPLATE = 0x62

        fun decode(resources: Resources, bytes: ByteArray): BerTlvElement? {
            val tlvs = BerTlv.listFrom(bytes)
            if (tlvs.isEmpty() || tlvs[0].tlvs.isEmpty()
                    || tlvs[0].tag != TAG_FCP_TEMPLATE) return null

            return BerTlvElement.Builder(tlvs[0])
                    .labelId(R.string.fcp_template_label)
                    .decoder(::fcpTemplateDecoder)
                    .build(resources)
        }

        // The elements which can exist in the first level of FCP template structure.
        const val TAG_FILE_SIZE = 0x80
        const val TAG_TOTAL_FILE_SIZE = 0x81
        const val TAG_FILE_DESCRIPTOR = 0x82
        const val TAG_FILE_IDENTIFIER = 0x83
        const val TAG_DF_NAME_AID = 0x84
        const val TAG_SHORT_FILE_IDENTIFIER = 0x88
        const val TAG_LIFE_CYCLE_STATUS = 0x8A
        const val TAG_SECURITY_ATTR_REF = 0x8B
        const val TAG_SECURITY_ATTR_COMPACT = 0x8C
        const val TAG_PROPRIETARY_INFORMATION = 0xA5
        const val TAG_SECURITY_ATTR_EXPAND = 0xAB
        const val TAG_PIN_STATUS_TEMPLATE = 0xC6

        // ETSI TS 102.221 Clause 11.1.1.3
        // Response Data
        private fun fcpTemplateDecoder(
            resources: Resources,
            tlvs: List<Tlv>,
            parent: Element?
        ): List<Element> {
            val list: MutableList<Element> = mutableListOf()

            tlvs.forEach { tlv ->
                when (tlv.tag) {
                    // ETSI TS 102.221 Clause 11.1.1.4.1
                    // File size
                    TAG_FILE_SIZE -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.file_size_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // ETSI TS 102.221 Clause 11.1.1.4.2
                    // Total file size
                    TAG_TOTAL_FILE_SIZE -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.total_file_size_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // ETSI TS 102.221 Clause 11.1.1.4.3
                    // File descriptor
                    TAG_FILE_DESCRIPTOR -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.file_descriptor_label)
                                .parent(parent)
                                .separator(::fileDescriptorSeparator)
                                .build(resources))
                    }
                    // ETSI TS 102.221 Clause 11.1.1.4.4
                    // File identifier
                    TAG_FILE_IDENTIFIER -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.file_identifier_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // ETSI TS 102.221 Clause 11.1.1.4.5
                    // DF name (AID)
                    TAG_DF_NAME_AID -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.df_name_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // ETSI TS 102.221 Clause 11.1.1.4.8
                    // Short file identifier
                    TAG_SHORT_FILE_IDENTIFIER -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.short_file_identifier_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // ETSI TS 102.221 Clause 11.1.1.4.9
                    // Life cycle status integer
                    TAG_LIFE_CYCLE_STATUS -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.total_file_size_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // ETSI TS 102 221 Clause 11.1.1.4.7.3
                    // Security Attribute - Referenced to expanded format
                    TAG_SECURITY_ATTR_REF -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.security_attr_ref_label)
                                .parent(parent)
                                .separator(::securityAttrRefSeparator)
                                .build(resources))
                    }
                    // ETSI TS 102 221 Clause 11.1.1.4.7.1
                    // Security Attribute - Compact format
                    TAG_SECURITY_ATTR_COMPACT -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.security_attr_compact_label)
                                .parent(parent)
                                .separator(::securityAttrCompactSeparator)
                                .build(resources))
                    }
                    // ETSI TS 102.221 Clause 11.1.1.4.6
                    // Proprietary information
                    TAG_PROPRIETARY_INFORMATION -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.proprietary_information_label)
                                .parent(parent)
                                .decoder(::proprietaryInfoDecoder)
                                .build(resources))
                    }
                    // ETSI TS 102 221 Clause 11.1.1.4.7.2
                    // Security Attribute - Expanded format
                    SecurityAttrExpanded.TAG -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(SecurityAttrExpanded.LABEL)
                                .parent(parent)
                                .decoder(SecurityAttrExpanded.decoder)
                                .build(resources))
                    }
                    // ETSI TS 102 221 Clause 11.1.1.4.10
                    // PIN status template DO
                    TAG_PIN_STATUS_TEMPLATE -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.pin_status_template_label)
                                .parent(parent)
                                .separator(::pinStatusTemplateSeparator)
                                .build(resources))
                    }
                    // Additional TLV objects may be present.
                    else -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .parent(parent)
                                .build(resources))
                    }
                }
            }

            return list
        }

        // ETSI TS 102.221 Clause 11.1.1.4.3
        // File descriptor
        private fun fileDescriptorSeparator(
            resources: Resources,
            value: ByteArray,
            parent: Element?
        ): List<Element> {
            if (value.size != 2 && value.size < 5) return listOf()

            val list: MutableList<Element> = mutableListOf()

            list.add(PrimitiveElement.Builder(byteArrayOf(value[0]))
                    .labelId(R.string.file_descriptor_byte_label)
                    .parent(parent)
                    .build(resources))
            list.add(PrimitiveElement.Builder(byteArrayOf(value[1]))
                    .labelId(R.string.data_coding_byte_label)
                    .parent(parent)
                    .build(resources))
            if (value.size > 2) {
                list.add(PrimitiveElement.Builder(value.sliceArray(2..3))
                        .labelId(R.string.record_length_label)
                        .parent(parent)
                        .build(resources))
                list.add(PrimitiveElement.Builder(byteArrayOf(value[4]))
                        .labelId(R.string.number_of_records_label)
                        .parent(parent)
                        .build(resources))
                if (value.size > 5) {
                    list.add(PrimitiveElement.Builder(value.sliceArray(IntRange(5, value.size - 1)))
                            .labelId(R.string.unknown_label)
                            .parent(parent)
                            .build(resources))
                }
            }

            return list
        }

        // ETSI TS 102 221 Clause 11.1.1.4.7.3
        // Security Attribute - Referenced to expanded format
        private fun securityAttrRefSeparator(
            resources: Resources,
            value: ByteArray,
            parent: Element?
        ): List<Element> {
            if (value.size < 3 || (value.size != 3 && (value.size % 2 == 1))) return listOf()

            val list: MutableList<Element> = mutableListOf()

            list.add(PrimitiveElement.Builder(value.sliceArray(0..1))
                    .labelId(R.string.ef_arr_file_id_label)
                    .parent(parent)
                    .build(resources))

            if (value.size != 3) {
                for (index in IntRange(2, value.size - 1) step 2) {
                    list.add(PrimitiveElement.Builder(byteArrayOf(value[index]))
                            .labelId(R.string.seid_label)
                            .parent(parent)
                            .build(resources))
                    list.add(PrimitiveElement.Builder(byteArrayOf(value[index + 1]))
                            .labelId(R.string.ef_arr_record_number_label)
                            .parent(parent)
                            .build(resources))
                }
            } else {
                list.add(PrimitiveElement.Builder(byteArrayOf(value[2]))
                        .labelId(R.string.ef_arr_record_number_label)
                        .parent(parent)
                        .build(resources))
            }

            return list
        }

        // ETSI TS 102 221 Clause 11.1.1.4.7.1
        // Security Attribute - Compact format
        private fun securityAttrCompactSeparator(
            resources: Resources,
            value: ByteArray,
            parent: Element?
        ): List<Element> {
            if (value.isEmpty() || value.size > 8) return listOf()

            val list: MutableList<Element> = mutableListOf()

            list.add(PrimitiveElement.Builder(byteArrayOf(value[0]))
                    .labelId(R.string.am_byte_label)
                    .parent(parent)
                    .build(resources))
            list.add(PrimitiveElement.Builder(value.sliceArray(IntRange(1, value.size - 1)))
                    .labelId(R.string.sc_bytes_label)
                    .parent(parent)
                    .build(resources))

            return list
        }

        // Elements which can exist in PIN status template DO
        const val TAG_PS_DO = 0x90
        const val TAG_USAGE_QUALIFIER_DO = 0x95
        const val TAG_KEY_REFERENCE = 0x83

        // ETSI TS 102 221 Clause 11.1.1.4.10
        // PIN status template DO
        private fun pinStatusTemplateSeparator(
            resources: Resources,
            value: ByteArray,
            parent: Element?
        ): List<Element> {
            val list: MutableList<Element> = mutableListOf()
            val tlvs = BerTlv.listFrom(value)

            tlvs.forEach { tlv ->
                when (tlv.tag) {
                    // ETSI TS 102 221 Clause 9.5.2
                    // PIN status DO
                    TAG_PS_DO -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.ps_do_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // ETSI TS 102 221 Clause 9.5.2
                    // Usage qualifier DO
                    TAG_USAGE_QUALIFIER_DO -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.usage_qualifier_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // ETSI TS 102 221 Table 9.3
                    // Key reference
                    TAG_KEY_REFERENCE -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.key_reference_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // Additional TLV objects may be present.
                    else -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .parent(parent)
                                .build(resources))
                    }
                }
            }

            return list
        }

        // Elements which can exist in Proprietary information
        const val TAG_UICC_CHARACTERISTICS = 0x80
        const val TAG_APP_POWER_CONSUMPTION = 0x81
        const val TAG_MIN_APP_CLOCK_FREQ = 0x82
        const val TAG_AVAILABLE_MEMORY = 0x83
        const val TAG_FILE_DETAILS = 0x84
        const val TAG_RESERVED_FILE_SIZE = 0x85
        const val TAG_MAXIMUM_FILE_SIZE = 0x86
        const val TAG_SUPPORTED_SYS_COMMANDS = 0x87
        const val TAG_UICC_ENV_CONDITIONS = 0x88
        const val TAG_CAT_SECURED_APDU = 0x89

        // ETSI TS 102.221 Clause 11.1.1.4.6
        // Proprietary information
        private fun proprietaryInfoDecoder(
            resources: Resources,
            tlvs: List<Tlv>,
            parent: Element?
        ): List<Element> {
            val list: MutableList<Element> = mutableListOf()

            tlvs.forEach { tlv ->
                when (tlv.tag) {
                    // ETSI TS 102 221 Clause 11.1.1.4.6.1
                    // UICC Characteristics
                    TAG_UICC_CHARACTERISTICS -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.uicc_characteristics_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // ETSI TS 102 221 Clause 11.1.1.4.6.2
                    // Application power consumption
                    TAG_APP_POWER_CONSUMPTION -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.app_power_consumption_label)
                                .parent(parent)
                                .separator(::appPowerConsumptionSeparator)
                                .build(resources))
                    }
                    // ETSI TS 102 221 Clause 11.1.1.4.6.3
                    // Minimum application clock frequency
                    TAG_MIN_APP_CLOCK_FREQ -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.min_app_clock_freq_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // ETSI TS 102 221 Clause 11.1.1.4.6.4
                    // Amount of available memory
                    TAG_AVAILABLE_MEMORY -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.available_memory_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // ETSI TS 102 221 Clause 11.1.1.4.6.5
                    // File details
                    TAG_FILE_DETAILS -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.file_details_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // ETSI TS 102 221 Clause 11.1.1.4.6.6
                    // Reserved file size
                    TAG_RESERVED_FILE_SIZE -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.reserved_file_size_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // ETSI TS 102 221 Clause 11.1.1.4.6.7
                    // Maximum file size
                    TAG_MAXIMUM_FILE_SIZE -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.maximum_file_size_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // ETSI TS 102 221 Clause 11.1.1.4.6.8
                    // Supported system commands
                    TAG_SUPPORTED_SYS_COMMANDS -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.supported_sys_commands_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // ETSI TS 102 221 Clause 11.1.1.4.6.9
                    // Specific UICC environment conditions
                    TAG_UICC_ENV_CONDITIONS -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.uicc_env_conditions_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // ETSI TS 102 221 Clause 11.1.1.4.6.10
                    // Platform to platform CAT secured APDU
                    TAG_CAT_SECURED_APDU -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.cat_secured_apdu_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // Additional TLV objects may be present.
                    else -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .parent(parent)
                                .build(resources))
                    }
                }
            }

            return list
        }

        // ETSI TS 102 221 Clause 11.1.1.4.6.2
        // Application power consumption
        private fun appPowerConsumptionSeparator(
            resources: Resources,
            value: ByteArray,
            parent: Element?
        ): List<Element> {
            if (value.size != 3) return listOf()

            val list: MutableList<Element> = mutableListOf()

            list.add(PrimitiveElement.Builder(byteArrayOf(value[0]))
                    .labelId(R.string.supply_voltage_class_label)
                    .parent(parent)
                    .build(resources))
            list.add(PrimitiveElement.Builder(byteArrayOf(value[1]))
                    .labelId(R.string.app_power_consumption_label)
                    .parent(parent)
                    .build(resources))
            list.add(PrimitiveElement.Builder(byteArrayOf(value[2]))
                    .labelId(R.string.reference_frequency_label)
                    .parent(parent)
                    .build(resources))

            return list
        }
    }
}
