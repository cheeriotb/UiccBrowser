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
import com.github.cheeriotb.uiccbrowser.element.Element
import com.github.cheeriotb.uiccbrowser.util.BerTlv
import com.github.cheeriotb.uiccbrowser.util.Tlv

// ETSI TS 102.221 Table 13.1, 13.2
// EF DIR - Application template
class AppTemplate {
    companion object {
        const val TAG_APPLICATION_TEMPLATE = 0x61
        const val TAG_APPLICATION_ID = 0x4F
        const val TAG_APPLICATION_LABEL = 0x50
        const val TAG_PATH = 0x51
        const val TAG_COMMAND_APDU = 0x52
        const val TAG_DISCRETIONARY_DATA = 0x53
        const val TAG_DISCRETIONARY_TEMPLATE = 0x73
        const val TAG_URL = 0x5F50

        /**
         * Parses one EF DIR record byte array and returns the Application template as a
         * BerTlvElement tree. Returns null if the bytes do not start with an Application template
         * (tag 0x61) or cannot be parsed.
         *
         * Only the bytes belonging to the first TLV object are passed to BerTlv, so that trailing
         * padding bytes (0xFF fill common in linear-fixed EFs) do not confuse the parser.
         */
        fun decode(resources: Resources, bytes: ByteArray): BerTlvElement? {
            val tlvs = BerTlv.listFrom(bytes)
            if (tlvs.isEmpty() || tlvs[0].tlvs.isEmpty()
                    || tlvs[0].tag != TAG_APPLICATION_TEMPLATE) return null

            return BerTlvElement.Builder(tlvs[0])
                    .labelId(R.string.app_template_label)
                    .decoder(::appTemplateDecoder)
                    .build(resources)
        }

        // ETSI TS 102.221 Table 13.2 — Application template contents
        private fun appTemplateDecoder(
            resources: Resources,
            tlvs: List<Tlv>,
            parent: Element?
        ): List<Element> {
            val list = mutableListOf<Element>()

            tlvs.forEach { tlv ->
                when (tlv.tag) {
                    // ETSI TS 102.221 Clause 13.1 — Application Identifier
                    TAG_APPLICATION_ID -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.app_id_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // ETSI TS 102.221 Clause 13.1 — Application Label
                    TAG_APPLICATION_LABEL -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.app_label_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // ETSI TS 102.221 Clause 13.1 — Path
                    TAG_PATH -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.app_path_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // ETSI TS 102.221 Clause 13.1 — Command APDU
                    TAG_COMMAND_APDU -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.command_apdu_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // ETSI TS 102.221 Clause 13.1 — Discretionary data
                    TAG_DISCRETIONARY_DATA -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.discretionary_data_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // ETSI TS 102.221 Clause 13.1 — Discretionary template
                    TAG_DISCRETIONARY_TEMPLATE -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.discretionary_template_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // ETSI TS 102.221 Clause 13.1 — URL
                    TAG_URL -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .labelId(R.string.url_label)
                                .parent(parent)
                                .build(resources))
                    }
                    // Additional TLV objects may be present.
                    else -> {
                        list.add(BerTlvElement.Builder(tlv)
                                .parent(parent)
                                .build(resources))
                    }
                }
            }

            return list
        }
    }
}
