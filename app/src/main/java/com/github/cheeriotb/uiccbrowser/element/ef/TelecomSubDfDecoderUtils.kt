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

/** Shared decoders for service tables and BER-length TLVs stored in transparent EFs. */
internal object TelecomSubDfDecoderUtils {
    data class PrefixField(val length: Int, val labelId: Int)

    private data class RawTlv(
        val tag: Int,
        val tagBytes: ByteArray,
        val lengthBytes: ByteArray,
        val value: ByteArray,
        val encoded: ByteArray
    )

    fun decodeServiceTable(
        resources: Resources,
        bytes: ByteArray,
        labelId: Int,
        serviceLabelIds: List<Int>,
        codingLabelId: Int? = null
    ): Element? {
        val minimumLength = if (codingLabelId == null) 1 else 2
        if (bytes.size < minimumLength) return null
        return ConstructedElement.Builder(bytes)
                .labelId(labelId)
                .decoder { innerResources, rawData, parent ->
                    serviceTableDecoder(
                            innerResources, rawData, parent, serviceLabelIds, codingLabelId)
                }
                .dataComposer { elements -> serviceTableDataComposer(elements, codingLabelId) }
                .build(resources)
    }

    fun decodeTlvFile(
        resources: Resources,
        bytes: ByteArray,
        labelId: Int,
        minimumLength: Int,
        prefixFields: List<PrefixField>,
        tagLabels: Map<Int, Int>,
        allowEmpty: Boolean = false
    ): Element? {
        if (bytes.size < minimumLength) return null
        val contentEnd = bytes.indexOfLast { it.toInt() and 0xFF != 0xFF } + 1
        val tlvs = parseTlvs(bytes.copyOfRange(0, contentEnd))
        if (contentEnd <= 0 && !allowEmpty) return null
        if (contentEnd > 0 && (tlvs.isEmpty() || tlvs.any { it.tag != 0xA0 })) return null
        return ConstructedElement.Builder(bytes)
                .labelId(labelId)
                .decoder { innerResources, rawData, parent ->
                    tlvFileDecoder(innerResources, rawData, parent, prefixFields, tagLabels)
                }
                .build(resources)
    }

    private fun serviceTableDecoder(
        resources: Resources,
        rawData: ByteArray,
        parent: Element?,
        serviceLabelIds: List<Int>,
        codingLabelId: Int?
    ): List<Element> {
        val elements = mutableListOf<Element>()
        val serviceData = if (codingLabelId == null) {
            rawData
        } else {
            rawData.copyOfRange(1, rawData.size)
        }
        if (codingLabelId != null) {
            elements += PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                    .labelId(codingLabelId)
                    .parent(parent)
                    .interpreter { innerResources, data ->
                        codingInterpreter(
                                innerResources, data,
                                codingLabelId == R.string.v2x_data_coding_label)
                    }
                    .build(resources)
        }
        serviceData.forEachIndexed { byteIndex, byte ->
            repeat(Byte.SIZE_BITS) { bitIndex ->
                val serviceNumber = byteIndex * Byte.SIZE_BITS + bitIndex + 1
                val value = (byte.toInt() ushr bitIndex) and 0x01
                val label = serviceLabelIds.getOrNull(serviceNumber - 1)
                        ?: R.string.generic_service_label
                elements += PrimitiveElement.Builder(byteArrayOf(value.toByte()))
                        .labelId(label)
                        .labelArgs(serviceNumber)
                        .parent(parent)
                        .interpreter(::availabilityInterpreter)
                        .build(resources)
            }
        }
        return elements
    }

    private fun serviceTableDataComposer(elements: List<Element>, codingLabelId: Int?): ByteArray {
        val coding = if (codingLabelId == null) byteArrayOf() else elements.first().data
        val serviceElements = if (codingLabelId == null) elements else elements.drop(1)
        val services = serviceElements.chunked(Byte.SIZE_BITS).map { bits ->
            bits.foldIndexed(0) { bitIndex, value, element ->
                value or ((element.data.first().toInt() and 0x01) shl bitIndex)
            }.toByte()
        }.toByteArray()
        return coding + services
    }

    private fun tlvFileDecoder(
        resources: Resources,
        rawData: ByteArray,
        parent: Element?,
        prefixFields: List<PrefixField>,
        tagLabels: Map<Int, Int>
    ): List<Element> {
        val contentEnd = rawData.indexOfLast { it.toInt() and 0xFF != 0xFF } + 1
        val elements = parseTlvs(rawData.copyOfRange(0, contentEnd)).mapIndexed { index, tlv ->
            tlvElement(resources, tlv, parent, index + 1, prefixFields, tagLabels)
        }.toMutableList()
        if (contentEnd < rawData.size) {
            elements += PrimitiveElement.Builder(rawData.copyOfRange(contentEnd, rawData.size))
                    .labelId(R.string.padding_label)
                    .parent(parent)
                    .build(resources)
        }
        return elements
    }

