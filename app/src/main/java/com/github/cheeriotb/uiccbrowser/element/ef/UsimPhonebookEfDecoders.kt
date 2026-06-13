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

// ETSI TS 131 102, clause 4.4.2.
class UsimPhonebookEfDecoders {
    companion object {
        private const val PSC_LENGTH = 4
        private const val CC_LENGTH = 2
        private const val PUID_LENGTH = 2
        private const val TAG_TYPE_1 = 0xA8
        private const val TAG_TYPE_2 = 0xA9
        private const val TAG_TYPE_3 = 0xAA

        /**
         * Decodes an EF PBR record into phone book file references.
         */
        fun decodePbr(resources: Resources, bytes: ByteArray): Element? {
            if (!isValidPbrRecord(bytes)) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_pbr_label)
                    .decoder(::pbrDecoder)
                    .dataComposer(::pbrDataComposer)
                    .build(resources)
        }

        /**
         * Decodes EF PSC into the phone book synchronisation counter.
         */
        fun decodePsc(resources: Resources, bytes: ByteArray): Element? =
                decodeCounter(resources, bytes, PSC_LENGTH, R.string.ef_psc_label,
                        R.string.phone_book_synchronisation_counter_label)

        /**
         * Decodes EF CC into the phone book change counter.
         */
        fun decodeCc(resources: Resources, bytes: ByteArray): Element? =
                decodeCounter(resources, bytes, CC_LENGTH, R.string.ef_cc_label,
                        R.string.phone_book_change_counter_label)

        /**
         * Decodes EF PUID into the previous unique identifier.
         */
        fun decodePuid(resources: Resources, bytes: ByteArray): Element? =
                decodeCounter(resources, bytes, PUID_LENGTH, R.string.ef_puid_label,
                        R.string.previous_unique_identifier_label)

        private fun decodeCounter(
            resources: Resources,
            bytes: ByteArray,
            expectedLength: Int,
            efLabelId: Int,
            valueLabelId: Int
        ): Element? {
            if (bytes.size != expectedLength) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(efLabelId)
                    .decoder { nestedResources, rawData, parent ->
                        listOf(PrimitiveElement.Builder(rawData)
                                .labelId(valueLabelId)
                                .parent(parent)
                                .interpreter(::unsignedIntegerInterpreter)
                                .build(nestedResources))
                    }
                    .build(resources)
        }

        private fun pbrDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val tlvs = BerTlv.listFrom(rawData)
            val elements = mutableListOf<Element>()
            tlvs.forEach { tlv ->
                elements.add(BerTlvElement.Builder(tlv)
                        .labelId(typeLabel(tlv.tag))
                        .parent(parent)
                        .decoder(::phonebookReferenceDecoder)
                        .build(resources))
            }
            val encodedLength = tlvs.sumOf { it.toByteArray().size }
            if (encodedLength < rawData.size) {
                elements.add(PrimitiveElement.Builder(rawData.copyOfRange(encodedLength,
                        rawData.size))
                        .labelId(R.string.padding_label)
                        .parent(parent)
                        .build(resources))
            }
            return elements
        }

        private fun pbrDataComposer(elements: List<Element>): ByteArray {
            var bytes = byteArrayOf()
            elements.forEach { bytes += it.byteArray }
            return bytes
        }

        private fun phonebookReferenceDecoder(
            resources: Resources,
            tlvs: List<Tlv>,
            parent: Element?
        ): List<Element> {
            val occurrences = mutableMapOf<Int, Int>()
            return tlvs.map { tlv ->
                val number = occurrences.getOrDefault(tlv.tag, 0) + 1
                occurrences[tlv.tag] = number
                BerTlvElement.Builder(tlv)
                        .labelId(referenceLabel(tlv.tag))
                        .labelArgs(number)
                        .parent(parent)
                        .separator(::fileReferenceSeparator)
                        .build(resources)
            }
        }

        private fun fileReferenceSeparator(
            resources: Resources,
            value: ByteArray,
            parent: Element?
        ): List<Element> {
            if (value.size !in 2..3) return emptyList()

            val elements = mutableListOf<Element>()
            elements.add(PrimitiveElement.Builder(value.copyOfRange(0, 2))
                    .labelId(R.string.file_identifier_label)
                    .parent(parent)
                    .build(resources))
            if (value.size == 3) {
                elements.add(PrimitiveElement.Builder(value.copyOfRange(2, 3))
                        .labelId(R.string.short_file_identifier_label)
                        .parent(parent)
                        .interpreter(::unsignedIntegerInterpreter)
                        .build(resources))
            }
            return elements
        }

        private fun isValidPbrRecord(bytes: ByteArray): Boolean {
            if (bytes.isEmpty()) return false
            val tlvs = BerTlv.listFrom(bytes)
            if (tlvs.isEmpty() || tlvs.any { it.tag !in TAG_TYPE_1..TAG_TYPE_3 }) return false
            if (tlvs.any { type -> type.tlvs.any { it.value.size !in 2..3 } }) return false

            val encodedLength = tlvs.sumOf { it.toByteArray().size }
            return encodedLength <= bytes.size &&
                    bytes.drop(encodedLength).all { it.toInt() and 0xFF == 0xFF }
        }

        private fun typeLabel(tag: Int): Int = when (tag) {
            TAG_TYPE_1 -> R.string.phone_book_type_1_reference_label
            TAG_TYPE_2 -> R.string.phone_book_type_2_reference_label
            TAG_TYPE_3 -> R.string.phone_book_type_3_reference_label
            else -> R.string.unknown_label
        }

        private fun referenceLabel(tag: Int): Int = when (tag) {
            0xC0 -> R.string.ef_adn_reference_label
            0xC1 -> R.string.ef_iap_reference_label
            0xC2 -> R.string.ef_ext1_reference_label
            0xC3 -> R.string.ef_sne_reference_label
            0xC4 -> R.string.ef_anr_reference_label
            0xC5 -> R.string.ef_pbc_reference_label
            0xC6 -> R.string.ef_grp_reference_label
            0xC7 -> R.string.ef_aas_reference_label
            0xC8 -> R.string.ef_gas_reference_label
            0xC9 -> R.string.ef_uid_reference_label
            0xCA -> R.string.ef_email_reference_label
            0xCB -> R.string.ef_ccp1_reference_label
            0xCC -> R.string.ef_puri_reference_label
            else -> R.string.unknown_reference_label
        }

        private fun unsignedIntegerInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.fold(0L) { acc, byte ->
                (acc shl 8) or (byte.toLong() and 0xFF)
            }
            return resources.getString(R.string.hex_decimal_interpretation,
                    byteArrayToHexString(rawData), value)
        }
    }
}
