/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui.efdetail

import com.github.cheeriotb.uiccbrowser.R
import com.github.cheeriotb.uiccbrowser.repository.Result
import com.github.cheeriotb.uiccbrowser.repository.VerifyPinQualifier
import com.github.cheeriotb.uiccbrowser.usecase.EditAccessUseCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EfDetailFragmentUnitTest {

    @Test
    fun buildRecordSelectorState_zeroRecords_notVisible() {
        val state = EfDetailFragment.buildRecordSelectorState(0, 0, true)

        assertThat(state.visible).isFalse()
    }

    @Test
    fun buildRecordSelectorState_nonZeroRecords_visible() {
        val state = EfDetailFragment.buildRecordSelectorState(3, 0, true)

        assertThat(state.visible).isTrue()
    }

    @Test
    fun buildRecordSelectorState_transparentEf_notVisibleOnAllTabs() {
        for (tab in 0..2) {
            assertThat(EfDetailFragment.buildRecordSelectorState(0, tab, true).visible).isFalse()
        }
    }

    @Test
    fun buildRecordSelectorState_binaryTab_enabled() {
        val state = EfDetailFragment.buildRecordSelectorState(3, 0, true)

        assertThat(state.enabled).isTrue()
    }

    @Test
    fun buildRecordSelectorState_infoTab_enabled() {
        val state = EfDetailFragment.buildRecordSelectorState(3, 1, true)

        assertThat(state.enabled).isTrue()
    }

    @Test
    fun buildRecordSelectorState_fcpTab_notEnabled() {
        val state = EfDetailFragment.buildRecordSelectorState(3, 2, true)

        assertThat(state.enabled).isFalse()
    }

    @Test
    fun buildRecordSelectorState_transparentEfOnFcpTab_notVisibleAndNotEnabled() {
        val state = EfDetailFragment.buildRecordSelectorState(0, 2, true)

        assertThat(state.visible).isFalse()
        assertThat(state.enabled).isFalse()
    }

    @Test
    fun buildRecordSelectorState_binaryTabWhenNoDecoder_enabled() {
        val state = EfDetailFragment.buildRecordSelectorState(3, 0, false)

        assertThat(state.enabled).isTrue()
    }

    @Test
    fun buildRecordSelectorState_fcpTabWhenNoDecoder_notEnabled() {
        val state = EfDetailFragment.buildRecordSelectorState(3, 1, false)

        assertThat(state.enabled).isFalse()
    }

    @Test
    fun errorMessageResId_insufficientSecurity_returnsSw6982Message() {
        val result = Result.Builder().sw(Result.SW_INSUFFICIENT_SECURITY).build()

        assertThat(EfDetailFragment.errorMessageResId(result))
            .isEqualTo(R.string.sw6982_insufficient_security)
    }

    @Test
    fun errorMessageResId_notFound_returnsSw6a82Message() {
        val result = Result.Builder().sw(Result.SW_NOT_FOUND).build()

        assertThat(EfDetailFragment.errorMessageResId(result))
            .isEqualTo(R.string.sw6a82_file_not_found)
    }

    @Test
    fun errorMessageResId_unknownSw_returnsUnknownErrorMessage() {
        val result = Result.Builder().sw(0x6F00).build()

        assertThat(EfDetailFragment.errorMessageResId(result))
            .isEqualTo(R.string.sw_unhandled_error)
    }

    @Test
    fun buildErrorMessage_formatsStatusWordAndMessage() {
        val result = Result.Builder().sw(Result.SW_INSUFFICIENT_SECURITY).build()

        assertThat(EfDetailFragment.buildErrorMessage(result, "Security status not satisfied"))
            .isEqualTo("SW 6982: Security status not satisfied")
    }

    @Test
    fun requiresReadAccessForEdit_insufficientSecurity_returnsTrue() {
        val result = Result.Builder().sw(Result.SW_INSUFFICIENT_SECURITY).build()

        assertThat(EfDetailFragment.requiresReadAccessForEdit(result)).isTrue()
    }

    @Test
    fun requiresReadAccessForEdit_notInsufficientSecurity_returnsFalse() {
        val result = Result.Builder().sw(Result.SW_NOT_FOUND).build()

        assertThat(EfDetailFragment.requiresReadAccessForEdit(result)).isFalse()
    }

    @Test
    fun requiresReadAccessForEdit_null_returnsFalse() {
        assertThat(EfDetailFragment.requiresReadAccessForEdit(null)).isFalse()
    }

    @Test
    fun messageResId_editAccessFailures_returnsMessageResources() {
        assertThat(EfDetailFragment.messageResId(EditAccessUseCase.Failure.CARD_UNAVAILABLE))
            .isEqualTo(R.string.edit_mode_card_unavailable)
        assertThat(EfDetailFragment.messageResId(EditAccessUseCase.Failure.FCP_UNAVAILABLE))
            .isEqualTo(R.string.edit_mode_fcp_unavailable)
        assertThat(EfDetailFragment.messageResId(EditAccessUseCase.Failure.ARR_READ_FAILED))
            .isEqualTo(R.string.edit_mode_arr_read_failed)
        assertThat(EfDetailFragment.messageResId(
            EditAccessUseCase.Failure.ARR_ACCESS_KEYS_UNAVAILABLE
        ))
            .isEqualTo(R.string.edit_mode_arr_access_keys_unavailable)
    }

    @Test
    fun arrAccessKeyUnavailableMessageResId_allBlocked_returnsNoVerifiableKey() {
        val statuses = listOf(
            EfDetailFragment.VerifyStatus.Unavailable(
                VerifyPinQualifier.ADM1,
                EfDetailFragment.VerifyUnavailableReason.BLOCKED
            ),
            EfDetailFragment.VerifyStatus.Unavailable(
                VerifyPinQualifier.ADM2,
                EfDetailFragment.VerifyUnavailableReason.BLOCKED
            )
        )

        assertThat(EfDetailFragment.arrAccessKeyUnavailableMessageResId(statuses))
            .isEqualTo(R.string.edit_mode_arr_no_verifiable_key)
    }

    @Test
    fun arrAccessKeyUnavailableMessageResId_blockedAndLastAttempt_returnsNoSafeKey() {
        val statuses = listOf(
            EfDetailFragment.VerifyStatus.Unavailable(
                VerifyPinQualifier.ADM1,
                EfDetailFragment.VerifyUnavailableReason.BLOCKED
            ),
            EfDetailFragment.VerifyStatus.Unavailable(
                VerifyPinQualifier.ADM2,
                EfDetailFragment.VerifyUnavailableReason.LAST_ATTEMPT
            )
        )

        assertThat(EfDetailFragment.arrAccessKeyUnavailableMessageResId(statuses))
            .isEqualTo(R.string.edit_mode_arr_no_safe_key)
    }

    @Test
    fun displayName_formatsVerifyPinQualifierNames() {
        assertThat(EfDetailFragment.verifyPinQualifierDisplayName(
            VerifyPinQualifier.GLOBAL_PIN1
        ))
            .isEqualTo("Global PIN1")
        assertThat(EfDetailFragment.verifyPinQualifierDisplayName(
            VerifyPinQualifier.LOCAL_PIN8
        ))
            .isEqualTo("Local PIN8")
        assertThat(EfDetailFragment.verifyPinQualifierDisplayName(VerifyPinQualifier.ADM10))
            .isEqualTo("ADM10")
    }
}