    private fun tlvElement(
        resources: Resources,
        tlv: RawTlv,
        parent: Element?,
        index: Int,
        prefixFields: List<PrefixField>,
        tagLabels: Map<Int, Int>
    ): Element {
        val labelId = tagLabels[tlv.tag] ?: R.string.telecom_sub_df_tlv_number_label
        val labelArgs = if (tagLabels.containsKey(tlv.tag)) emptyArray() else arrayOf(index)
        return ConstructedElement.Builder(tlv.encoded)
                .labelId(labelId)
                .labelArgs(*labelArgs)
                .parent(parent)
                .decoder { innerResources, _, tlvParent ->
                    tlvDecoder(innerResources, tlv, tlvParent, prefixFields, tagLabels)
                }
                .build(resources)
    }

    private fun tlvDecoder(
        resources: Resources,
        tlv: RawTlv,
        parent: Element?,
        prefixFields: List<PrefixField>,
        tagLabels: Map<Int, Int>
    ): List<Element> {
        val elements = mutableListOf<Element>()
        elements += primitive(resources, tlv.tagBytes, R.string.tlv_tag_label, parent)
        elements += primitive(resources, tlv.lengthBytes, R.string.tlv_length_label, parent)
        var offset = 0
        resolvePrefixFields(tlv.value, prefixFields).forEach { field ->
            if (field.length > 0 && offset + field.length <= tlv.value.size) {
                elements += primitive(
                        resources, tlv.value.copyOfRange(offset, offset + field.length),
                        field.labelId, parent)
                offset += field.length
            }
        }
        val nested = parseTlvs(tlv.value.copyOfRange(offset, tlv.value.size))
        if (nested.isNotEmpty() && nested.sumOf { it.encoded.size } == tlv.value.size - offset) {
            nested.forEachIndexed { index, child ->
                elements += tlvElement(resources, child, parent, index + 1, emptyList(), tagLabels)
            }
        } else if (offset < tlv.value.size) {
            elements += primitive(
                    resources, tlv.value.copyOfRange(offset, tlv.value.size),
                    R.string.tlv_value_label, parent)
        }
        return elements
    }

    private fun resolvePrefixFields(
        value: ByteArray,
        prefixFields: List<PrefixField>
    ): List<PrefixField> {
        if (prefixFields.none { it.length < 0 }) return prefixFields
        val fixedLength = prefixFields.filter { it.length > 0 }.sumOf { it.length }
        val nestedOffset = (fixedLength until value.size).firstOrNull { offset ->
            val nested = parseTlvs(value.copyOfRange(offset, value.size))
            nested.isNotEmpty() && nested.first().tag == 0x80 &&
                    nested.sumOf { it.encoded.size } == value.size - offset
        } ?: return prefixFields
        return prefixFields.map { field ->
            if (field.length < 0) field.copy(length = nestedOffset - fixedLength) else field
        }
    }

    private fun primitive(
        resources: Resources,
        bytes: ByteArray,
        labelId: Int,
        parent: Element?
    ): Element = PrimitiveElement.Builder(bytes)
            .labelId(labelId)
            .parent(parent)
            .build(resources)

    private fun parseTlvs(bytes: ByteArray): List<RawTlv> {
        val result = mutableListOf<RawTlv>()
        var offset = 0
        try {
            while (offset < bytes.size) {
                val start = offset
                val tagStart = offset
                var tag = bytes[offset++].toInt() and 0xFF
                if (tag and 0x1F == 0x1F) {
                    do {
                        val next = bytes[offset++].toInt() and 0xFF
                        tag = (tag shl Byte.SIZE_BITS) or next
                    } while (next and 0x80 != 0)
                }
                val tagBytes = bytes.copyOfRange(tagStart, offset)
                val lengthStart = offset
                val firstLength = bytes[offset++].toInt() and 0xFF
                val length = if (firstLength <= 0x7F) {
                    firstLength
                } else {
                    var value = 0
                    repeat(firstLength and 0x7F) {
                        value = (value shl Byte.SIZE_BITS) or (bytes[offset++].toInt() and 0xFF)
                    }
                    value
                }
                val lengthBytes = bytes.copyOfRange(lengthStart, offset)
                val value = bytes.copyOfRange(offset, offset + length)
                offset += length
                result += RawTlv(
                        tag, tagBytes, lengthBytes, value, bytes.copyOfRange(start, offset))
            }
        } catch (_: RuntimeException) {
            return emptyList()
        }
        return result
    }

    private fun codingInterpreter(
        resources: Resources,
        rawData: ByteArray,
        supportsBinaryFormat: Boolean
    ): String {
        val labelId = when (rawData.first().toInt() and 0xFF) {
            0 -> R.string.xml_coding_label
            1 -> if (supportsBinaryFormat) R.string.specified_coding_label else R.string.rfu_label
            else -> R.string.rfu_label
        }
        return resources.getString(labelId)
    }

    private fun availabilityInterpreter(resources: Resources, rawData: ByteArray): String =
            resources.getString(
                    if (rawData.first().toInt() and 0x01 == 1) R.string.available_label
                    else R.string.not_available_label)
}
