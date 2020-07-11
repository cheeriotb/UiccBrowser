/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
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
        const val LABEL = R.string.security_attr_expand_label
        val decoder = ::decoderImpl

        private fun decoderImpl(
            resources: Resources,
            tlvs: List<Tlv>,
            parent: Element?
        ): List<Element> {
            val list: MutableList<Element> = mutableListOf()

            tlvs.forEach { tlv ->
                list.add(
                    if (list.size % 2 == 0) {
                        // Access mode data object (Tag: 8X)
                        BerTlvElement.Builder(tlv)
                                .labelId(R.string.am_do_label)
                                .parent(parent)
                                .build(resources)
                    } else {
                        // Security condition data object
                        BerTlvElement.Builder(tlv)
                                .labelId(R.string.sc_do_label)
                                .parent(parent)
                                .build(resources)
                    }
                )
            }

            return list
        }
    }
}
