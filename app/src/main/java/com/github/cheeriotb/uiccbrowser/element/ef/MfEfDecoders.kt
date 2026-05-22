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
import com.github.cheeriotb.uiccbrowser.util.Tlv
import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString
import java.util.Locale

// ETSI TS 102 221, clauses 13.2, 13.3, and 13.6.
// ISO/IEC 7816-4:2005, clause 8.2.1.1 for EF ATR.
class MfEfDecoders {
    companion object {
        private const val ICCID_LENGTH = 10
        private const val PL_ENTRY_LENGTH = 2
        private const val UMPC_LENGTH = 5

        /**
         * Decodes EF ATR into interindustry BER-TLV data objects.
         */
        fun decodeAtr(resources: Resources, bytes: ByteArray): Element? {
            if (BerTlv.listFrom(bytes).isEmpty()) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_atr_label)
                    .decoder(::atrDecoder)
                    .dataComposer(::tlvDataComposer)
                    .build(resources)
        }

        /**
         * Decodes EF PL into ordered two-byte ISO 639 preferred language entries.
         */
        fun decodePl(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty() || bytes.size % PL_ENTRY_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_pl_label)
                    .decoder(::plDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF UMPC into maximum power, operator timeout, flags, and RFU bytes.
         */
        fun decodeUmpc(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != UMPC_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_umpc_label)
                    .decoder(::umpcDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF ICCID into the ten-byte swapped BCD identification number.
         */
        fun decodeIccid(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != ICCID_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_iccid_label)
                    .decoder(::iccidDecoder)
                    .build(resources)
        }

        private fun atrDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return atrTlvDecoder(resources, BerTlv.listFrom(rawData), parent)
        }

        private fun atrTlvDecoder(
            resources: Resources,
            tlvs: List<Tlv>,
            parent: Element?
        ): List<Element> {
            return tlvs.map { tlv ->
                when (tlv.tag) {
                    AppTemplate.TAG_APPLICATION_TEMPLATE -> BerTlvElement.Builder(tlv)
                            .labelId(R.string.app_template_label)
                            .decoder(::atrTlvDecoder)
                    AppTemplate.TAG_APPLICATION_ID -> BerTlvElement.Builder(tlv)
                            .labelId(R.string.app_id_label)
                    AppTemplate.TAG_APPLICATION_LABEL -> BerTlvElement.Builder(tlv)
                            .labelId(R.string.app_label_label)
                            .interpreter(PrimitiveElement::defaultStringInterpreter)
                    AppTemplate.TAG_PATH -> BerTlvElement.Builder(tlv)
                            .labelId(R.string.app_path_label)
                    AppTemplate.TAG_COMMAND_APDU -> BerTlvElement.Builder(tlv)
                            .labelId(R.string.command_apdu_label)
                    AppTemplate.TAG_DISCRETIONARY_DATA -> BerTlvElement.Builder(tlv)
                            .labelId(R.string.discretionary_data_label)
                    AppTemplate.TAG_DISCRETIONARY_TEMPLATE -> BerTlvElement.Builder(tlv)
                            .labelId(R.string.discretionary_template_label)
                            .decoder(::atrTlvDecoder)
                    AppTemplate.TAG_URL -> BerTlvElement.Builder(tlv)
                            .labelId(R.string.url_label)
                    else -> BerTlvElement.Builder(tlv)
                }.parent(parent).build(resources)
            }
        }

        private fun tlvDataComposer(elements: List<Element>): ByteArray {
            var array = byteArrayOf()
            elements.forEach { array += it.byteArray }
            return array
        }

        private fun plDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.asIterable().chunked(PL_ENTRY_LENGTH).mapIndexed { index, entry ->
                PrimitiveElement.Builder(entry.toByteArray())
                        .labelId(R.string.preferred_language_label)
                        .labelArgs(index + 1)
                        .parent(parent)
                        .interpreter(::languageCodeInterpreter)
                        .build(resources)
            }
        }

        private fun umpcDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                        .labelId(R.string.uicc_max_power_consumption_label)
                        .parent(parent)
                        .interpreter(::powerConsumptionInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(1, 2))
                        .labelId(R.string.operator_timeout_label)
                        .parent(parent)
                        .interpreter(::timeoutInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(2, 3))
                        .labelId(R.string.additional_information_label)
                        .parent(parent)
                        .interpreter(::additionalInformationInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(3, UMPC_LENGTH))
                        .labelId(R.string.rfu_label)
                        .parent(parent)
                        .build(resources)
        )

        private fun iccidDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData)
                        .labelId(R.string.identification_number_label)
                        .parent(parent)
                        .interpreter(::iccidInterpreter)
                        .build(resources)
        )

        private fun languageCodeInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size == PL_ENTRY_LENGTH && rawData.all { it.toInt() and 0xFF == 0xFF }) {
                return byteArrayToHexString(rawData) + " (" +
                        resources.getString(R.string.unused_label) + ")"
            }
            return PrimitiveElement.defaultStringInterpreter(resources, rawData)
        }

        private fun powerConsumptionInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0x7F) ?: return ""
            return resources.getString(
                    R.string.hex_with_description,
                    byteArrayToHexString(rawData),
                    resources.getString(R.string.power_consumption_ma, value)
            )
        }

        private fun timeoutInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            return resources.getString(
                    R.string.hex_with_description,
                    byteArrayToHexString(rawData),
                    resources.getString(R.string.timeout_seconds, value)
            )
        }

        private fun additionalInformationInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val idleCurrent = if (value and 0x01 == 0) {
                resources.getString(R.string.increased_idle_current_not_required)
            } else {
                resources.getString(R.string.increased_idle_current_required)
            }
            val suspension = if (value and 0x02 == 0) {
                resources.getString(R.string.uicc_suspension_not_supported)
            } else {
                resources.getString(R.string.uicc_suspension_supported)
            }
            return resources.getString(
                    R.string.hex_with_description,
                    byteArrayToHexString(rawData),
                    resources.getString(
                            R.string.semicolon_joined_descriptions,
                            idleCurrent,
                            suspension
                    )
            )
        }

        private fun iccidInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val digits = rawData.flatMap { byte ->
                val value = byte.toInt() and 0xFF
                listOf(value and 0x0F, (value ushr 4) and 0x0F)
            }.map { nibble ->
                if (nibble in 0..9) {
                    ('0'.code + nibble).toChar()
                } else {
                    nibble.toString(16).uppercase(Locale.US)[0]
                }
            }.filter { it != 'F' }.joinToString("")
            return resources.getString(
                    R.string.hex_with_description,
                    byteArrayToHexString(rawData),
                    digits
            )
        }
    }
}
