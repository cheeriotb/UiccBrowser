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
import com.github.cheeriotb.uiccbrowser.util.BerTlv
import com.github.cheeriotb.uiccbrowser.util.StringUtils
import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString

// ETSI TS 131 103, clauses 4.2.2 to 4.2.23.
class IsimEfDecoders {
    companion object {
        private const val TAG_URI = 0x80
        private const val TAG_PC_SCF_ADDRESS = 0x80
        private const val MIN_AD_LENGTH = 3
        private const val MIN_SMSS_LENGTH = 2
        private const val P_CSCF_TYPE_LENGTH = 1
        private const val P_CSCF_TYPE_FQDN = 0x00
        private const val P_CSCF_TYPE_IPV4 = 0x01
        private const val P_CSCF_TYPE_IPV6 = 0x02
        private const val IPV4_LENGTH = 4
        private const val IPV6_LENGTH = 16

        /**
         * Decodes EF IMPI into the Network Access Identifier TLV data object.
         */
        fun decodeImpi(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleUriFile(
                        resources,
                        bytes,
                        R.string.ef_impi_label,
                        R.string.private_user_identity_label
                )

        /**
         * Decodes EF DOMAIN into the Home Network Domain Name TLV data object.
         */
        fun decodeDomain(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleUriFile(
                        resources,
                        bytes,
                        R.string.ef_domain_label,
                        R.string.home_network_domain_name_label
                )

        /**
         * Decodes EF IMPU into indexed public user identity TLV data objects.
         */
        fun decodeImpu(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty()) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_impu_label)
                    .decoder(::impuDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF AD into UE operation mode, additional information, and optional RFU bytes.
         */
        fun decodeAd(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < MIN_AD_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_ad_label)
                    .decoder(::adDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF ARR by using the common UICC access rule decoder.
         */
        fun decodeArr(resources: Resources, bytes: ByteArray): Element? =
                EfArrRecord.decode(resources, bytes)

        /**
         * Decodes EF IST into one child for each advertised ISIM service bit.
         */
        fun decodeIst(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty()) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_ist_label)
                    .decoder(::istDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF P-CSCF into indexed P-CSCF address TLV data objects.
         */
        fun decodePcscf(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty()) return null
            if (BerTlv.listFrom(bytes).none { it.tag == TAG_PC_SCF_ADDRESS }) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_pcscf_label)
                    .decoder(::pcscfDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF GBABP into RAND, B-TID, and key lifetime.
         */
        fun decodeGbabp(resources: Resources, bytes: ByteArray): Element? =
                UsimEfDecoders.decodeGbabp(resources, bytes)

        /**
         * Decodes EF GBANL into NAF key identifier TLV objects.
         */
        fun decodeGbanl(resources: Resources, bytes: ByteArray): Element? =
                UsimEfDecoders.decodeGbanl(resources, bytes)

        /**
         * Decodes EF NAFKCA into NAF key centre TLV objects.
         */
        fun decodeNafkca(resources: Resources, bytes: ByteArray): Element? =
                UsimEfDecoders.decodeNafkca(resources, bytes)

        /**
         * Decodes EF SMS into status and TPDU bytes.
         */
        fun decodeSms(resources: Resources, bytes: ByteArray): Element? =
                UsimEfDecoders.decodeSms(resources, bytes)

        /**
         * Decodes EF SMSS into message reference, memory flag, and optional RFU bytes.
         */
        fun decodeSmss(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < MIN_SMSS_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_smss_label)
                    .decoder(::smssDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF SMSR into SMS record identifier and status report bytes.
         */
        fun decodeSmsr(resources: Resources, bytes: ByteArray): Element? =
                UsimEfDecoders.decodeSmsr(resources, bytes)

        /**
         * Decodes EF SMSP into alpha identifier and SMS parameter bytes.
         */
        fun decodeSmsp(resources: Resources, bytes: ByteArray): Element? =
                UsimEfDecoders.decodeSmsp(resources, bytes)

        /**
         * Decodes EF UICCIARI into the IARI TLV.
         */
        fun decodeUicciari(resources: Resources, bytes: ByteArray): Element? =
                UsimEfDecoders.decodeUicciari(resources, bytes)

        /**
         * Decodes EF FromPreferred into the preferred From header URI.
         */
        fun decodeFrompreferred(resources: Resources, bytes: ByteArray): Element? =
                UsimEfDecoders.decodeFrompreferred(resources, bytes)

        /**
         * Decodes EF WebRTCURI into the WebRTC URI TLV data object.
         */
        fun decodeWebrtcuri(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleUriFile(
                        resources,
                        bytes,
                        R.string.ef_webrtcuri_label,
                        R.string.uri_label
                )

        /**
         * Decodes EF AC_GBAUAPI into applet and NAF access control entries.
         */
        fun decodeAcGbauapi(resources: Resources, bytes: ByteArray): Element? =
                UsimEfDecoders.decodeAcGbauapi(resources, bytes)

        /**
         * Decodes EF IMSDCI into the IMS data channel indication byte.
         */
        fun decodeImsdci(resources: Resources, bytes: ByteArray): Element? =
                UsimEfDecoders.decodeImsdci(resources, bytes)

        private fun decodeSingleUriFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int,
            childLabelId: Int
        ): Element? {
            if (bytes.isEmpty()) return null
            if (BerTlv.listFrom(bytes).none { it.tag == TAG_URI }) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder { innerResources, rawData, parent ->
                        singleUriDecoder(innerResources, rawData, parent, childLabelId)
                    }
                    .build(resources)
        }

        private fun singleUriDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?,
            labelId: Int
        ): List<Element> {
            val tlv = BerTlv.listFrom(rawData).firstOrNull { it.tag == TAG_URI }
                    ?: return listOf()
            return listOf(
                    PrimitiveElement.Builder(tlv.value)
                            .labelId(labelId)
                            .parent(parent)
                            .interpreter(PrimitiveElement::defaultStringInterpreter)
                            .build(resources)
            )
        }

        private fun impuDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return BerTlv.listFrom(rawData).filter { it.tag == TAG_URI }.mapIndexed { index, tlv ->
                PrimitiveElement.Builder(tlv.value)
                        .labelId(R.string.public_user_identity_label)
                        .labelArgs(index + 1)
                        .parent(parent)
                        .interpreter(PrimitiveElement::defaultStringInterpreter)
                        .build(resources)
            }
        }

        private fun adDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val elements = mutableListOf<Element>()
            elements.add(PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                    .labelId(R.string.ue_operation_mode_label)
                    .parent(parent)
                    .build(resources))
            elements.add(PrimitiveElement.Builder(rawData.copyOfRange(1, 2))
                    .labelId(R.string.additional_information_label)
                    .parent(parent)
                    .build(resources))
            elements.add(PrimitiveElement.Builder(rawData.copyOfRange(2, 3))
                    .labelId(R.string.rfu_label)
                    .parent(parent)
                    .build(resources))
            if (rawData.size > MIN_AD_LENGTH) {
                elements.add(PrimitiveElement.Builder(rawData.copyOfRange(3, rawData.size))
                        .labelId(R.string.rfu_label)
                        .parent(parent)
                        .build(resources))
            }
            return elements
        }

        private fun istDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.mapIndexed { byteIndex, byte ->
                ConstructedElement.Builder(byteArrayOf(byte))
                        .labelId(R.string.byte_number_label)
                        .labelArgs(byteIndex + 1)
                        .parent(parent)
                        .decoder { innerResources, byteData, byteParent ->
                            istByteDecoder(innerResources, byteData, byteParent, byteIndex)
                        }
                        .dataComposer(::serviceTableByteDataComposer)
                        .build(resources)
            }
        }

        private fun istByteDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?,
            byteIndex: Int
        ): List<Element> {
            val byte = rawData.firstOrNull() ?: return emptyList()
            return (0 until 8).map { bitIndex ->
                val serviceNumber = byteIndex * 8 + bitIndex + 1
                val value = if (byte.toInt() and (1 shl bitIndex) != 0) 1 else 0
                PrimitiveElement.Builder(byteArrayOf(value.toByte()))
                        .labelId(R.string.service_number_label)
                        .labelArgs(serviceNumber, isimServiceName(resources, serviceNumber))
                        .parent(parent)
                        .interpreter(::serviceStateInterpreter)
                        .build(resources)
            }
        }

