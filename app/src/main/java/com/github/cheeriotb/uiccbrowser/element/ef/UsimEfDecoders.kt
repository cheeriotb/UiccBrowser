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
import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString
import java.util.Locale

// ETSI TS 131 102, clauses 4.2.1 to 4.2.7.
class UsimEfDecoders {
    companion object {
        private const val IMSI_LENGTH = 9
        private const val KEYS_LENGTH = 33
        private const val HPPLMN_LENGTH = 1
        private const val ACM_MAX_LENGTH = 3
        private const val LI_ENTRY_LENGTH = 2
        private const val PLMN_W_ACT_ENTRY_LENGTH = 5
        private const val MIN_PLMN_W_ACT_LENGTH = 40

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

        private fun liDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.asIterable().chunked(LI_ENTRY_LENGTH).map { entry ->
                PrimitiveElement.Builder(entry.toByteArray())
                        .labelId(R.string.language_code_label)
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
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 3))
                        .labelId(R.string.plmn_label)
                        .parent(parent)
                        .interpreter(::plmnInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(3, PLMN_W_ACT_ENTRY_LENGTH))
                        .labelId(R.string.access_technology_identifier_label)
                        .parent(parent)
                        .interpreter(::accessTechnologyInterpreter)
                        .build(resources)
        )

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
                    digits.add(nibbleToDigit(value and 0x0F))
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
            if (rawData.all { it.toInt() and 0xFF == 0xFF }) {
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
            return byteArrayToHexString(rawData) + " (MCC $mcc, MNC $mnc)"
        }

        private fun accessTechnologyInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size != 2) return byteArrayToHexString(rawData)

            val first = rawData[0].toInt() and 0xFF
            val second = rawData[1].toInt() and 0xFF
            val names = linkedSetOf<String>()
            if (first and 0x80 != 0) names.add("Satellite E-UTRAN in NB-S1 mode")
            if (first and 0x40 != 0) names.add("Satellite E-UTRAN in WB-S1 mode")
            if (first and 0x20 != 0) names.add("Satellite NG-RAN")
            if (first and 0x10 != 0) names.add("NG-RAN")
            if (first and 0x08 != 0) names.add("E-UTRAN in NB-S1 mode")
            if (first and 0x04 != 0) names.add("E-UTRAN in WB-S1 mode")
            if (first and 0x02 != 0) names.add("E-UTRAN")
            if (first and 0x01 != 0) names.add("UTRAN")
            if (second and 0x20 != 0) names.add("GSM")
            if (second and 0x10 != 0) names.add("EC-GSM-IoT")
            if (second and 0x08 != 0) names.add("cdma2000 1xRTT")
            if (second and 0x04 != 0) names.add("cdma2000 HRPD")
            if (second and 0x02 != 0) names.add("GSM COMPACT")
            if (second and 0x01 != 0) names.add("GSM")

            val suffix = if (names.isEmpty()) {
                resources.getString(R.string.no_access_technology_selected)
            } else {
                names.joinToString(", ")
            }
            return byteArrayToHexString(rawData) + " ($suffix)"
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
                1 -> "2 hours for NB-IoT, EC-GSM-IoT, or Category M1"
                in 2..40 -> "${value * 2} hours for NB-IoT, EC-GSM-IoT, or Category M1"
                in 41..80 -> "${value * 4 - 80} hours for NB-IoT, EC-GSM-IoT, or Category M1"
                else -> "default period for NB-IoT, EC-GSM-IoT, or Category M1"
            }
            return "$hex (${value}n minutes; $iotText)"
        }

        private fun unsignedIntegerInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) }
            return byteArrayToHexString(rawData) + " ($value)"
        }

        private fun nibbleToDigit(nibble: Int): Char {
            return if (nibble in 0..9) {
                ('0'.code + nibble).toChar()
            } else {
                nibble.toString(16).uppercase(Locale.US)[0]
            }
        }
    }
}
