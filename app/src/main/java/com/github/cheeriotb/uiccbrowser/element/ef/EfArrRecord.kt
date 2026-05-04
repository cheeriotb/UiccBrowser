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
import com.github.cheeriotb.uiccbrowser.element.fcp.SecurityAttrExpanded
import com.github.cheeriotb.uiccbrowser.util.BerTlv

// ETSI TS 102 221 Clause 13.1 and ISO/IEC 7816-4 security attributes.
// EF ARR - Access Rule Reference record.
class EfArrRecord {
    companion object {
        fun decode(resources: Resources, bytes: ByteArray): Element? {
            if (BerTlv.listFrom(bytes).isEmpty()) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_arr_record_label)
                    .decoder(::efArrRecordDecoder)
                    .build(resources)
        }

        private fun efArrRecordDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val tlvs = BerTlv.listFrom(rawData)
            if (tlvs.isEmpty()) return listOf()

            return SecurityAttrExpanded.decoder(resources, tlvs, parent)
        }
    }
}
