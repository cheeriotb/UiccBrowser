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
import com.github.cheeriotb.uiccbrowser.util.StringUtils
import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString
import kotlin.math.pow
import java.util.Locale

// ETSI TS 131 102, clauses 4.2.1 to 4.2.38.
class UsimEfDecoders {
    companion object {
        private const val IMSI_LENGTH = 9
        private const val KEYS_LENGTH = 33
        private const val HPPLMN_LENGTH = 1
        private const val ACM_MAX_LENGTH = 3
        private const val SPN_LENGTH = 17
        private const val PUCT_LENGTH = 5
        private const val ACC_LENGTH = 2
        private const val LOCI_LENGTH = 11
        private const val MIN_AD_LENGTH = 4
        private const val LI_ENTRY_LENGTH = 2
        private const val CBMI_ENTRY_LENGTH = 2
        private const val CBMIR_ENTRY_LENGTH = 4
        private const val ECC_MIN_LENGTH = 3
        private const val PSLOCI_LENGTH = 14
        private const val DIALING_NUMBER_TRAILER_LENGTH = 14
        private const val SMS_LENGTH = 176
        private const val SMSP_TRAILER_LENGTH = 28
        private const val SMSS_LENGTH = 2
        private const val SMSR_MIN_LENGTH = 30
        private const val EXTENSION_RECORD_LENGTH = 13
        private const val CCP2_MIN_LENGTH = 15
        private const val FPLMN_ENTRY_LENGTH = 3
        private const val PLMN_W_ACT_ENTRY_LENGTH = 5
        private const val MIN_PLMN_W_ACT_LENGTH = 40
        private const val MIN_FPLMN_LENGTH = 12

        /**
         * Decodes EF LI into ordered two-byte ISO 639 language entries. Unused entries are
         * represented by FF FF and displayed without passing through string decoding.
         */
        fun decodeLi(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty() || bytes.size % LI_ENTRY_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_li_label)
                    .decoder(::liDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF IMSI into length and IMSI bytes. The IMSI bytes keep their raw hex value and
         * also display the decimal IMSI digits encoded in swapped BCD nibbles.
         */
        fun decodeImsi(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != IMSI_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_imsi_label)
                    .decoder(::imsiDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF Keys into KSI, ciphering key, and integrity key.
         */
        fun decodeKeys(resources: Resources, bytes: ByteArray): Element? =
                decodeKeysFile(
                        resources,
                        bytes,
                        R.string.ef_keys_label,
                        R.string.key_set_identifier_label,
                        R.string.ciphering_key_label,
                        R.string.integrity_key_label
                )

        /**
         * Decodes EF KeysPS into KSIPS, packet-switched ciphering key, and integrity key.
         */
        fun decodeKeysPs(resources: Resources, bytes: ByteArray): Element? =
                decodeKeysFile(
                        resources,
                        bytes,
                        R.string.ef_keys_ps_label,
                        R.string.key_set_identifier_ps_label,
                        R.string.ciphering_key_ps_label,
                        R.string.integrity_key_ps_label
                )

        /**
         * Decodes EF PLMNwAcT into five-byte priority entries containing PLMN and access
         * technology masks.
         */
        fun decodePlmnWAct(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < MIN_PLMN_W_ACT_LENGTH ||
                    bytes.size % PLMN_W_ACT_ENTRY_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_plmn_w_act_label)
                    .decoder(::plmnWActDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF HPPLMN into the configured higher priority PLMN search interval.
         */
        fun decodeHpplmn(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != HPPLMN_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_hpplmn_label)
                    .decoder(::hpplmnDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF ACMmax into the three-byte binary maximum accumulated call meter value.
         */
        fun decodeAcmMax(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != ACM_MAX_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_acm_max_label)
                    .decoder(::acmMaxDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF UST into one child for each advertised USIM service bit.
         */
        fun decodeUst(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty()) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_ust_label)
                    .decoder(::ustDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF GID1 into the operator-defined USIM group identifier bytes.
         */
        fun decodeGid1(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleElementFile(
                        resources,
                        bytes,
                        R.string.ef_gid1_label,
                        R.string.usim_group_identifiers_label
                )

        /**
         * Decodes EF GID2 into the operator-defined USIM group identifier bytes.
         */
        fun decodeGid2(resources: Resources, bytes: ByteArray): Element? =
                decodeSingleElementFile(
                        resources,
                        bytes,
                        R.string.ef_gid2_label,
                        R.string.usim_group_identifiers_label
                )

        /**
         * Decodes EF SPN into display conditions and the service provider name string.
         */
        fun decodeSpn(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != SPN_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_spn_label)
                    .decoder(::spnDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF PUCT into the three-character currency code and price-per-unit data.
         */
        fun decodePuct(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != PUCT_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_puct_label)
                    .decoder(::puctDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF CBMI into two-byte Cell Broadcast message identifiers.
         */
        fun decodeCbmi(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty() || bytes.size % CBMI_ENTRY_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_cbmi_label)
                    .decoder(::cbmiDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF ACC into allocated access control classes.
         */
        fun decodeAcc(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != ACC_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_acc_label)
                    .decoder(::accDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF FPLMN into forbidden PLMN entries.
         */
        fun decodeFplmn(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < MIN_FPLMN_LENGTH || bytes.size % FPLMN_ENTRY_LENGTH != 0) {
                return null
            }

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_fplmn_label)
                    .decoder(::fplmnDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF LOCI into TMSI, LAI, RFU, and location update status.
         */
        fun decodeLoci(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != LOCI_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_loci_label)
                    .decoder(::lociDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF AD into UE operation mode, additional information, MNC length, and RFU bytes.
         */
        fun decodeAd(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < MIN_AD_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_ad_label)
                    .decoder(::adDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF CBMID into two-byte Cell Broadcast message identifiers.
         */
        fun decodeCbmid(resources: Resources, bytes: ByteArray): Element? =
                decodeCbMessageIdentifierFile(resources, bytes, R.string.ef_cbmid_label)

        /**
         * Decodes EF ECC into emergency call code, alpha identifier, and service category.
         */
        fun decodeEcc(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size <= ECC_MIN_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_ecc_label)
                    .decoder(::eccDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF CBMIR into lower and upper Cell Broadcast message identifier ranges.
         */
        fun decodeCbmir(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.isEmpty() || bytes.size % CBMIR_ENTRY_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_cbmir_label)
                    .decoder(::cbmirDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF PSLOCI into packet-switched location information.
         */
        fun decodePsloci(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != PSLOCI_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_psloci_label)
                    .decoder(::pslociDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF FDN into alpha identifier, dialling number, CCP2, and EXT2 references.
         */
        fun decodeFdn(resources: Resources, bytes: ByteArray): Element? =
                decodeDiallingNumberFile(
                        resources,
                        bytes,
                        R.string.ef_fdn_label,
                        R.string.ccp2_record_identifier_label,
                        R.string.extension2_record_identifier_label
                )

        /**
         * Decodes EF SMS into status and TPDU bytes.
         */
        fun decodeSms(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != SMS_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_sms_label)
                    .decoder(::smsDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF MSISDN into alpha identifier, dialling number, CCP2, and EXT1 references.
         */
        fun decodeMsisdn(resources: Resources, bytes: ByteArray): Element? =
                decodeDiallingNumberFile(
                        resources,
                        bytes,
                        R.string.ef_msisdn_label,
                        R.string.ccp2_record_identifier_label,
                        R.string.identifier_label
                )

        /**
         * Decodes EF SMSP into alpha identifier and SMS parameter bytes.
         */
        fun decodeSmsp(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size <= SMSP_TRAILER_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_smsp_label)
                    .decoder(::smspDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF SMSS into message reference and memory capacity exceeded flag.
         */
        fun decodeSmss(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size != SMSS_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_smss_label)
                    .decoder(::smssDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF SDN into alpha identifier, dialling number, CCP2, and EXT3 references.
         */
        fun decodeSdn(resources: Resources, bytes: ByteArray): Element? =
                decodeDiallingNumberFile(
                        resources,
                        bytes,
                        R.string.ef_sdn_label,
                        R.string.ccp2_record_identifier_label,
                        R.string.extension3_record_identifier_label
                )

        /**
         * Decodes EF EXT2 into one extension record.
         */
        fun decodeExt2(resources: Resources, bytes: ByteArray): Element? =
                decodeExtensionRecordFile(resources, bytes, R.string.ef_ext2_label)

        /**
         * Decodes EF EXT3 into one extension record.
         */
        fun decodeExt3(resources: Resources, bytes: ByteArray): Element? =
                decodeExtensionRecordFile(resources, bytes, R.string.ef_ext3_label)

        /**
         * Decodes EF SMSR into SMS record identifier and status report bytes.
         */
        fun decodeSmsr(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < SMSR_MIN_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_smsr_label)
                    .decoder(::smsrDecoder)
                    .build(resources)
        }

        /**
         * Decodes EF EXT5 into one extension record.
         */
        fun decodeExt5(resources: Resources, bytes: ByteArray): Element? =
                decodeExtensionRecordFile(resources, bytes, R.string.ef_ext5_label)

        /**
         * Decodes EF CCP2 into the bearer capability information element.
         */
        fun decodeCcp2(resources: Resources, bytes: ByteArray): Element? {
            if (bytes.size < CCP2_MIN_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(R.string.ef_ccp2_label)
                    .decoder(::ccp2Decoder)
                    .build(resources)
        }

        private fun liDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.asIterable().chunked(LI_ENTRY_LENGTH).mapIndexed { index, entry ->
                PrimitiveElement.Builder(entry.toByteArray())
                        .labelId(R.string.language_code_label)
                        .labelArgs(index + 1)
                        .parent(parent)
                        .interpreter(::languageCodeInterpreter)
                        .build(resources)
            }
        }

        private fun imsiDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                        .labelId(R.string.length_of_imsi_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(1, IMSI_LENGTH))
                        .labelId(R.string.imsi_label)
                        .parent(parent)
                        .interpreter(::imsiInterpreter)
                        .build(resources)
        )

        private fun decodeKeysFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int,
            ksiLabelId: Int,
            ckLabelId: Int,
            ikLabelId: Int
        ): Element? {
            if (bytes.size != KEYS_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder { innerResources, rawData, parent ->
                        keyFileDecoder(innerResources, rawData, parent, ksiLabelId, ckLabelId,
                                ikLabelId)
                    }
                    .build(resources)
        }

        private fun keyFileDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?,
            ksiLabelId: Int,
            ckLabelId: Int,
            ikLabelId: Int
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                        .labelId(ksiLabelId)
                        .parent(parent)
                        .interpreter(::keySetIdentifierInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(1, 17))
                        .labelId(ckLabelId)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(17, KEYS_LENGTH))
                        .labelId(ikLabelId)
                        .parent(parent)
                        .build(resources)
        )

        private fun plmnWActDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.asIterable().chunked(PLMN_W_ACT_ENTRY_LENGTH).mapIndexed {
                index, entry ->
                    ConstructedElement.Builder(entry.toByteArray())
                            .labelId(R.string.plmn_w_act_entry_label)
                            .labelArgs(index + 1)
                            .parent(parent)
                            .decoder { innerResources, entryData, entryParent ->
                                plmnWActEntryDecoder(innerResources, entryData, entryParent)
                            }
                            .interpreter { _, _ -> (index + 1).toString() }
                            .build(resources)
            }
        }

        private fun plmnWActEntryDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val plmn = rawData.copyOfRange(0, 3)
            val accessTechnology = rawData.copyOfRange(3, PLMN_W_ACT_ENTRY_LENGTH)
            val accessTechnologyBuilder = PrimitiveElement.Builder(accessTechnology)
                    .labelId(R.string.access_technology_identifier_label)
                    .parent(parent)
            if (!isUnusedPlmn(plmn)) {
                accessTechnologyBuilder.interpreter(::accessTechnologyInterpreter)
            }
            return listOf(
                PrimitiveElement.Builder(plmn)
                        .labelId(R.string.plmn_label)
                        .parent(parent)
                        .interpreter(::plmnInterpreter)
                        .build(resources),
                accessTechnologyBuilder.build(resources)
            )
        }

        private fun hpplmnDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData)
                        .labelId(R.string.time_interval_label)
                        .parent(parent)
                        .interpreter(::hpplmnInterpreter)
                        .build(resources)
        )

        private fun acmMaxDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData)
                        .labelId(R.string.maximum_value_label)
                        .parent(parent)
                        .interpreter(::unsignedIntegerInterpreter)
                        .build(resources)
        )

        private fun ustDecoder(
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
                            ustByteDecoder(innerResources, byteData, byteParent, byteIndex)
                        }
                        .dataComposer(::ustByteDataComposer)
                        .build(resources)
            }
        }

        private fun ustByteDataComposer(elements: List<Element>): ByteArray {
            val value = elements.take(8).foldIndexed(0) { bitIndex, acc, element ->
                if ((element.data.firstOrNull()?.toInt()?.and(0x01) ?: 0) == 1) {
                    acc or (1 shl bitIndex)
                } else {
                    acc
                }
            }
            return byteArrayOf(value.toByte())
        }

        private fun ustByteDecoder(
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
                        .labelArgs(serviceNumber, usimServiceName(resources, serviceNumber))
                        .parent(parent)
                        .interpreter(::usimServiceStateInterpreter)
                        .build(resources)
            }
        }

        private fun decodeSingleElementFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int,
            childLabelId: Int
        ): Element? {
            if (bytes.isEmpty()) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder { innerResources, rawData, parent ->
                        listOf(
                                PrimitiveElement.Builder(rawData)
                                        .labelId(childLabelId)
                                        .parent(parent)
                                        .build(innerResources)
                        )
                    }
                    .build(resources)
        }

        private fun spnDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                        .labelId(R.string.display_condition_label)
                        .parent(parent)
                        .interpreter(::spnDisplayConditionInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(1, SPN_LENGTH))
                        .labelId(R.string.service_provider_name_label)
                        .parent(parent)
                        .interpreter(::alphaIdentifierInterpreter)
                        .build(resources)
        )

        private fun puctDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 3))
                        .labelId(R.string.currency_code_label)
                        .parent(parent)
                        .interpreter(::alphaIdentifierInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(3, PUCT_LENGTH))
                        .labelId(R.string.price_per_unit_label)
                        .parent(parent)
                        .interpreter(::pricePerUnitInterpreter)
                        .build(resources)
        )

        private fun cbmiDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.asIterable().chunked(CBMI_ENTRY_LENGTH).map {
                PrimitiveElement.Builder(it.toByteArray())
                        .labelId(R.string.cb_message_identifier_label)
                        .parent(parent)
                        .interpreter(::cbMessageIdentifierInterpreter)
                        .build(resources)
            }
        }

