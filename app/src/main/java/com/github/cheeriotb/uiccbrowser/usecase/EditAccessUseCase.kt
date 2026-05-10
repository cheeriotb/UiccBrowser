/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.usecase

import android.content.Context
import com.github.cheeriotb.uiccbrowser.element.BerTlvElement
import com.github.cheeriotb.uiccbrowser.element.Element
import com.github.cheeriotb.uiccbrowser.element.ef.EfArrRecord
import com.github.cheeriotb.uiccbrowser.element.fcp.FcpTemplate
import com.github.cheeriotb.uiccbrowser.element.fcp.SecurityAttrExpanded
import com.github.cheeriotb.uiccbrowser.repository.CardRepository
import com.github.cheeriotb.uiccbrowser.repository.FileId
import com.github.cheeriotb.uiccbrowser.repository.ReadRecordParams
import com.github.cheeriotb.uiccbrowser.repository.Result
import com.github.cheeriotb.uiccbrowser.repository.VerifyPinQualifier
import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString

class EditAccessUseCase(private val context: Context) {

    data class Outcome(
        val qualifierOptions: List<List<VerifyPinQualifier>> = emptyList(),
        val exploreQualifierOptions: List<VerifyPinQualifier> = emptyList(),
        val failure: Failure? = null
    ) {
        val qualifiers: List<VerifyPinQualifier>
            get() = qualifierOptions.firstOrNull().orEmpty()
    }

    enum class Failure {
        CARD_UNAVAILABLE,
        FCP_UNAVAILABLE,
        SECURITY_ATTRIBUTES_UNAVAILABLE,
        SECURITY_CONDITION_UNSUPPORTED,
        ARR_READ_FAILED,
        ARR_ACCESS_KEYS_UNAVAILABLE,
        ARR_RECORD_UNAVAILABLE
    }

    suspend fun execute(
        slotId: Int,
        fileId: FileId,
        requireReadAccess: Boolean = true
    ): Outcome {
        val repo = CardRepository.from(context, slotId) ?: return Outcome(
                failure = Failure.CARD_UNAVAILABLE)
        if (!repo.isAccessible) return Outcome(failure = Failure.CARD_UNAVAILABLE)

        val fcpResults = repo.queryFileControlParameters(fileId)
        if (fcpResults.any { !it.isOk }) return Outcome(failure = Failure.FCP_UNAVAILABLE)
        val fcpData = fcpResults.firstOrNull { it.isOk }?.data
            ?: return Outcome(failure = Failure.FCP_UNAVAILABLE)
        val fcpElement = FcpTemplate.decode(context.resources, fcpData)
            ?: return Outcome(failure = Failure.FCP_UNAVAILABLE)

        val requiredMode = if (requireReadAccess) READ_BIT or UPDATE_BIT else UPDATE_BIT
        val directOutcome = directSecurityAttributeOutcome(fcpElement, requiredMode)
        if (directOutcome != null) return directOutcome

        val arrRef = arrReference(fcpElement)
            ?: return Outcome(failure = Failure.SECURITY_ATTRIBUTES_UNAVAILABLE)
        return arrSecurityAttributeOutcome(repo, fileId, arrRef, requiredMode)
    }

    private fun directSecurityAttributeOutcome(
        fcpElement: Element,
        requiredMode: Int
    ): Outcome? {
        val children = fcpElement.subElements.filterIsInstance<BerTlvElement>()
        val expanded = children.find { it.tag == FcpTemplate.TAG_SECURITY_ATTR_EXPAND }
        if (expanded != null) {
            return outcomeFromExpandedChildren(expanded.subElements, requiredMode)
        }

        val compact = children.find { it.tag == FcpTemplate.TAG_SECURITY_ATTR_COMPACT }
        if (compact != null) {
            return outcomeFromAccessRules(parseCompact(compact.data), requiredMode)
        }

        return null
    }

