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

// ETSI TS 131 102, clause 4.6.1.
class GraphicsEfDecoders {
    companion object {
        private const val DESCRIPTOR_LENGTH = 9

        /** Decodes one EFIMG record into its image instance descriptors. */
        fun decodeImg(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < 10 || bytes.size !in listOf(
                            1 + (bytes[0].toInt() and 0xFF) * DESCRIPTOR_LENGTH,
                            2 + (bytes[0].toInt() and 0xFF) * DESCRIPTOR_LENGTH)) return null
            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_img_label)
                    .decoder(::imgDecoder)
                    .build(resources)
        }

        /** Decodes an EFIIDF into its image instance data element. */
        fun decodeIidf(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty()) return null
            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_iidf_label)
                    .decoder(::iidfDecoder)
                    .build(resources)
        }

        private fun imgDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val count = rawData[0].toInt() and 0xFF
            val elements = mutableListOf<Element>()
            elements += primitive(resources, rawData.copyOfRange(0, 1),
                    R.string.actual_image_instances_label, parent, ::unsignedInterpreter)
            repeat(count) { index ->
                val start = 1 + index * DESCRIPTOR_LENGTH
                elements += ConstructedElement.Builder(
                        rawData.copyOfRange(start, start + DESCRIPTOR_LENGTH))
                        .labelId(R.string.image_instance_descriptor_label)
                        .labelArgs(index + 1)
                        .parent(parent)
                        .decoder(::descriptorDecoder)
                        .build(resources)
            }
            if (rawData.size > 1 + count * DESCRIPTOR_LENGTH) {
                elements += primitive(
                        resources, rawData.copyOfRange(rawData.lastIndex, rawData.size),
                        R.string.rfu_label, parent)
            }
            return elements
        }

        private fun descriptorDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                primitive(resources, rawData.copyOfRange(0, 1), R.string.image_width_label, parent,
                        ::unsignedInterpreter),
                primitive(resources, rawData.copyOfRange(1, 2), R.string.image_height_label, parent,
                        ::unsignedInterpreter),
                primitive(resources, rawData.copyOfRange(2, 3), R.string.image_coding_scheme_label,
                        parent, ::codingSchemeInterpreter),
                primitive(resources, rawData.copyOfRange(3, 5), R.string.image_data_file_id_label,
                        parent),
                primitive(resources, rawData.copyOfRange(5, 7), R.string.image_data_offset_label,
                        parent, ::unsignedInterpreter),
                primitive(resources, rawData.copyOfRange(7, 9), R.string.image_data_length_label,
                        parent, ::unsignedInterpreter)
        )

        private fun iidfDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                primitive(resources, rawData, R.string.image_instance_data_label, parent)
        )

        private fun primitive(
            resources: Resources,
            bytes: ByteArray,
            labelId: Int,
            parent: Element?,
            interpreter: (Resources, ByteArray) -> String = PrimitiveElement::defaultInterpreter
        ): Element = PrimitiveElement.Builder(bytes)
                .labelId(labelId)
                .parent(parent)
                .interpreter(interpreter)
                .build(resources)

        private fun unsignedInterpreter(resources: Resources, rawData: ByteArray): String {
            val value = rawData.fold(0) { result, byte ->
                (result shl Byte.SIZE_BITS) or (byte.toInt() and 0xFF)
            }
            return "${byteArrayToHexString(rawData)} ($value)"
        }

        private fun codingSchemeInterpreter(resources: Resources, rawData: ByteArray): String {
            val label = when (rawData.first().toInt() and 0xFF) {
                0x11 -> R.string.basic_image_coding_label
                0x21 -> R.string.colour_image_coding_label
                0x22 -> R.string.transparent_colour_image_coding_label
                else -> R.string.rfu_label
            }
            return "${byteArrayToHexString(rawData)} (${resources.getString(label)})"
        }
    }
}
