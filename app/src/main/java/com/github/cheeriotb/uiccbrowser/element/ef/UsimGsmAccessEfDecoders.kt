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

// ETSI TS 131 102, clause 4.4.3.
class UsimGsmAccessEfDecoders {
    companion object {
        private const val CIPHERING_KEY_FILE_LENGTH = 9
        private const val CPBCCH_ENTRY_LENGTH = 2
        private const val INVSCAN_LENGTH = 1
        private const val BAND_INDICATOR_MASK = 0x04
        private const val RFU_MASK = 0x78
        private const val EMPTY_INDICATOR_MASK = 0x80

        /**
         * Decodes EF Kc into the GSM ciphering key and key sequence number.
         */
        fun decodeKc(resources: Resources, bytes: ByteArray): Element? =
                decodeCipheringKeyFile(
                        resources, bytes, R.string.ef_kc_label,
                        R.string.gsm_ciphering_key_label
                )

        /**
         * Decodes EF KcGPRS into the GPRS ciphering key and key sequence number.
         */
        fun decodeKcGprs(resources: Resources, bytes: ByteArray): Element? =
                decodeCipheringKeyFile(
                        resources, bytes, R.string.ef_kc_gprs_label,
                        R.string.gprs_ciphering_key_label
                )

        /**
         * Decodes EF CPBCCH into indexed carrier list entries and their bit fields.
         */
        fun decodeCpbcch(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty() || bytes.size % CPBCCH_ENTRY_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_cpbcch_label)
                    .decoder(::cpbcchDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF InvScan into the investigation scan indication.
         */
        fun decodeInvScan(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != INVSCAN_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_invscan_label)
                    .decoder { innerResources, rawData, parent ->
                        listOf(
                                PrimitiveElement.Builder(rawData)
                                        .labelId(R.string.investigation_scan_label)
                                        .parent(parent)
                                        .interpreter(::investigationScanInterpreter)
                                        .build(innerResources)
                        )
                    }
                    .build(resources)
        }

        private fun decodeCipheringKeyFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int,
            keyLabelId: Int
        ): Element? {
            if (bytes.size != CIPHERING_KEY_FILE_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder { innerResources, rawData, parent ->
                        cipheringKeyDecoder(innerResources, rawData, parent, keyLabelId)
                    }
                    .build(resources)
        }

        private fun cipheringKeyDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?,
            keyLabelId: Int
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 8))
                        .labelId(keyLabelId)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(8, 9))
                        .labelId(R.string.ciphering_key_sequence_number_label)
                        .parent(parent)
                        .interpreter(::keySequenceNumberInterpreter)
                        .build(resources)
        )

        private fun cpbcchDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.asIterable().chunked(CPBCCH_ENTRY_LENGTH).mapIndexed { index, entry ->
                val entryData = entry.toByteArray()
                ConstructedElement.Builder(entryData)
                        .labelId(R.string.cpbcch_carrier_list_element_label)
                        .labelArgs(index + 1)
                        .parent(parent)
                        .decoder(::cpbcchEntryDecoder)
                        .dataComposer { entryData }
                        .interpreter(::arfcnInterpreter)
                        .build(resources)
            }
        }

        private fun cpbcchEntryDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val secondByte = rawData[1].toInt() and 0xFF
            val bandIndicator = if (secondByte and BAND_INDICATOR_MASK == 0) 0x00 else 0x01
            val rfu = (secondByte and RFU_MASK) shr 3
            val emptyIndicator = if (secondByte and EMPTY_INDICATOR_MASK == 0) 0x00 else 0x01
            return listOf(
                    PrimitiveElement.Builder(rawData)
                            .labelId(R.string.arfcn_label)
                            .parent(parent)
                            .interpreter(::arfcnInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(byteArrayOf(bandIndicator.toByte()))
                            .labelId(R.string.high_low_band_indicator_label)
                            .parent(parent)
                            .interpreter(::bandIndicatorInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(byteArrayOf(rfu.toByte()))
                            .labelId(R.string.rfu_label)
                            .parent(parent)
                            .build(resources),
                    PrimitiveElement.Builder(byteArrayOf(emptyIndicator.toByte()))
                            .labelId(R.string.empty_indicator_label)
                            .parent(parent)
                            .interpreter(::emptyIndicatorInterpreter)
                            .build(resources)
            )
        }

        private fun keySequenceNumberInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.first().toInt() and 0x07
            val interpretation = if (value == 0x07) {
                resources.getString(R.string.ciphering_key_not_available)
            } else {
                value.toString()
            }
            return "${byteArrayToHexString(rawData)} ($interpretation)"
        }

        private fun arfcnInterpreter(resources: Resources, rawData: ByteArray): String {
            val value = (rawData[0].toInt() and 0xFF) or
                    ((rawData[1].toInt() and 0x03) shl 8)
            return "${byteArrayToHexString(rawData)} ($value)"
        }

        private fun bandIndicatorInterpreter(resources: Resources, rawData: ByteArray): String {
            val interpretation = if (rawData.first().toInt() and 0x01 == 0) {
                resources.getString(R.string.lower_band_label)
            } else {
                resources.getString(R.string.higher_band_label)
            }
            return "${byteArrayToHexString(rawData)} ($interpretation)"
        }

        private fun emptyIndicatorInterpreter(resources: Resources, rawData: ByteArray): String {
            val interpretation = if (rawData.first().toInt() and 0x01 == 0) {
                resources.getString(R.string.cpbcch_carrier_present)
            } else {
                resources.getString(R.string.cpbcch_carrier_empty)
            }
            return "${byteArrayToHexString(rawData)} ($interpretation)"
        }

        private fun investigationScanInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val interpretation = when (rawData.first().toInt() and 0xFF) {
                0x00 -> resources.getString(R.string.investigation_scan_not_required)
                0x01 -> resources.getString(R.string.investigation_scan_required)
                else -> resources.getString(R.string.rfu_label)
            }
            return "${byteArrayToHexString(rawData)} ($interpretation)"
        }
    }
}
