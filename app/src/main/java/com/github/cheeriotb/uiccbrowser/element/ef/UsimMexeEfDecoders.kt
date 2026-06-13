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

// ETSI TS 131 102, clause 4.4.4.
class UsimMexeEfDecoders {
    companion object {
        private const val ROOT_PUBLIC_KEY_FIXED_LENGTH = 10
        private const val THIRD_PARTY_ROOT_PUBLIC_KEY_FIXED_LENGTH = 11

        /** Decodes EF MexE-ST into individual MexE service availability bits. */
        fun decodeMexeSt(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty()) return null
            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_mexe_st_label)
                    .decoder(::mexeServiceTableDecoder)
                    .dataComposer { bytes }
                    .build(resources)
        }

        /** Decodes one EF ORPK record into its root public key descriptor fields. */
        fun decodeOrpk(resources: Resources, bytes: ByteArray): Element? =
                decodeRootPublicKey(resources, bytes, R.string.ef_orpk_label)

        /** Decodes one EF ARPK record into its root public key descriptor fields. */
        fun decodeArpk(resources: Resources, bytes: ByteArray): Element? =
                decodeRootPublicKey(resources, bytes, R.string.ef_arpk_label)

        /** Decodes one EF TPRPK record into its root public key descriptor fields. */
        fun decodeTprpk(resources: Resources, bytes: ByteArray): Element? {
            if (!isValidThirdPartyRootPublicKey(bytes)) return null
            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_tprpk_label)
                    .decoder(::thirdPartyRootPublicKeyDecoder)
                    .build(resources)
        }

        /** Decodes a transparent EF TKCDF into its key or certificate data element. */
        fun decodeTkcdf(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty()) return null
            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_tkcdf_label)
                    .decoder { innerResources, rawData, parent ->
                        listOf(PrimitiveElement.Builder(rawData)
                                .labelId(R.string.key_certificate_data_label)
                                .parent(parent)
                                .build(innerResources))
                    }
                    .build(resources)
        }

        private fun decodeRootPublicKey(
            resources: Resources,
            bytes: ByteArray,
            labelId: Int
        ): Element? {
            if (!isValidRootPublicKey(bytes)) return null
            return ConstructedElement.Builder(bytes)
                    .labelId(labelId)
                    .decoder(::rootPublicKeyDecoder)
                    .build(resources)
        }

        private fun isValidRootPublicKey(bytes: ByteArray): Boolean {
            if (bytes.size < ROOT_PUBLIC_KEY_FIXED_LENGTH) return false
            val keyIdentifierLength = bytes[9].toInt() and 0xFF
            return bytes.size == ROOT_PUBLIC_KEY_FIXED_LENGTH + keyIdentifierLength
        }

        private fun isValidThirdPartyRootPublicKey(bytes: ByteArray): Boolean {
            if (bytes.size < THIRD_PARTY_ROOT_PUBLIC_KEY_FIXED_LENGTH) return false
            val keyIdentifierLength = bytes[9].toInt() and 0xFF
            val certificateLengthOffset = ROOT_PUBLIC_KEY_FIXED_LENGTH + keyIdentifierLength
            if (certificateLengthOffset >= bytes.size) return false
            val certificateIdentifierLength = bytes[certificateLengthOffset].toInt() and 0xFF
            return bytes.size == THIRD_PARTY_ROOT_PUBLIC_KEY_FIXED_LENGTH +
                    keyIdentifierLength + certificateIdentifierLength
        }

        private fun mexeServiceTableDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = rawData.flatMapIndexed { byteIndex, byte ->
            (0 until Byte.SIZE_BITS).map { bitIndex ->
                val available = (byte.toInt() ushr bitIndex) and 0x01
                PrimitiveElement.Builder(byteArrayOf(available.toByte()))
                        .labelId(mexeServiceLabelId(byteIndex * Byte.SIZE_BITS + bitIndex + 1))
                        .labelArgs(byteIndex * Byte.SIZE_BITS + bitIndex + 1)
                        .parent(parent)
                        .interpreter(::serviceAvailabilityInterpreter)
                        .build(resources)
            }
        }

        private fun rootPublicKeyDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                primitive(resources, rawData, 0, 1, R.string.parameters_indicator_label, parent,
                        ::parametersIndicatorInterpreter),
                primitive(resources, rawData, 1, 2, R.string.flags_label, parent,
                        ::flagsInterpreter),
                primitive(resources, rawData, 2, 3, R.string.certificate_type_label, parent,
                        ::certificateTypeInterpreter),
                primitive(resources, rawData, 3, 5, R.string.key_certificate_file_id_label, parent),
                primitive(resources, rawData, 5, 7, R.string.key_certificate_offset_label, parent,
                        ::unsignedIntegerInterpreter),
                primitive(resources, rawData, 7, 9, R.string.key_certificate_length_label, parent,
                        ::unsignedIntegerInterpreter),
                primitive(resources, rawData, 9, 10, R.string.key_identifier_length_label, parent,
                        ::unsignedIntegerInterpreter),
                primitive(resources, rawData, 10, rawData.size, R.string.key_identifier_label,
                        parent, PrimitiveElement::defaultStringInterpreter)
        )

        private fun thirdPartyRootPublicKeyDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val keyIdentifierEnd = ROOT_PUBLIC_KEY_FIXED_LENGTH + (rawData[9].toInt() and 0xFF)
            val rootPublicKey = rootPublicKeyDecoder(
                    resources, rawData.copyOfRange(0, keyIdentifierEnd), parent
            )
            return rootPublicKey + listOf(
                            primitive(resources, rawData, keyIdentifierEnd, keyIdentifierEnd + 1,
                                    R.string.certificate_identifier_length_label, parent,
                                    ::unsignedIntegerInterpreter),
                            primitive(resources, rawData, keyIdentifierEnd + 1, rawData.size,
                                    R.string.certificate_identifier_label, parent,
                                    PrimitiveElement::defaultStringInterpreter)
                    )
        }

        private fun mexeServiceLabelId(serviceNumber: Int): Int = when (serviceNumber) {
            1 -> R.string.mexe_operator_root_public_key_service_label
            2 -> R.string.mexe_administrator_root_public_key_service_label
            3 -> R.string.mexe_third_party_root_public_key_service_label
            else -> R.string.mexe_rfu_service_label
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

        private fun parametersIndicatorInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val interpretation = if (rawData.first().toInt() and 0x01 == 0) {
                resources.getString(R.string.key_descriptor_valid_label)
            } else {
                resources.getString(R.string.certificate_descriptor_valid_label)
            }
            return "${byteArrayToHexString(rawData)} ($interpretation)"
        }

        private fun flagsInterpreter(resources: Resources, rawData: ByteArray): String {
            val interpretation = if (rawData.first().toInt() and 0x01 == 0x01) {
                resources.getString(R.string.authority_certificate_label)
            } else {
                resources.getString(R.string.non_authority_certificate_label)
            }
            return "${byteArrayToHexString(rawData)} ($interpretation)"
        }

        private fun certificateTypeInterpreter(resources: Resources, rawData: ByteArray): String {
            val interpretation = when (rawData.first().toInt() and 0xFF) {
                0 -> resources.getString(R.string.wtls_certificate_label)
                1 -> resources.getString(R.string.x509_certificate_label)
                2 -> resources.getString(R.string.x968_certificate_label)
                else -> resources.getString(R.string.rfu_label)
            }
            return "${byteArrayToHexString(rawData)} ($interpretation)"
        }

        private fun unsignedIntegerInterpreter(resources: Resources, rawData: ByteArray): String {
            val value = rawData.fold(0L) { result, byte ->
                (result shl Byte.SIZE_BITS) or (byte.toLong() and 0xFF)
            }
            return "${byteArrayToHexString(rawData)} ($value)"
        }

        private fun serviceAvailabilityInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String = if (rawData.first().toInt() and 0x01 == 0x01) {
            resources.getString(R.string.available_label)
        } else {
            resources.getString(R.string.not_available_label)
        }
    }
}