    private suspend fun arrSecurityAttributeOutcome(
        repo: CardRepository,
        fileId: FileId,
        reference: ArrReference,
        requiredMode: Int
    ): Outcome {
        val recordNo = applicableArrRecordNumber(fileId, reference)
            ?: return Outcome(failure = Failure.SECURITY_CONDITION_UNSUPPORTED)
        if (recordNo <= 0) {
            return Outcome(failure = Failure.SECURITY_CONDITION_UNSUPPORTED)
        }
        val arrFileId = FileId(fileId.aid, fileId.path, reference.fileId)
        val result = repo.readRecord(ReadRecordParams(arrFileId, recordNo))
        if (result.sw == Result.SW_INSUFFICIENT_SECURITY) {
            return arrAccessKeyOutcome(fileId)
        }
        if (!result.isOk || result.data.isEmpty()) {
            return Outcome(failure = Failure.ARR_READ_FAILED)
        }
        val element = EfArrRecord.decode(context.resources, result.data)
            ?: return Outcome(failure = Failure.ARR_RECORD_UNAVAILABLE)
        return outcomeFromExpandedChildren(element.subElements, requiredMode)
    }

    /**
     * Returns an EF ARR record number when the direct or SEID-qualified reference is resolvable.
     */
    private fun applicableArrRecordNumber(fileId: FileId, reference: ArrReference): Int? {
        val directReference = reference.recordReferences.singleOrNull()
            ?.takeIf { it.seId == null }
        if (directReference != null) return directReference.recordNumber

        val seId = currentSecurityEnvironmentId(fileId) ?: return null
        return reference.recordReferences
            .singleOrNull { it.seId == seId }
            ?.recordNumber
    }

    private fun arrAccessKeyOutcome(fileId: FileId): Outcome {
        val fcpData = CurrentDirectoryFcpUseCase(context).queryForEf(fileId)
            ?.takeIf { it.isOk }
            ?.data
            ?: return Outcome(failure = Failure.ARR_ACCESS_KEYS_UNAVAILABLE)
        val fcpElement = FcpTemplate.decode(context.resources, fcpData)
            ?: return Outcome(failure = Failure.ARR_ACCESS_KEYS_UNAVAILABLE)
        val qualifiers = arrAccessKeyQualifiers(fcpElement)
        if (qualifiers.isEmpty()) {
            return Outcome(failure = Failure.ARR_ACCESS_KEYS_UNAVAILABLE)
        }

        return Outcome(exploreQualifierOptions = qualifiers)
    }

    private fun outcomeFromExpandedChildren(
        children: List<Element>,
        requiredMode: Int
    ): Outcome = outcomeFromAccessRules(parseExpanded(children), requiredMode)

    private fun outcomeFromAccessRules(rules: AccessRules, requiredMode: Int): Outcome {
        val qualifierOptions = rules.qualifierOptionsFor(requiredMode)
        if (qualifierOptions.isEmpty()) {
            return Outcome(failure = Failure.SECURITY_CONDITION_UNSUPPORTED)
        }

        return Outcome(qualifierOptions = qualifierOptions)
    }

    private fun parseExpanded(children: List<Element>): AccessRules {
        val result = AccessRules()
        val tlvs = children.filterIsInstance<BerTlvElement>()
        var index = 0
        while (index < tlvs.size - 1) {
            val am = tlvs[index]
            val sc = tlvs[index + 1]
            if (am.tag in ACCESS_MODE_TAGS && am.data.size == 1) {
                result.add(am.data[0].toInt() and 0xFF, conditionFor(sc))
                index += 2
            } else {
                index++
            }
        }
        return result
    }

