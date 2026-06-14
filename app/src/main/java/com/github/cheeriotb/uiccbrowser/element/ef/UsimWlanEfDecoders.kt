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

// ETSI TS 131 102, clause 4.4.5.
class UsimWlanEfDecoders {
    companion object {
        private const val PLMN_LENGTH = 3
        private const val MIN_PLMN_LIST_LENGTH = 30
        private const val PSEUDONYM_LENGTH_BYTES = 2
        private const val TAG_REAUTHENTICATION_IDENTITY = 0x80
        private const val TAG_MASTER_KEY = 0x81
        private const val TAG_COUNTER = 0x82

        /** Decodes EF Pseudo into its length, pseudonym, and unused storage. */
        fun decodePseudo(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < PSEUDONYM_LENGTH_BYTES) return null
            val length = unsignedValue(bytes.copyOfRange(0, PSEUDONYM_LENGTH_BYTES))
            if (length > bytes.size - PSEUDONYM_LENGTH_BYTES) return null
            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_wlan_pseudo_label)
                    .decoder { innerResources, rawData, parent ->
                        lengthPrefixedDecoder(
                                innerResources, rawData, parent, PSEUDONYM_LENGTH_BYTES,
                                R.string.pseudonym_length_label, R.string.pseudonym_label,
                                PrimitiveElement::defaultStringInterpreter)
                    }
                    .build(resources)
        }

        /** Decodes EF UPLMNWLAN into priority-ordered PLMN entries. */
        fun decodeUplmnWlan(resources: Resources, bytes: ByteArray): Element? =
                decodePlmnList(resources, bytes, R.string.ef_wlan_uplmn_label)

        /** Decodes EF OPLMNWLAN into priority-ordered PLMN entries. */
        fun decodeOplmnWlan(resources: Resources, bytes: ByteArray): Element? =
                decodePlmnList(resources, bytes, R.string.ef_wlan_oplmn_label)

        /** Decodes one EF UWSIDL record into its length, WSID, and unused storage. */
        fun decodeUwsidl(resources: Resources, bytes: ByteArray): Element? =
                decodeWsid(resources, bytes, R.string.ef_wlan_uwsidl_label)

        /** Decodes one EF OWSIDL record into its length, WSID, and unused storage. */
        fun decodeOwsidl(resources: Resources, bytes: ByteArray): Element? =
                decodeWsid(resources, bytes, R.string.ef_wlan_owsidl_label)

        /** Decodes EF WRI into reauthentication identity, master key, and counter TLVs. */
        fun decodeWri(resources: Resources, bytes: ByteArray): Element? {
            if (!isValidWri(bytes)) return null
            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_wlan_wri_label)
                    .decoder(::wriDecoder)
                    .build(resources)
        }

        /** Decodes one EF HWSIDL record into its length, WSID, and unused storage. */
        fun decodeHwsidl(resources: Resources, bytes: ByteArray): Element? =
                decodeWsid(resources, bytes, R.string.ef_wlan_hwsidl_label)

        /** Decodes EF WEHPLMNPI into its I-WLAN EHPLMN display mode. */
        fun decodeWehplmnpi(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleByte(
                        resources, bytes, R.string.ef_wlan_wehplmnpi_label,
                        R.string.wlan_ehplmn_presentation_indication_label,
                        ::wehplmnpiInterpreter)

        /** Decodes EF WHPI into its I-WLAN last RPLMN selection policy. */
        fun decodeWhpi(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleByte(
                        resources, bytes, R.string.ef_wlan_whpi_label,
                        R.string.wlan_last_rplmn_selection_indication_label, ::whpiInterpreter)

        /** Decodes EF WLRPLMN into the last registered I-WLAN PLMN. */
        fun decodeWlrplmn(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != PLMN_LENGTH) return null
            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_wlan_wlrplmn_label)
                    .decoder { innerResources, rawData, parent ->
                        listOf(primitive(
                                innerResources, rawData, R.string.wlan_last_registered_plmn_label,
                                parent, ::plmnInterpreter))
                    }
                    .build(resources)
        }

        /** Decodes EF HPLMNDAI into its direct access state. */
        fun decodeHplmndai(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleByte(
                        resources, bytes, R.string.ef_wlan_hplmndai_label,
                        R.string.hplmn_direct_access_indication_label, ::hplmndaiInterpreter)

        private fun decodePlmnList(resources: Resources, bytes: ByteArray, labelId: Int): Element? {
            if (bytes.size < MIN_PLMN_LIST_LENGTH || bytes.size % PLMN_LENGTH != 0) return null
            return ConstructedElement.Builder(bytes)
                    .labelId(labelId)
                    .decoder(::plmnListDecoder)
                    .build(resources)
        }

        private fun decodeWsid(resources: Resources, bytes: ByteArray, labelId: Int): Element? {
            if (bytes.isEmpty() || unsignedValue(bytes.copyOfRange(0, 1)) > bytes.size - 1) {
                return null
            }
            return ConstructedElement.Builder(bytes)
                    .labelId(labelId)
                    .decoder { innerResources, rawData, parent ->
                        lengthPrefixedDecoder(
                                innerResources, rawData, parent, 1, R.string.wsid_length_label,
                                R.string.wsid_label)
                    }
                    .build(resources)
        }

        private fun decodeSingleByte(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int,
            valueLabelId: Int,
            interpreter: (Resources, ByteArray) -> String
        ): Element? {
            if (bytes.size != 1) return null
            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder { innerResources, rawData, parent ->
                        listOf(primitive(
                                innerResources, rawData, valueLabelId, parent, interpreter))
                    }
                    .build(resources)
        }

        private fun lengthPrefixedDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?,
            lengthBytes: Int,
            lengthLabelId: Int,
            valueLabelId: Int,
            valueInterpreter: (Resources, ByteArray) -> String =
                    PrimitiveElement::defaultInterpreter
        ): List<Element> {
            val valueEnd = lengthBytes + unsignedValue(rawData.copyOfRange(0, lengthBytes))
            val elements = mutableListOf(
                    primitive(
                            resources, rawData.copyOfRange(0, lengthBytes), lengthLabelId, parent,
                            ::unsignedInterpreter),
                    primitive(
                            resources, rawData.copyOfRange(lengthBytes, valueEnd), valueLabelId,
                            parent, valueInterpreter)
            )
            if (valueEnd < rawData.size) {
                elements += primitive(
                        resources, rawData.copyOfRange(valueEnd, rawData.size),
                        R.string.unused_label, parent)
            }
            return elements
        }

        private fun plmnListDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = rawData.asIterable().chunked(PLMN_LENGTH).mapIndexed { index, entry ->
            PrimitiveElement.Builder(entry.toByteArray())
                    .labelId(R.string.wlan_plmn_number_label)
                    .labelArgs(index + 1)
                    .parent(parent)
                    .interpreter(::plmnInterpreter)
                    .build(resources)
        }

        private fun isValidWri(bytes: ByteArray): Boolean {
            var offset = 0
            listOf(TAG_REAUTHENTICATION_IDENTITY, TAG_MASTER_KEY, TAG_COUNTER).forEach { tag ->
                if (offset + 2 > bytes.size || bytes[offset].toInt() and 0xFF != tag) return false
                offset += 2 + (bytes[offset + 1].toInt() and 0xFF)
                if (offset > bytes.size) return false
            }
            return true
        }

        private fun wriDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            var offset = 0
            val elements: MutableList<Element> = listOf(
                    R.string.reauthentication_identity_label to
                            PrimitiveElement::defaultStringInterpreter,
                    R.string.master_key_label to PrimitiveElement::defaultInterpreter,
                    R.string.counter_value_label to PrimitiveElement::defaultInterpreter
            ).map { (labelId, interpreter) ->
                val end = offset + 2 + (rawData[offset + 1].toInt() and 0xFF)
                val element = ConstructedElement.Builder(rawData.copyOfRange(offset, end))
                        .labelId(labelId)
                        .parent(parent)
                        .decoder { innerResources, tlvData, tlvParent ->
                            tlvDecoder(innerResources, tlvData, tlvParent, interpreter)
                        }
                        .build(resources)
                offset = end
                element
            }.toMutableList()
            if (offset < rawData.size) {
                elements += primitive(
                        resources, rawData.copyOfRange(offset, rawData.size),
                        R.string.unused_label, parent)
            }
            return elements
        }

        private fun tlvDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?,
            valueInterpreter: (Resources, ByteArray) -> String
        ): List<Element> = listOf(
                primitive(
                        resources, rawData.copyOfRange(0, 1), R.string.wlan_tlv_tag_label, parent),
                primitive(
                        resources, rawData.copyOfRange(1, 2),
                        R.string.wlan_tlv_length_label, parent, ::unsignedInterpreter),
                primitive(
                        resources, rawData.copyOfRange(2, rawData.size),
                        R.string.wlan_tlv_value_label, parent, valueInterpreter)
        )

        private fun primitive(
            resources: Resources,
            rawData: ByteArray,
            labelId: Int,
            parent: Element?,
            interpreter: (Resources, ByteArray) -> String = PrimitiveElement::defaultInterpreter
        ): Element = PrimitiveElement.Builder(rawData)
                .labelId(labelId)
                .parent(parent)
                .interpreter(interpreter)
                .build(resources)

        private fun unsignedValue(rawData: ByteArray): Int =
                rawData.fold(0) { result, byte ->
                    (result shl Byte.SIZE_BITS) or (byte.toInt() and 0xFF)
                }

        private fun unsignedInterpreter(resources: Resources, rawData: ByteArray): String =
                "${byteArrayToHexString(rawData)} (${unsignedValue(rawData)})"

        private fun plmnInterpreter(resources: Resources, rawData: ByteArray): String {
            if (rawData.size != PLMN_LENGTH) return byteArrayToHexString(rawData)
            if (rawData.all { it.toInt() and 0xFF == 0xFF }) {
                return withDescription(resources, rawData, R.string.unused_label)
            }
            val b1 = rawData[0].toInt() and 0xFF
            val b2 = rawData[1].toInt() and 0xFF
            val b3 = rawData[2].toInt() and 0xFF
            val mcc = "${b1 and 0x0F}${b1 ushr 4}${b2 and 0x0F}"
            val thirdMncDigit = b2 ushr 4
            val mnc = "${b3 and 0x0F}${b3 ushr 4}" +
                    if (thirdMncDigit == 0x0F) "" else thirdMncDigit
            return "${byteArrayToHexString(rawData)} (" +
                    resources.getString(R.string.plmn_interpretation, mcc, mnc) + ")"
        }

        private fun wehplmnpiInterpreter(resources: Resources, rawData: ByteArray): String =
                withDescription(
                        resources, rawData, when (rawData[0].toInt() and 0xFF) {
                            0 -> R.string.wlan_ehplmn_display_no_preference
                            1 -> R.string.wlan_ehplmn_display_highest_priority
                            2 -> R.string.wlan_ehplmn_display_all
                            else -> R.string.rfu_label
                        })

        private fun whpiInterpreter(resources: Resources, rawData: ByteArray): String =
                withDescription(
                        resources, rawData, when (rawData[0].toInt() and 0xFF) {
                            0 -> R.string.wlan_attempt_last_rplmn
                            1 -> R.string.wlan_attempt_home_network
                            else -> R.string.rfu_label
                        })

        private fun hplmndaiInterpreter(resources: Resources, rawData: ByteArray): String =
                withDescription(
                        resources, rawData, when (rawData[0].toInt() and 0xFF) {
                            0 -> R.string.disabled_label
                            1 -> R.string.enabled_label
                            else -> R.string.rfu_label
                        })

        private fun withDescription(resources: Resources, rawData: ByteArray, descriptionId: Int) =
                "${byteArrayToHexString(rawData)} (${resources.getString(descriptionId)})"
    }
}
