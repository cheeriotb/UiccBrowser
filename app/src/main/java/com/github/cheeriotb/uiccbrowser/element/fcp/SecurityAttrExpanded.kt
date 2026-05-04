/*
 *  Copyright (C) 2020-2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.element.fcp

import android.content.res.Resources
import com.github.cheeriotb.uiccbrowser.R
import com.github.cheeriotb.uiccbrowser.element.BerTlvElement
import com.github.cheeriotb.uiccbrowser.element.Element
import com.github.cheeriotb.uiccbrowser.util.Tlv

// ETSI TS 102 221 Clause 11.1.1.4.7.2
// Security Attribute - Expanded format
class SecurityAttrExpanded {
    companion object {
        const val TAG = 0xAB
        /* const */ val LABEL = R.string.security_attr_expand_label
        val decoder = ::decoderImpl

        const val TAG_SC_ALWAYS_DO = 0x90
        const val TAG_SC_NEVER_DO = 0x97
        const val TAG_CONTROL_DO = 0xA4
        const val TAG_OR_DO = 0xA0
        const val TAG_AND_DO = 0xAF
        const val TAG_NOT_DO = 0xA7
        const val TAG_KEY_REFERENCE = 0x83
        const val TAG_USAGE_QUALIFIER = 0x95

        private fun decoderImpl(
            resources: Resources,
            tlvs: List<Tlv>,
            parent: Element?
        ): List<Element> = decodeTlvs(resources, tlvs, parent, true)

        private fun scDecoderImpl(
            resources: Resources,
            tlvs: List<Tlv>,
            parent: Element?
        ): List<Element> = decodeTlvs(resources, tlvs, parent, false)

        private fun decodeTlvs(
            resources: Resources,
            tlvs: List<Tlv>,
            parent: Element?,
            topLevel: Boolean
        ): List<Element> = tlvs.map { tlv ->
            BerTlvElement.Builder(tlv)
                    .labelId(labelIdFor(tlv.tag, topLevel))
                    .parent(parent)
                    .decoder(::scDecoderImpl)
                    .build(resources)
        }

        private fun labelIdFor(tag: Int, topLevel: Boolean): Int = when {
            topLevel && tag in 0x80..0x8F -> R.string.am_do_label
            tag == TAG_SC_ALWAYS_DO -> R.string.sc_do_always_label
            tag == TAG_SC_NEVER_DO -> R.string.sc_do_never_label
            tag == TAG_CONTROL_DO -> R.string.control_do_label
            tag == TAG_KEY_REFERENCE -> R.string.key_reference_label
            tag == TAG_USAGE_QUALIFIER -> R.string.usage_qualifier_label
            tag == TAG_OR_DO -> R.string.or_do_label
            tag == TAG_AND_DO -> R.string.and_do_label
            tag == TAG_NOT_DO -> R.string.not_do_label
            else -> R.string.unknown_label
        }
    }
}
