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

// ETSI TS 131 102, clauses 4.4.11.8 and 4.4.11.9.
class Usim5gsEfDecoders {
    companion object {
        private const val TAG_PROTECTION_SCHEME_LIST = 0xA0
        private const val TAG_PUBLIC_KEY_LIST = 0xA1
        private const val TAG_PUBLIC_KEY_IDENTIFIER = 0x80
        private const val TAG_PUBLIC_KEY = 0x81
        private const val PROTECTION_SCHEME_ENTRY_LENGTH = 2
        private const val MIN_SUCI_CALC_INFO_LENGTH = 2
        private const val OPL5G_ENTRY_LENGTH = 10

        private data class RawTlv(
            val tag: Int,
            val value: ByteArray,
            val encoded: ByteArray
        )

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