    private fun parseCompact(data: ByteArray): AccessRules {
        val result = AccessRules()
        var index = 0
        while (index < data.size) {
            val accessMode = data[index++].toInt() and 0xFF
            val accessBits = accessMode and 0x7F
            val scCount = Integer.bitCount(accessBits)
            if (scCount == 0 || index + scCount > data.size) return AccessRules()

            val bitOrder = listOf(0x40, 0x20, 0x10, 0x08, 0x04, UPDATE_BIT, READ_BIT)
            bitOrder.filter { accessBits and it != 0 }.forEach { bit ->
                result.add(bit, compactConditionFor(data[index++].toInt() and 0xFF))
            }
        }
        return result
    }

    private fun conditionFor(element: BerTlvElement): SecurityCondition =
        when (element.tag) {
            SecurityAttrExpanded.TAG_SC_ALWAYS_DO -> SecurityCondition.Always
            SecurityAttrExpanded.TAG_SC_NEVER_DO -> SecurityCondition.Never
            SecurityAttrExpanded.TAG_CONTROL_DO -> controlDoCondition(element)
            SecurityAttrExpanded.TAG_AND_DO -> SecurityCondition.And(
                element.subElements.filterIsInstance<BerTlvElement>().map(::conditionFor)
            )
            SecurityAttrExpanded.TAG_OR_DO -> SecurityCondition.Or(
                element.subElements.filterIsInstance<BerTlvElement>().map(::conditionFor)
            )
            SecurityAttrExpanded.TAG_NOT_DO -> SecurityCondition.Unsupported
            else -> SecurityCondition.Unsupported
        }

    private fun controlDoCondition(element: BerTlvElement): SecurityCondition {
        val keyReferences = keyReferencesFrom(element.subElements)
            .toSet()
        return if (keyReferences.isEmpty()) {
            SecurityCondition.Unsupported
        } else {
            SecurityCondition.Verify(keyReferences)
        }
    }

    private fun compactConditionFor(value: Int): SecurityCondition =
        when (value) {
            COMPACT_ALWAYS -> SecurityCondition.Always
            COMPACT_NEVER -> SecurityCondition.Never
            else -> keyReferenceFor(value)?.let { SecurityCondition.Verify(setOf(it)) }
                ?: SecurityCondition.Unsupported
        }

    private fun keyReferencesFrom(children: List<Element>): List<KeyReference> {
        val keyReferences = mutableListOf<KeyReference>()
        var usageQualifier: ByteArray? = null
        children.filterIsInstance<BerTlvElement>().forEach { child ->
            when (child.tag) {
                SecurityAttrExpanded.TAG_USAGE_QUALIFIER -> {
                    usageQualifier = child.data
                }
                SecurityAttrExpanded.TAG_KEY_REFERENCE -> {
                    val keyReference = keyReferenceFor(
                        child.data.firstOrNull()?.toInt()?.and(0xFF) ?: -1,
                        usageQualifier
                    )
                    if (keyReference != null) keyReferences.add(keyReference)
                    usageQualifier = null
                }
                else -> {
                    usageQualifier = null
                }
            }
        }
        return keyReferences
    }

    private fun arrReference(fcpElement: Element): ArrReference? {
        val element = fcpElement.subElements
            .filterIsInstance<BerTlvElement>()
            .find { it.tag == FcpTemplate.TAG_SECURITY_ATTR_REF }
            ?: return null
        val data = element.data
        if (data.size < 3 || (data.size != 3 && (data.size - 2) % 2 != 0)) return null

        val arrFileId = byteArrayToHexString(data.sliceArray(0..1))
        val references = if (data.size == 3) {
            listOf(ArrRecordReference(null, data[2].toInt() and 0xFF))
        } else {
            (2 until data.size step 2).map {
                ArrRecordReference(
                    seId = data[it].toInt() and 0xFF,
                    recordNumber = data[it + 1].toInt() and 0xFF
                )
            }
        }
        if (references.isEmpty()) return null
        return ArrReference(arrFileId, references)
    }

