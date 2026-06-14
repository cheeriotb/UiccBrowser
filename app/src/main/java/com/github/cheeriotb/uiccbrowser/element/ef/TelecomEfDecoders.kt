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
import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString

// ETSI TS 131 102, clauses 4.5.4 to 4.5.9, and ETSI TS 102 222, clause 7.
class TelecomEfDecoders {
    companion object {
        private const val DIALING_NUMBER_TRAILER_LENGTH = 14
        private const val TAG_ICE_FREE_FORMAT_LABEL = 0x80
        private const val TAG_ICE_FREE_FORMAT_CONTENT = 0x81
        private const val TAG_URI = 0x80
        private const val CTLV_TAG_ALPHA_IDENTIFIER = 0x05
        private const val CTLV_TAG_ICON_IDENTIFIER = 0x1E
        private const val CTLV_TAG_TEXT_ATTRIBUTE = 0x50

        private data class ParsedTlv(
            val rawData: ByteArray,
            val tag: Int,
            val tagLength: Int,
            val lengthLength: Int
        )

        /** Decodes EF SUME into its SET UP MENU comprehension TLVs and padding. */
        fun decodeSume(resources: Resources, bytes: ByteArray): Element? {
            val parsed = parseComprehensionTlvs(bytes, allowPadding = true) ?: return null
            if (parsed.first.firstOrNull()?.tag != CTLV_TAG_ALPHA_IDENTIFIER) return null
            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_sume_label)
                    .decoder(::sumeDecoder)
                    .build(resources)
        }

        /** Decodes one DF TELECOM EF ARR record with the common access-rule decoder. */
        fun decodeArr(resources: Resources, bytes: ByteArray): Element? =
                EfArrRecord.decode(resources, bytes)

