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

// ETSI TS 131 103, clauses 4.2.2 to 4.2.4.
class IsimEfDecoders {
    companion object {
        private const val TAG_URI = 0x80

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
    }
}