        private fun accDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData)
                        .labelId(R.string.access_control_classes_label)
                        .parent(parent)
                        .interpreter(::accessControlClassesInterpreter)
                        .build(resources)
        )

        private fun fplmnDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.asIterable().chunked(FPLMN_ENTRY_LENGTH).mapIndexed { index, entry ->
                PrimitiveElement.Builder(entry.toByteArray())
                        .labelId(R.string.plmn_number_label)
                        .labelArgs(index + 1)
                        .parent(parent)
                        .interpreter(::plmnInterpreter)
                        .build(resources)
            }
        }

        private fun lociDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 4))
                        .labelId(R.string.tmsi_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(4, 9))
                        .labelId(R.string.location_area_information_label)
                        .parent(parent)
                        .interpreter(::locationAreaInformationInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(9, 10))
                        .labelId(R.string.rfu_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(10, LOCI_LENGTH))
                        .labelId(R.string.location_update_status_label)
                        .parent(parent)
                        .interpreter(::locationUpdateStatusInterpreter)
                        .build(resources)
        )

        private fun adDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val elements = mutableListOf(
                    PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                            .labelId(R.string.ue_operation_mode_label)
                            .parent(parent)
                            .interpreter(::ueOperationModeInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(1, 3))
                            .labelId(R.string.additional_information_label)
                            .parent(parent)
                            .interpreter(::additionalInformationInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(3, 4))
                            .labelId(R.string.length_of_mnc_in_imsi_label)
                            .parent(parent)
                            .interpreter(::mncLengthInterpreter)
                            .build(resources)
            )
            if (rawData.size > MIN_AD_LENGTH) {
                elements.add(
                        PrimitiveElement.Builder(rawData.copyOfRange(MIN_AD_LENGTH, rawData.size))
                                .labelId(R.string.rfu_label)
                                .parent(parent)
                                .build(resources)
                )
            }
            return elements
        }

        private fun decodeCbMessageIdentifierFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int
        ): Element? {
            if (bytes.isEmpty() || bytes.size % CBMI_ENTRY_LENGTH != 0) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder(::cbmiDecoder)
                    .build(resources)
        }

        private fun eccDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, ECC_MIN_LENGTH))
                        .labelId(R.string.emergency_call_code_label)
                        .parent(parent)
                        .interpreter(::emergencyCallCodeInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(
                        rawData.copyOfRange(ECC_MIN_LENGTH, rawData.lastIndex))
                        .labelId(R.string.alpha_identifier_label)
                        .parent(parent)
                        .interpreter(::alphaIdentifierInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(rawData.lastIndex, rawData.size))
                        .labelId(R.string.service_category_label)
                        .parent(parent)
                        .build(resources)
        )

        private fun cbmirDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            return rawData.asIterable().chunked(CBMIR_ENTRY_LENGTH).mapIndexed { index, entry ->
                ConstructedElement.Builder(entry.toByteArray())
                        .labelId(R.string.cb_message_identifier_range_label)
                        .labelArgs(index + 1)
                        .parent(parent)
                        .decoder { innerResources, entryData, entryParent ->
                            cbmirEntryDecoder(innerResources, entryData, entryParent)
                        }
                        .interpreter { _, _ -> (index + 1).toString() }
                        .build(resources)
            }
        }

        private fun cbmirEntryDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 2))
                        .labelId(R.string.lower_cb_message_identifier_label)
                        .parent(parent)
                        .interpreter(::cbMessageIdentifierInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(2, CBMIR_ENTRY_LENGTH))
                        .labelId(R.string.upper_cb_message_identifier_label)
                        .parent(parent)
                        .interpreter(::cbMessageIdentifierInterpreter)
                        .build(resources)
        )

        private fun pslociDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 4))
                        .labelId(R.string.p_tmsi_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(4, 7))
                        .labelId(R.string.p_tmsi_signature_value_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(7, 13))
                        .labelId(R.string.routing_area_information_label)
                        .parent(parent)
                        .interpreter(::routingAreaInformationInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(13, PSLOCI_LENGTH))
                        .labelId(R.string.routing_area_update_status_label)
                        .parent(parent)
                        .interpreter(::routingAreaUpdateStatusInterpreter)
                        .build(resources)
        )

        private fun decodeDiallingNumberFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int,
            ccpLabelId: Int,
            extensionLabelId: Int
        ): Element? {
            if (bytes.size <= DIALING_NUMBER_TRAILER_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder { innerResources, rawData, parent ->
                        diallingNumberDecoder(
                                innerResources,
                                rawData,
                                parent,
                                ccpLabelId,
                                extensionLabelId
                        )
                    }
                    .build(resources)
        }

        private fun diallingNumberDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?,
            ccpLabelId: Int,
            extensionLabelId: Int
        ): List<Element> {
            val alphaLength = rawData.size - DIALING_NUMBER_TRAILER_LENGTH
            return listOf(
                    PrimitiveElement.Builder(rawData.copyOfRange(0, alphaLength))
                            .labelId(R.string.alpha_identifier_label)
                            .parent(parent)
                            .interpreter(::alphaIdentifierInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(alphaLength, alphaLength + 1))
                            .labelId(R.string.bcd_number_length_label)
                            .parent(parent)
                            .interpreter(::unsignedIntegerInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(alphaLength + 1, alphaLength + 2))
                            .labelId(R.string.ton_npi_label)
                            .parent(parent)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(alphaLength + 2, alphaLength + 12))
                            .labelId(R.string.dialling_number_label)
                            .parent(parent)
                            .interpreter(::swappedBcdStringInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(
                            rawData.copyOfRange(alphaLength + 12, alphaLength + 13))
                            .labelId(ccpLabelId)
                            .parent(parent)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(alphaLength + 13, rawData.size))
                            .labelId(extensionLabelId)
                            .parent(parent)
                            .build(resources)
            )
        }

        private fun smsDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                        .labelId(R.string.sms_status_label)
                        .parent(parent)
                        .interpreter(::smsStatusInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(1, SMS_LENGTH))
                        .labelId(R.string.sms_tpdu_label)
                        .parent(parent)
                        .build(resources)
        )

        private fun smspDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val alphaLength = rawData.size - SMSP_TRAILER_LENGTH
            return listOf(
                    PrimitiveElement.Builder(rawData.copyOfRange(0, alphaLength))
                            .labelId(R.string.alpha_identifier_label)
                            .parent(parent)
                            .interpreter(::alphaIdentifierInterpreter)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(alphaLength, alphaLength + 1))
                            .labelId(R.string.sms_parameters_indicator_label)
                            .parent(parent)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(alphaLength + 1, alphaLength + 13))
                            .labelId(R.string.destination_address_label)
                            .parent(parent)
                            .build(resources),
                    PrimitiveElement.Builder(
                            rawData.copyOfRange(alphaLength + 13, alphaLength + 25))
                            .labelId(R.string.service_centre_address_label)
                            .parent(parent)
                            .build(resources),
                    PrimitiveElement.Builder(
                            rawData.copyOfRange(alphaLength + 25, alphaLength + 26))
                            .labelId(R.string.protocol_identifier_label)
                            .parent(parent)
                            .build(resources),
                    PrimitiveElement.Builder(
                            rawData.copyOfRange(alphaLength + 26, alphaLength + 27))
                            .labelId(R.string.data_coding_scheme_label)
                            .parent(parent)
                            .build(resources),
                    PrimitiveElement.Builder(rawData.copyOfRange(alphaLength + 27, rawData.size))
                            .labelId(R.string.validity_period_label)
                            .parent(parent)
                            .build(resources)
            )
        }

        private fun smssDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                        .labelId(R.string.message_reference_label)
                        .parent(parent)
                        .interpreter(::unsignedIntegerInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(1, SMSS_LENGTH))
                        .labelId(R.string.memory_capacity_exceeded_flag_label)
                        .parent(parent)
                        .interpreter(::memoryCapacityExceededInterpreter)
                        .build(resources)
        )

        private fun decodeExtensionRecordFile(
            resources: Resources,
            bytes: ByteArray,
            rootLabelId: Int
        ): Element? {
            if (bytes.size != EXTENSION_RECORD_LENGTH) return null

            return ConstructedElement.Builder(bytes)
                    .labelId(rootLabelId)
                    .decoder(::extensionRecordDecoder)
                    .build(resources)
        }

        private fun extensionRecordDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                        .labelId(R.string.record_type_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(1, 12))
                        .labelId(R.string.extension_data_label)
                        .parent(parent)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(12, EXTENSION_RECORD_LENGTH))
                        .labelId(R.string.identifier_label)
                        .parent(parent)
                        .build(resources)
        )

        private fun smsrDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData.copyOfRange(0, 1))
                        .labelId(R.string.sms_record_identifier_label)
                        .parent(parent)
                        .interpreter(::smsRecordIdentifierInterpreter)
                        .build(resources),
                PrimitiveElement.Builder(rawData.copyOfRange(1, rawData.size))
                        .labelId(R.string.sms_status_report_label)
                        .parent(parent)
                        .build(resources)
        )

        private fun ccp2Decoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> = listOf(
                PrimitiveElement.Builder(rawData)
                        .labelId(R.string.bearer_capability_information_element_label)
                        .parent(parent)
                        .build(resources)
        )

        private fun languageCodeInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size == LI_ENTRY_LENGTH && rawData.all { it.toInt() and 0xFF == 0xFF }) {
                return byteArrayToHexString(rawData) + " (" +
                        resources.getString(R.string.unused_label) + ")"
            }
            return PrimitiveElement.defaultStringInterpreter(resources, rawData)
        }

        private fun imsiInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val digits = mutableListOf<Char>()
            rawData.forEachIndexed { index, byte ->
                val value = byte.toInt() and 0xFF
                if (index == 0) {
                    digits.add(nibbleToDigit((value ushr 4) and 0x0F))
                } else {
                    digits.add(nibbleToDigit(value and 0x0F))
                    digits.add(nibbleToDigit((value ushr 4) and 0x0F))
                }
            }
            val imsi = digits.filter { it != 'F' }.joinToString("")
            return byteArrayToHexString(rawData) + " ($imsi)"
        }

        private fun keySetIdentifierInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            return byteArrayToHexString(rawData) + " (${value and 0x07})"
        }

        private fun plmnInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size != 3) return byteArrayToHexString(rawData)
            if (isUnusedPlmn(rawData)) {
                return byteArrayToHexString(rawData) + " (" +
                        resources.getString(R.string.unused_label) + ")"
            }

            val b1 = rawData[0].toInt() and 0xFF
            val b2 = rawData[1].toInt() and 0xFF
            val b3 = rawData[2].toInt() and 0xFF
            val mcc = "${b1 and 0x0F}${(b1 ushr 4) and 0x0F}${b2 and 0x0F}"
            val mncDigit3 = (b2 ushr 4) and 0x0F
            val mnc = "${b3 and 0x0F}${(b3 ushr 4) and 0x0F}" +
                    if (mncDigit3 == 0x0F) "" else mncDigit3.toString()
            return hexWithDescription(
                    resources,
                    rawData,
                    resources.getString(R.string.plmn_interpretation, mcc, mnc)
            )
        }

        private fun accessTechnologyInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size != 2) return byteArrayToHexString(rawData)

            val first = rawData[0].toInt() and 0xFF
            val second = rawData[1].toInt() and 0xFF
            val names = linkedSetOf<String>()
            if (first and 0x80 != 0) names.add(
                    resources.getString(R.string.access_tech_satellite_e_utran_nb_s1))
            if (first and 0x40 != 0) names.add(
                    resources.getString(R.string.access_tech_satellite_e_utran_wb_s1))
            if (first and 0x20 != 0) names.add(
                    resources.getString(R.string.access_tech_satellite_ng_ran))
            if (first and 0x10 != 0) names.add(resources.getString(R.string.access_tech_ng_ran))
            if (first and 0x08 != 0) names.add(
                    resources.getString(R.string.access_tech_e_utran_nb_s1))
            if (first and 0x04 != 0) names.add(
                    resources.getString(R.string.access_tech_e_utran_wb_s1))
            if (first and 0x02 != 0) names.add(resources.getString(R.string.access_tech_e_utran))
            if (first and 0x01 != 0) names.add(resources.getString(R.string.access_tech_utran))
            if (second and 0x20 != 0) names.add(resources.getString(R.string.access_tech_gsm))
            if (second and 0x10 != 0) names.add(
                    resources.getString(R.string.access_tech_ec_gsm_iot))
            if (second and 0x08 != 0) names.add(
                    resources.getString(R.string.access_tech_cdma2000_1xrtt))
            if (second and 0x04 != 0) names.add(
                    resources.getString(R.string.access_tech_cdma2000_hrpd))
            if (second and 0x02 != 0) names.add(
                    resources.getString(R.string.access_tech_gsm_compact))
            if (second and 0x01 != 0) names.add(resources.getString(R.string.access_tech_gsm))

            val suffix = if (names.isEmpty()) {
                resources.getString(R.string.no_access_technology_selected)
            } else {
                names.joinToString(", ")
            }
            return hexWithDescription(resources, rawData, suffix)
        }

        private fun hpplmnInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val hex = byteArrayToHexString(rawData)
            if (value == 0) {
                return "$hex (${resources.getString(R.string.no_hpplmn_search_attempts)})"
            }

            val iotText = when (value) {
                1 -> resources.getString(R.string.hpplmn_iot_hours, 2)
                in 2..40 -> resources.getString(R.string.hpplmn_iot_hours, value * 2)
                in 41..80 -> resources.getString(R.string.hpplmn_iot_hours, value * 4 - 80)
                else -> resources.getString(R.string.hpplmn_iot_default_period)
            }
            return resources.getString(
                    R.string.hex_with_description,
                    hex,
                    resources.getString(R.string.hpplmn_interval_interpretation, value, iotText)
            )
        }

        private fun unsignedIntegerInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) }
            return byteArrayToHexString(rawData) + " ($value)"
        }

        private fun usimServiceName(
            resources: Resources,
            serviceNumber: Int
        ): String = resources.getStringArray(R.array.usim_service_names).getOrElse(
                serviceNumber - 1) {
            resources.getString(R.string.rfu_label)
        }

        private fun usimServiceStateInterpreter(
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

        private fun alphaIdentifierInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val textData = rawData.dropLastWhile { it.toInt() and 0xFF == 0xFF }.toByteArray()
            return hexWithDescription(resources, rawData, StringUtils.decode(textData))
        }

        private fun spnDisplayConditionInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val registeredPlmn = if (value and 0x01 == 0) {
                resources.getString(R.string.spn_registered_plmn_name_not_required)
            } else {
                resources.getString(R.string.spn_registered_plmn_name_required)
            }
            val spn = if (value and 0x02 == 0) {
                resources.getString(R.string.spn_required_outside_hplmn_or_spdi_plmn)
            } else {
                resources.getString(R.string.spn_not_required_outside_hplmn_or_spdi_plmn)
            }
            return hexWithDescription(
                    resources,
                    rawData,
                    resources.getString(R.string.semicolon_joined_descriptions, registeredPlmn, spn)
            )
        }

        private fun pricePerUnitInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size != 2) return byteArrayToHexString(rawData)

            val first = rawData[0].toInt() and 0xFF
            val second = rawData[1].toInt() and 0xFF
            val eppu = (first shl 4) or (second and 0x0F)
            val exponentMagnitude = (second ushr 5) and 0x07
            val exponent = if (second and 0x10 == 0) exponentMagnitude else -exponentMagnitude
            val price = eppu * 10.0.pow(exponent)
            return hexWithDescription(
                    resources,
                    rawData,
                    resources.getString(
                            R.string.price_per_unit_interpretation,
                            eppu,
                            exponent,
                            price.toString()
                    )
            )
        }

        private fun cbMessageIdentifierInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.all { it.toInt() and 0xFF == 0xFF }) {
                return byteArrayToHexString(rawData) + " (" +
                        resources.getString(R.string.unused_label) + ")"
            }
            return unsignedIntegerInterpreter(resources, rawData)
        }

        private fun accessControlClassesInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size != ACC_LENGTH) return byteArrayToHexString(rawData)

            val allocated = mutableListOf<Int>()
            val first = rawData[0].toInt() and 0xFF
            val second = rawData[1].toInt() and 0xFF
            (0 until 8).forEach { bit ->
                if (first and (1 shl bit) != 0 && bit != 2) allocated.add(bit + 8)
                if (second and (1 shl bit) != 0) allocated.add(bit)
            }
            val suffix = if (allocated.isEmpty()) {
                resources.getString(R.string.no_access_control_class_allocated)
            } else {
                allocated.sorted().joinToString(", ") { it.toString().padStart(2, '0') }
            }
            return byteArrayToHexString(rawData) + " ($suffix)"
        }

        private fun plmnEntryInterpreter(
            resources: Resources,
            index: Int,
            rawData: ByteArray
        ): String = resources.getString(
                R.string.plmn_entry_interpretation,
                index,
                plmnInterpreter(resources, rawData)
        )

        private fun locationAreaInformationInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size != 5) return byteArrayToHexString(rawData)

            val plmn = plmnInterpreter(resources, rawData.copyOfRange(0, 3))
            val lac = byteArrayToHexString(rawData.copyOfRange(3, 5))
            return hexWithDescription(
                    resources,
                    rawData,
                    resources.getString(R.string.location_area_interpretation, plmn, lac)
            )
        }

        private fun routingAreaInformationInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size != 6) return byteArrayToHexString(rawData)

            val lai = locationAreaInformationInterpreter(resources, rawData.copyOfRange(0, 5))
            val rac = byteArrayToHexString(rawData.copyOfRange(5, 6))
            return hexWithDescription(resources, rawData, "$lai, RAC $rac")
        }

        private fun locationUpdateStatusInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val status = when (rawData.firstOrNull()?.toInt()?.and(0x07)) {
                0x00 -> resources.getString(R.string.location_update_status_updated)
                0x01 -> resources.getString(R.string.location_update_status_not_updated)
                0x02 -> resources.getString(R.string.location_update_status_plmn_not_allowed)
                0x03 -> resources.getString(R.string.location_update_status_lai_not_allowed)
                0x07 -> resources.getString(R.string.reserved_label)
                else -> resources.getString(R.string.rfu_label)
            }
            return hexWithDescription(resources, rawData, status)
        }

        private fun routingAreaUpdateStatusInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val status = when (rawData.firstOrNull()?.toInt()?.and(0x07)) {
                0x00 -> resources.getString(R.string.location_update_status_updated)
                0x01 -> resources.getString(R.string.location_update_status_not_updated)
                0x02 -> resources.getString(R.string.location_update_status_plmn_not_allowed)
                0x03 -> resources.getString(R.string.routing_area_update_status_rai_not_allowed)
                0x07 -> resources.getString(R.string.reserved_label)
                else -> resources.getString(R.string.rfu_label)
            }
            return hexWithDescription(resources, rawData, status)
        }

        private fun swappedBcdStringInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val text = rawData.flatMap { byte ->
                val value = byte.toInt() and 0xFF
                listOf(value and 0x0F, (value ushr 4) and 0x0F)
            }.map(::nibbleToDiallingChar)
                    .filter { it != 'F' }
                    .joinToString("")
            return hexWithDescription(resources, rawData, text)
        }

        private fun emergencyCallCodeInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String = swappedBcdStringInterpreter(resources, rawData)

        private fun smsStatusInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val status = when {
                value == 0x00 || value == 0xFF -> resources.getString(
                        R.string.sms_status_free_space)
                value and 0x01 == 0 -> resources.getString(R.string.sms_record_empty)
                value and 0x07 == 0x01 -> resources.getString(R.string.sms_status_received_read)
                value and 0x07 == 0x03 -> resources.getString(R.string.sms_status_received_unread)
                value and 0x07 == 0x05 -> resources.getString(R.string.sms_status_mo_to_be_sent)
                value and 0x07 == 0x07 -> resources.getString(R.string.sms_status_used_space)
                value == 0x09 -> resources.getString(R.string.sms_status_mo_sent_no_status_report)
                value == 0x0D -> resources.getString(
                        R.string.sms_status_mo_sent_status_report_not_received)
                value == 0x15 -> resources.getString(
                        R.string.sms_status_mo_sent_status_report_received_not_stored)
                else -> resources.getString(R.string.sms_status_used_space)
            }
            return hexWithDescription(resources, rawData, status)
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
            return hexWithDescription(resources, rawData, label)
        }

        private fun smsRecordIdentifierInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0xFF) ?: return ""
            val label = if (value == 0x00 || value == 0xFF) {
                resources.getString(R.string.sms_record_empty)
            } else {
                resources.getString(R.string.sms_record_number, value)
            }
            return hexWithDescription(resources, rawData, label)
        }

        private fun ueOperationModeInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val mode = when (rawData.firstOrNull()?.toInt()?.and(0xFF)) {
                0x00 -> resources.getString(R.string.ue_operation_mode_normal)
                0x80 -> resources.getString(R.string.ue_operation_mode_type_approval)
                0x01 -> resources.getString(R.string.ue_operation_mode_normal_specific)
                0x81 -> resources.getString(R.string.ue_operation_mode_type_approval_specific)
                0x02 -> resources.getString(R.string.ue_operation_mode_maintenance_off_line)
                0x04 -> resources.getString(R.string.ue_operation_mode_cell_test)
                else -> resources.getString(R.string.rfu_label)
            }
            return hexWithDescription(resources, rawData, mode)
        }

        private fun additionalInformationInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            if (rawData.size != 2) return byteArrayToHexString(rawData)

            val second = rawData[1].toInt() and 0xFF
            val flags = listOf(
                    resources.getString(R.string.additional_info_ciphering_indicator) to
                            (second and 0x01 != 0),
                    resources.getString(R.string.additional_info_csg_display_control) to
                            (second and 0x02 != 0),
                    resources.getString(R.string.additional_info_prose_public_safety) to
                            (second and 0x04 != 0),
                    resources.getString(R.string.additional_info_extended_drx_cycle) to
                            (second and 0x08 != 0),
                    resources.getString(R.string.additional_info_5g_prose) to
                            (second and 0x10 != 0)
            ).filter { it.second }.map { it.first }
            val suffix = if (flags.isEmpty()) {
                resources.getString(R.string.no_additional_information_enabled)
            } else {
                flags.joinToString(", ")
            }
            return hexWithDescription(resources, rawData, suffix)
        }

        private fun mncLengthInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            val value = rawData.firstOrNull()?.toInt()?.and(0x0F) ?: return ""
            val suffix = when (value) {
                0 -> resources.getString(R.string.mnc_length_zero_digits_service_130_available)
                2 -> resources.getString(R.string.mnc_length_digits, 2)
                3 -> resources.getString(R.string.mnc_length_digits, 3)
                else -> resources.getString(R.string.rfu_label)
            }
            return hexWithDescription(resources, rawData, suffix)
        }

        private fun hexWithDescription(
            resources: Resources,
            rawData: ByteArray,
            description: String
        ): String {
            val hex = byteArrayToHexString(rawData)
            if (description.isEmpty()) return hex

            return resources.getString(R.string.hex_with_description, hex, description)
        }

        private fun isUnusedPlmn(rawData: ByteArray): Boolean {
            return rawData.size == 3 && rawData.all { it.toInt() and 0xFF == 0xFF }
        }

        private fun nibbleToDigit(nibble: Int): Char {
            return if (nibble in 0..9) {
                ('0'.code + nibble).toChar()
            } else {
                nibble.toString(16).uppercase(Locale.US)[0]
            }
        }

        private fun nibbleToDiallingChar(nibble: Int): Char {
            return when (nibble) {
                in 0..9 -> ('0'.code + nibble).toChar()
                0x0A -> '*'
                0x0B -> '#'
                0x0C -> 'a'
                0x0D -> 'b'
                0x0E -> 'c'
                else -> 'F'
            }
        }

    }
}