        /** Decodes one EF ICE_DN record using the EF ADN record layout. */
        fun decodeIceDn(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < DIALING_NUMBER_TRAILER_LENGTH) return null
            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_ice_dn_label)
                    .decoder(::iceDnDecoder)
                    .build(resources)
        }

        /** Decodes one EF ICE_FF record into its label/content TLVs and padding. */
        fun decodeIceFf(resources: Resources, bytes: ByteArray): Element? {
            val parsed = parseBerTlvs(bytes) ?: return null
            if (parsed.first.map { it.tag } !=
                    listOf(TAG_ICE_FREE_FORMAT_LABEL, TAG_ICE_FREE_FORMAT_CONTENT)) return null
            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_ice_ff_label)
                    .decoder(::iceFfDecoder)
                    .dataComposer(::tlvFileDataComposer)
                    .build(resources)
        }

        /** Decodes one EF RMA record into its length, comprehension TLVs, and padding. */
        fun decodeRma(resources: Resources, bytes: ByteArray): Element? {
            val field = parseLengthPrefixedField(bytes) ?: return null
            if (parseComprehensionTlvs(field.second, allowPadding = false) == null) return null
            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_rma_label)
                    .decoder(::rmaDecoder)
                    .build(resources)
        }

        /** Decodes one EF PSISMSC record into its UTF-8 URI TLV and optional padding. */
        fun decodePsismsc(resources: Resources, bytes: ByteArray): Element? {
            val parsed = parseBerTlvs(bytes) ?: return null
            if (parsed.first.size != 1 || parsed.first.single().tag != TAG_URI) return null
            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_psismsc_label)
                    .decoder(::psismscDecoder)
                    .dataComposer(::tlvFileDataComposer)
                    .build(resources)
        }

        private fun sumeDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val (tlvs, padding) = parseComprehensionTlvs(rawData, allowPadding = true)
                    ?: return emptyList()
            val elements = tlvs.mapIndexed { index, tlv ->
                val labelId = when (tlv.tag) {
                    CTLV_TAG_ALPHA_IDENTIFIER -> R.string.title_alpha_identifier_label
                    CTLV_TAG_ICON_IDENTIFIER -> R.string.title_icon_identifier_label
                    CTLV_TAG_TEXT_ATTRIBUTE -> R.string.title_text_attribute_label
                    else -> R.string.comprehension_tlv_number_label
                }
                comprehensionTlvElement(resources, tlv, parent, labelId, index + 1)
            }.toMutableList()
            addPadding(resources, elements, padding, parent)
            return elements
        }

        private fun iceDnDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val alphaLength = rawData.size - DIALING_NUMBER_TRAILER_LENGTH
            return listOf(
                    primitive(resources, rawData, 0, alphaLength, R.string.alpha_identifier_label,
                            parent, ::alphaIdentifierInterpreter),
                    primitive(resources, rawData, alphaLength, alphaLength + 1,
                            R.string.bcd_number_length_label, parent, ::unsignedInterpreter),
                    primitive(resources, rawData, alphaLength + 1, alphaLength + 2,
                            R.string.ton_npi_label, parent, ::tonNpiInterpreter),
                    primitive(resources, rawData, alphaLength + 2, alphaLength + 12,
                            R.string.dialling_number_label, parent, ::swappedBcdInterpreter),
                    primitive(resources, rawData, alphaLength + 12, alphaLength + 13,
                            R.string.ccp1_record_identifier_label, parent,
                            ::recordIdentifierInterpreter),
                    primitive(resources, rawData, alphaLength + 13, rawData.size,
                            R.string.extension1_record_identifier_label, parent,
                            ::recordIdentifierInterpreter)
            )
        }

        private fun iceFfDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val (tlvs, padding) = parseBerTlvs(rawData) ?: return emptyList()
            val elements = tlvs.map { parsed ->
                val tlv = BerTlv.listFrom(parsed.rawData).single()
                BerTlvElement.Builder(tlv)
                        .labelId(if (parsed.tag == TAG_ICE_FREE_FORMAT_LABEL) {
                            R.string.ice_free_format_label_label
                        } else {
                            R.string.ice_free_format_content_label
                        })
                        .parent(parent)
                        .separator(::textStringSeparator)
                        .build(resources)
            }.toMutableList<Element>()
            addPadding(resources, elements, padding, parent)
            return elements
        }

        private fun rmaDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val (lengthData, field, padding) = parseLengthPrefixedField(rawData)
                    ?: return emptyList()
            val parsed = parseComprehensionTlvs(field, allowPadding = false)?.first
                    ?: return emptyList()
            val elements = mutableListOf<Element>(
                    PrimitiveElement.Builder(lengthData)
                            .labelId(R.string.comprehension_tlv_set_length_label)
                            .parent(parent)
                            .interpreter { _, _ -> "${byteArrayToHexString(lengthData)} " +
                                    "(${field.size})" }
                            .build(resources)
            )
            parsed.forEachIndexed { index, tlv ->
                elements += comprehensionTlvElement(
                        resources, tlv, parent, R.string.comprehension_tlv_number_label, index + 1)
            }
            addPadding(resources, elements, padding, parent)
            return elements
        }

        private fun psismscDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val (parsed, padding) = parseBerTlvs(rawData) ?: return emptyList()
            val tlv = BerTlv.listFrom(parsed.single().rawData).single()
            val elements = mutableListOf<Element>(
                    BerTlvElement.Builder(tlv)
                            .labelId(R.string.public_service_identity_sm_sc_label)
                            .parent(parent)
                            .interpreter(::utf8Interpreter)
                            .build(resources)
            )
            addPadding(resources, elements, padding, parent)
            return elements
        }

        private fun comprehensionTlvElement(
            resources: Resources,
            tlv: ParsedTlv,
            parent: Element?,
            labelId: Int,
            index: Int
        ): Element = ConstructedElement.Builder(tlv.rawData)
                .labelId(labelId)
                .labelArgs(index)
                .parent(parent)
                .decoder { innerResources, rawData, tlvParent ->
                    comprehensionTlvDecoder(innerResources, rawData, tlvParent, tlv)
                }
                .build(resources)

        private fun comprehensionTlvDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?,
            tlv: ParsedTlv
        ): List<Element> {
            val valueOffset = tlv.tagLength + tlv.lengthLength
            val valueInterpreter = if (tlv.tag == CTLV_TAG_ALPHA_IDENTIFIER) {
                ::alphaIdentifierInterpreter
            } else {
                PrimitiveElement::defaultInterpreter
            }
            return listOf(
                    primitive(resources, rawData, 0, tlv.tagLength,
                            R.string.comprehension_tlv_tag_label, parent,
                            ::comprehensionTagInterpreter),
                    primitive(resources, rawData, tlv.tagLength, valueOffset,
                            R.string.comprehension_tlv_length_label, parent,
                            ::encodedLengthInterpreter),
                    primitive(resources, rawData, valueOffset, rawData.size,
                            R.string.comprehension_tlv_value_label, parent, valueInterpreter)
            )
        }

        private fun textStringSeparator(
            resources: Resources,
            value: ByteArray,
            parent: Element?
        ): List<Element> {
            if (value.isEmpty()) return emptyList()
            return listOf(
                    PrimitiveElement.Builder(value.copyOfRange(0, 1))
                            .labelId(R.string.data_coding_scheme_label)
                            .parent(parent)
                            .interpreter(::textStringDcsInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(value.copyOfRange(1, value.size))
                            .labelId(R.string.text_string_label)
                            .parent(parent)
                            .interpreter { innerResources, rawData ->
                                textStringInterpreter(innerResources, value[0], rawData)
                            }
                            .build(resources)
            )
        }

        private fun parseComprehensionTlvs(
            bytes: ByteArray,
            allowPadding: Boolean
        ): Pair<List<ParsedTlv>, ByteArray>? {
            val result = mutableListOf<ParsedTlv>()
            var offset = 0
            while (offset < bytes.size) {
                if (allowPadding && bytes[offset].toInt() and 0xFF == 0xFF) break
                val parsed = parseTlvAt(bytes, offset, comprehension = true) ?: return null
                result += parsed
                offset += parsed.rawData.size
            }
            if (!allowPadding && offset != bytes.size) return null
            val padding = bytes.copyOfRange(offset, bytes.size)
            if (allowPadding && padding.any { it.toInt() and 0xFF != 0xFF }) return null
            return result to padding
        }

        private fun parseBerTlvs(bytes: ByteArray): Pair<List<ParsedTlv>, ByteArray>? {
            val result = mutableListOf<ParsedTlv>()
            var offset = 0
            while (offset < bytes.size && bytes[offset].toInt() and 0xFF != 0xFF) {
                val parsed = parseTlvAt(bytes, offset, comprehension = false) ?: return null
                result += parsed
                offset += parsed.rawData.size
            }
            val padding = bytes.copyOfRange(offset, bytes.size)
            if (padding.any { it.toInt() and 0xFF != 0xFF }) return null
            return result to padding
        }

        private fun parseTlvAt(bytes: ByteArray, offset: Int, comprehension: Boolean): ParsedTlv? {
            if (offset >= bytes.size) return null
            var cursor = offset
            val firstTag = bytes[cursor++].toInt() and 0xFF
            val tagLength: Int
            val tag: Int
            if (comprehension && firstTag == 0x7F) {
                if (cursor + 2 > bytes.size) return null
                tag = ((bytes[cursor].toInt() and 0x7F) shl 8) or
                        (bytes[cursor + 1].toInt() and 0xFF)
                cursor += 2
                tagLength = 3
            } else {
                tag = if (comprehension) firstTag and 0x7F else firstTag
                tagLength = 1
            }
            val length = parseLength(bytes, cursor) ?: return null
            val totalLength = tagLength + length.first.size + length.second
            if (offset + totalLength > bytes.size) return null
            return ParsedTlv(
                    bytes.copyOfRange(offset, offset + totalLength), tag, tagLength,
                    length.first.size)
        }

        private fun parseLengthPrefixedField(
            bytes: ByteArray
        ): Triple<ByteArray, ByteArray, ByteArray>? {
            val length = parseLength(bytes, 0) ?: return null
            val fieldEnd = length.first.size + length.second
            if (fieldEnd > bytes.size) return null
            return Triple(
                    length.first, bytes.copyOfRange(length.first.size, fieldEnd),
                    bytes.copyOfRange(fieldEnd, bytes.size))
        }

        private fun parseLength(bytes: ByteArray, offset: Int): Pair<ByteArray, Int>? {
            if (offset >= bytes.size) return null
            val first = bytes[offset].toInt() and 0xFF
            if (first <= 0x7F) return byteArrayOf(bytes[offset]) to first
            val count = first and 0x7F
            if (count !in 1..3 || offset + count >= bytes.size) return null
            val lengthData = bytes.copyOfRange(offset, offset + count + 1)
            val length = lengthData.drop(1).fold(0) { result, byte ->
                (result shl Byte.SIZE_BITS) or (byte.toInt() and 0xFF)
            }
            return lengthData to length
        }

        private fun tlvFileDataComposer(elements: List<Element>): ByteArray =
                elements.fold(byteArrayOf()) { result, element -> result + element.byteArray }

        private fun addPadding(
            resources: Resources,
            elements: MutableList<Element>,
            padding: ByteArray,
            parent: Element?
        ) {
            if (padding.isNotEmpty()) {
                elements += PrimitiveElement.Builder(padding)
                        .labelId(R.string.padding_label)
                        .parent(parent)
                        .build(resources)
            }
        }

        private fun primitive(
            resources: Resources,
            rawData: ByteArray,
            start: Int,
            end: Int,
            labelId: Int,
            parent: Element?,
            interpreter: (Resources, ByteArray) -> String = PrimitiveElement::defaultInterpreter
        ): Element = PrimitiveElement.Builder(rawData.copyOfRange(start, end))
                .labelId(labelId)
                .parent(parent)
                .interpreter(interpreter)
                .build(resources)

        private fun unsignedInterpreter(resources: Resources, rawData: ByteArray): String {
            val value = rawData.fold(0L) { result, byte ->
                (result shl Byte.SIZE_BITS) or (byte.toLong() and 0xFF)
            }
            return "${byteArrayToHexString(rawData)} ($value)"
        }

        private fun alphaIdentifierInterpreter(resources: Resources, rawData: ByteArray): String {
            val value = rawData.dropLastWhile { it.toInt() and 0xFF == 0xFF }.toByteArray()
            return withDescription(resources, rawData, StringUtils.decode(value))
        }

        private fun swappedBcdInterpreter(resources: Resources, rawData: ByteArray): String {
            val value = rawData.flatMap { byte ->
                listOf(byte.toInt() and 0x0F, (byte.toInt() ushr 4) and 0x0F)
            }.takeWhile { it != 0x0F }.joinToString("") { digit ->
                when (digit) {
                    0x0A -> "*"
                    0x0B -> "#"
                    0x0C -> "a"
                    0x0D -> "b"
                    0x0E -> "c"
                    else -> digit.toString()
                }
            }
            return withDescription(resources, rawData, value)
        }

        private fun recordIdentifierInterpreter(resources: Resources, rawData: ByteArray): String {
            val value = rawData.first().toInt() and 0xFF
            val description = when (value) {
                0x00 -> resources.getString(R.string.no_record_associated)
                0xFF -> resources.getString(R.string.unused_label)
                else -> resources.getString(R.string.record_number, value)
            }
            return withDescription(resources, rawData, description)
        }

        private fun tonNpiInterpreter(resources: Resources, rawData: ByteArray): String {
            val value = rawData.first().toInt() and 0xFF
            val ton = (value ushr 4) and 0x07
            val npi = value and 0x0F
            val description = resources.getString(R.string.ton_npi_interpretation, ton, npi)
            return withDescription(resources, rawData, description)
        }

        private fun comprehensionTagInterpreter(resources: Resources, rawData: ByteArray): String {
            val requiredByte = if (rawData.first().toInt() and 0xFF == 0x7F) {
                rawData.getOrElse(1) { 0 }
            } else {
                rawData.first()
            }
            val required = requiredByte.toInt() and 0x80 != 0
            return withDescription(
                    resources, rawData, resources.getString(if (required) {
                        R.string.comprehension_required_label
                    } else {
                        R.string.comprehension_not_required_label
                    }))
        }

        private fun encodedLengthInterpreter(resources: Resources, rawData: ByteArray): String =
                withDescription(resources, rawData, parseLength(rawData, 0)!!.second.toString())

        private fun textStringInterpreter(
            resources: Resources,
            dcs: Byte,
            rawData: ByteArray
        ): String = if (dcs.toInt() and 0xFF == 0x08) {
            val ucs2 = byteArrayOf(0x80.toByte()) + rawData
            withDescription(resources, rawData, StringUtils.decode(ucs2))
        } else {
            PrimitiveElement.defaultStringInterpreter(resources, rawData)
        }

        private fun textStringDcsInterpreter(resources: Resources, rawData: ByteArray): String {
            val description = when (rawData.first().toInt() and 0xFF) {
                0x00 -> resources.getString(R.string.gsm_default_alphabet_label)
                0x04 -> resources.getString(R.string.eight_bit_data_label)
                0x08 -> resources.getString(R.string.ucs2_label)
                else -> resources.getString(R.string.unknown_label)
            }
            return withDescription(resources, rawData, description)
        }

        private fun utf8Interpreter(resources: Resources, rawData: ByteArray): String =
                withDescription(resources, rawData, rawData.toString(Charsets.UTF_8))

        private fun withDescription(
            resources: Resources,
            rawData: ByteArray,
            description: String
        ): String = if (description.isEmpty()) {
            byteArrayToHexString(rawData)
        } else {
            "${byteArrayToHexString(rawData)} ($description)"
        }
    }
}