    private fun arrAccessKeyQualifiers(fcpElement: Element): List<VerifyPinQualifier> {
        val pinStatusTemplate = fcpElement.subElements
            .filterIsInstance<BerTlvElement>()
            .find { it.tag == FcpTemplate.TAG_PIN_STATUS_TEMPLATE }
            ?: return emptyList()
        val children = pinStatusTemplate.subElements.filterIsInstance<BerTlvElement>()
        val psData = children.find { it.tag == FcpTemplate.TAG_PS_DO }?.data
        val entries = pinStatusTemplateEntries(children)

        return entries
            .filterIndexed { index, _ -> psData == null || psData.isPinEnabled(index) }
            .mapNotNull { it.keyReference?.qualifier }
            .distinct()
    }

    /**
     * Resolves ETSI-defined SE00/SE01 from the current directory PIN status template.
     */
    private fun currentSecurityEnvironmentId(fileId: FileId): Int? {
        val fcpData = CurrentDirectoryFcpUseCase(context).queryForEf(fileId)
            ?.takeIf { it.isOk }
            ?.data
            ?: return null
        val fcpElement = FcpTemplate.decode(context.resources, fcpData) ?: return null
        val pinStatusTemplate = fcpElement.subElements
            .filterIsInstance<BerTlvElement>()
            .find { it.tag == FcpTemplate.TAG_PIN_STATUS_TEMPLATE }
            ?: return null
        val children = pinStatusTemplate.subElements.filterIsInstance<BerTlvElement>()
        val psData = children.find { it.tag == FcpTemplate.TAG_PS_DO }?.data
            ?: return null
        val enabledEntries = pinStatusTemplateEntries(children)
            .filterIndexed { index, _ -> psData.isPinEnabled(index) }

        if (enabledEntries.any { it.keyReference?.qualifier?.isApplicationPin() == true }) {
            return SE_ID_01
        }
        val universalPin = enabledEntries
            .mapNotNull { it.keyReference }
            .firstOrNull {
                it.qualifier == VerifyPinQualifier.UNIVERSAL_PIN
            } ?: return SE_ID_00

        return when (universalPin.usageQualifier) {
            USE_UNIVERSAL_PIN -> SE_ID_00
            DO_NOT_USE_UNIVERSAL_PIN -> SE_ID_01
            else -> null
        }
    }

    private fun pinStatusTemplateEntries(children: List<BerTlvElement>): List<PinStatusEntry> {
        val entries = mutableListOf<PinStatusEntry>()
        var usageQualifier: ByteArray? = null
        children.forEach { child ->
            when (child.tag) {
                FcpTemplate.TAG_USAGE_QUALIFIER_DO -> {
                    usageQualifier = child.data
                }
                FcpTemplate.TAG_KEY_REFERENCE -> {
                    val keyReference = keyReferenceFor(
                        child.data.firstOrNull()?.toInt()?.and(0xFF) ?: -1,
                        usageQualifier
                    )
                    entries.add(PinStatusEntry(usageQualifier, keyReference))
                    usageQualifier = null
                }
                else -> {
                    usageQualifier = null
                }
            }
        }
        return entries
    }

    private fun ByteArray.isPinEnabled(index: Int): Boolean {
        val byteIndex = index / 8
        if (byteIndex !in indices) return false
        return (this[byteIndex].toInt() and (0x80 ushr (index % 8))) != 0
    }

    private fun keyReferenceFor(
        value: Int,
        usageQualifier: ByteArray? = null
    ): KeyReference? =
        VerifyPinQualifier.entries
            .find { it.value == value }
            ?.let { KeyReference(it, usageQualifier?.let(::byteArrayToHexString)) }

    private fun VerifyPinQualifier.isApplicationPin(): Boolean =
        value in VerifyPinQualifier.GLOBAL_PIN1.value..VerifyPinQualifier.GLOBAL_PIN8.value

    private data class KeyReference(
        val qualifier: VerifyPinQualifier,
        val usageQualifier: String?
    )

    private data class PinStatusEntry(
        val usageQualifier: ByteArray?,
        val keyReference: KeyReference?
    )

