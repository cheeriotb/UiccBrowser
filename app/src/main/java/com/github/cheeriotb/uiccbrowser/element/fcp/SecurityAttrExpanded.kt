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
import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString

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
        const val TAG_PROPRIETARY_STATE_MACHINE_DESCRIPTION = 0x9C

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
                    .interpreter(interpreterFor(tlv.tag, topLevel))
                    .build(resources)
        }

        private fun labelIdFor(tag: Int, topLevel: Boolean): Int = when {
            topLevel && tag in 0x80..0x8F -> R.string.am_do_label
            topLevel && tag == TAG_PROPRIETARY_STATE_MACHINE_DESCRIPTION ->
                R.string.proprietary_state_machine_label
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

        private fun interpreterFor(
            tag: Int,
            topLevel: Boolean
        ): (Resources, ByteArray) -> String = when {
            topLevel && tag == 0x80 -> ::accessModeInterpreter
            topLevel && tag in 0x81..0x8F -> commandDescriptionInterpreter(tag)
            tag == TAG_KEY_REFERENCE -> ::keyReferenceInterpreter
            else -> BerTlvElement::defaultInterpreter
        }

        internal fun accessModeInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val hex = byteArrayToHexString(rawData)
            if (rawData.size != 1) return hex

            val accessMode = rawData[0].toInt() and 0xFF
            val modes = ACCESS_MODE_BITS
                    .filter { (mask, _) -> accessMode and mask != 0 }
                    .joinToString(", ") { (_, name) -> name }

            return if (modes.isEmpty()) hex else "$hex ($modes)"
        }

        /**
         * ISO/IEC 7816-4 command description AM_DO.
         *
         * For tags 81..8F, bits b4..b1 show whether CLA, INS, P1 and P2 are present.
         * CLA zero is independent from logical channels; even INS is independent from
         * data field format indications.
         */
        private fun commandDescriptionInterpreter(
            tag: Int
        ): (Resources, ByteArray) -> String = { _, rawData ->
            val hex = byteArrayToHexString(rawData)
            val fields = COMMAND_HEADER_FIELDS
                    .filter { (mask, _) -> tag and mask != 0 }

            if (fields.isEmpty() || rawData.size != fields.size) {
                hex
            } else {
                val description = fields
                        .mapIndexed { index, (_, name) ->
                            "$name ${byteArrayToHexString(byteArrayOf(rawData[index]))}"
                        }
                        .joinToString(", ")
                "$hex ($description)"
            }
        }

        internal fun keyReferenceInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val hex = byteArrayToHexString(rawData)
            if (rawData.size != 1) return hex

            val reference = rawData[0].toInt() and 0xFF
            val key = when (reference) {
                0x01 -> resources.getString(R.string.key_reference_application_pin1)
                0x02 -> resources.getString(R.string.key_reference_application_pin2)
                0x03 -> resources.getString(R.string.key_reference_application_pin3)
                0x04 -> resources.getString(R.string.key_reference_application_pin4)
                0x05 -> resources.getString(R.string.key_reference_application_pin5)
                0x06 -> resources.getString(R.string.key_reference_application_pin6)
                0x07 -> resources.getString(R.string.key_reference_application_pin7)
                0x08 -> resources.getString(R.string.key_reference_application_pin8)
                in 0x0A..0x0E -> "ADM${reference - 0x09}"
                0x11 -> "Universal PIN"
                in 0x81..0x88 -> "Local PIN${reference - 0x80}"
                in 0x8A..0x8E -> "ADM${reference - 0x84}"
                else -> null
            }

            return if (key == null) hex else "$hex ($key)"
        }

        private val ACCESS_MODE_BITS = listOf(
                0x80 to "RESERVED",
                0x40 to "ADMIN",
                0x20 to "DELETE",
                0x10 to "CREATE",
                0x08 to "DEACTIVATE",
                0x04 to "ACTIVATE",
                0x02 to "UPDATE",
                0x01 to "READ"
        )

        private val COMMAND_HEADER_FIELDS = listOf(
                0x08 to "CLA",
                0x04 to "INS",
                0x02 to "P1",
                0x01 to "P2"
        )
    }
}