        private fun serviceTableByteDataComposer(elements: List<Element>): ByteArray {
            val value = elements.take(8).foldIndexed(0) { bitIndex, acc, element ->
                if ((element.data.firstOrNull()?.toInt()?.and(0x01) ?: 0) == 1) {
                    acc or (1 shl bitIndex)
                } else {
                    acc
                }
            }
            return byteArrayOf(value.toByte())
        }

        private fun pcscfDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return BerTlv.listFrom(rawData).filter { it.tag == TAG_PC_SCF_ADDRESS }
                    .mapIndexed { index, tlv ->
                ConstructedElement.Builder(BerTlv.encode(tlv.tag, tlv.value))
                        .labelId(R.string.pcscf_address_record_label)
                        .labelArgs(index + 1)
                        .parent(parent)
                        .decoder { innerResources, _, recordParent ->
                            pcscfAddressDecoder(innerResources, tlv.value, recordParent)
                        }
                        .dataComposer { BerTlv.encode(tlv.tag, tlv.value) }
                        .build(resources)
            }
        }

        private fun pcscfAddressDecoder(
            resources: Resources,
            value: ByteArray,
            parent: Element?
        ): List<Element> {
            if (value.size <= P_CSCF_TYPE_LENGTH) return emptyList()
            val addressType = value.first().toInt() and 0xFF
            return listOf(
                    PrimitiveElement.Builder(value.copyOfRange(0, P_CSCF_TYPE_LENGTH))
                            .labelId(R.string.type_of_address_label)
                            .parent(parent)
                            .interpreter(::pcscfAddressTypeInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(value.copyOfRange(P_CSCF_TYPE_LENGTH, value.size))
                            .labelId(R.string.pcscf_address_label)
                            .parent(parent)
                            .interpreter { innerResources, address ->
                                pcscfAddressInterpreter(innerResources, address, addressType)
                            }
                            .build(resources)
            )
        }

        private fun smssDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val elements = mutableListOf<Element>()
            elements.add(PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                    .labelId(R.string.message_reference_label)
                    .parent(parent)
                    .interpreter(::unsignedIntegerInterpreter)
                    .build(resources))
            elements.add(PrimitiveElement.Builder(rawData.copyOfRange(1, 2))
                    .labelId(R.string.memory_capacity_exceeded_flag_label)
                    .parent(parent)
                    .interpreter(::memoryCapacityExceededInterpreter)
                    .build(resources))
            if (rawData.size > MIN_SMSS_LENGTH) {
                elements.add(PrimitiveElement.Builder(rawData.copyOfRange(2, rawData.size))
                        .labelId(R.string.rfu_label)
                        .parent(parent)
                        .build(resources))
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

        private fun memoryCapacityExceededInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val exceeded = rawData.firstOrNull()?.toInt()?.and(0x01) == 1
            val label = if (exceeded) {
                resources.getString(R.string.memory_capacity_exceeded)
            } else {
                resources.getString(R.string.memory_capacity_not_exceeded)
            }
            return resources.getString(
                    R.string.hex_with_description,
                    byteArrayToHexString(rawData),
                    label
            )
        }

        private fun serviceStateInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val available = rawData.firstOrNull()?.toInt()?.and(0x01) == 1
            return if (available) {
                resources.getString(R.string.available_label)
            } else {
                resources.getString(R.string.not_available_label)
            }
        }

        private fun isimServiceName(
            resources: Resources,
            serviceNumber: Int
        ): String = resources.getStringArray(R.array.isim_service_names).getOrElse(
                serviceNumber - 1) {
            resources.getString(R.string.rfu_label)
        }

        private fun pcscfAddressTypeInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val label = when (rawData.firstOrNull()?.toInt()?.and(0xFF)) {
                P_CSCF_TYPE_FQDN -> resources.getString(R.string.pcscf_address_type_fqdn)
                P_CSCF_TYPE_IPV4 -> resources.getString(R.string.pcscf_address_type_ipv4)
                P_CSCF_TYPE_IPV6 -> resources.getString(R.string.pcscf_address_type_ipv6)
                else -> resources.getString(R.string.unknown_label)
            }
            return resources.getString(
                    R.string.hex_with_description,
                    byteArrayToHexString(rawData),
                    label
            )
        }

        private fun pcscfAddressInterpreter(
            resources: Resources,
            rawData: ByteArray,
            addressType: Int
        ): String {
            val description = when (addressType) {
                P_CSCF_TYPE_FQDN -> StringUtils.decode(rawData)
                P_CSCF_TYPE_IPV4 -> ipv4Address(rawData)
                P_CSCF_TYPE_IPV6 -> ipv6Address(rawData)
                else -> byteArrayToHexString(rawData)
            }
            return if (description == byteArrayToHexString(rawData)) {
                description
            } else {
                resources.getString(
                        R.string.hex_with_description,
                        byteArrayToHexString(rawData),
                        description
                )
            }
        }

        private fun ipv4Address(rawData: ByteArray): String {
            if (rawData.size != IPV4_LENGTH) return byteArrayToHexString(rawData)
            return rawData.joinToString(".") { (it.toInt() and 0xFF).toString() }
        }

        private fun ipv6Address(rawData: ByteArray): String {
            if (rawData.size != IPV6_LENGTH) return byteArrayToHexString(rawData)
            return rawData.asIterable().chunked(2).joinToString(":") {
                "%02X%02X".format(it[0].toInt() and 0xFF, it[1].toInt() and 0xFF)
            }
        }
    }
}