    private data class ArrReference(
        val fileId: String,
        val recordReferences: List<ArrRecordReference>
    )

    private data class ArrRecordReference(
        val seId: Int?,
        val recordNumber: Int
    )

    private sealed class SecurityCondition {
        data object Always : SecurityCondition()
        data object Never : SecurityCondition()
        data object Unsupported : SecurityCondition()
        data class Verify(val keyReferences: Set<KeyReference>) : SecurityCondition()
        data class And(val conditions: List<SecurityCondition>) : SecurityCondition()
        data class Or(val conditions: List<SecurityCondition>) : SecurityCondition()

        /**
         * Converts the parsed condition tree into selectable VERIFY options.
         */
        fun qualifierOptions(): List<Set<KeyReference>> = when (this) {
            Always -> listOf(emptySet())
            Never, Unsupported -> emptyList()
            is Verify -> listOf(keyReferences)
            is And -> combineAnd(conditions)
            is Or -> conditions.flatMap { it.qualifierOptions() }
        }

        private fun combineAnd(
            conditions: List<SecurityCondition>
        ): List<Set<KeyReference>> {
            var result = listOf(emptySet<KeyReference>())
            for (condition in conditions) {
                val childOptions = condition.qualifierOptions()
                if (childOptions.isEmpty()) return emptyList()
                result = result.flatMap { base -> childOptions.map { base + it } }
            }
            return result
        }
    }

    private class AccessRules {
        private val rules = mutableListOf<ModeRule>()

        /**
         * Adds one access mode rule. Non READ/UPDATE bits are kept out of edit checks.
         */
        fun add(accessMode: Int, condition: SecurityCondition) {
            val relevantMode = accessMode and (READ_BIT or UPDATE_BIT)
            val options = condition.qualifierOptions().map { option ->
                option.map { it.qualifier }.toSet()
            }
            if (relevantMode != 0 && options.isNotEmpty()) {
                rules.add(ModeRule(relevantMode, options))
            }
        }

        fun qualifierOptionsFor(requiredMode: Int): List<List<VerifyPinQualifier>> {
            val choices = rules.flatMap { rule ->
                rule.options.map { option -> ModeChoice(rule.accessMode, option) }
            }
            val results = mutableSetOf<Set<VerifyPinQualifier>>()

            fun collect(index: Int, coveredMode: Int, qualifiers: Set<VerifyPinQualifier>) {
                if (coveredMode and requiredMode == requiredMode) {
                    results.add(qualifiers)
                    return
                }
                if (index >= choices.size) return

                collect(index + 1, coveredMode, qualifiers)
                val choice = choices[index]
                collect(
                    index + 1,
                    coveredMode or choice.accessMode,
                    qualifiers + choice.qualifiers
                )
            }

            collect(0, 0, emptySet())
            return results
                .filter { option -> results.none { it != option && option.containsAll(it) } }
                .sortedWith(compareBy<Set<VerifyPinQualifier>> { it.size }
                    .thenBy { option -> option.joinToString(",") { it.value.toString() } })
                .map { it.toList() }
        }
    }

    private data class ModeRule(
        val accessMode: Int,
        val options: List<Set<VerifyPinQualifier>>
    )

    private data class ModeChoice(
        val accessMode: Int,
        val qualifiers: Set<VerifyPinQualifier>
    )

    companion object {
        private const val READ_BIT = 0x01
        private const val UPDATE_BIT = 0x02
        private const val COMPACT_ALWAYS = 0x00
        private const val COMPACT_NEVER = 0xFF
        private const val SE_ID_00 = 0x00
        private const val SE_ID_01 = 0x01
        private const val USE_UNIVERSAL_PIN = "08"
        private const val DO_NOT_USE_UNIVERSAL_PIN = "00"
        private val ACCESS_MODE_TAGS = 0x80..0x8F
    }
}
