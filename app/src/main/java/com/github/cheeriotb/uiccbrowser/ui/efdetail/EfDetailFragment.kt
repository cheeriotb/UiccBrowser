/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui.efdetail

import android.os.Bundle
import android.graphics.Typeface
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.github.cheeriotb.uiccbrowser.R
import com.github.cheeriotb.uiccbrowser.databinding.FragmentEfDetailBinding
import com.github.cheeriotb.uiccbrowser.repository.CardRepository
import com.github.cheeriotb.uiccbrowser.repository.FileId
import com.github.cheeriotb.uiccbrowser.repository.Result
import com.github.cheeriotb.uiccbrowser.repository.KeyReference
import com.github.cheeriotb.uiccbrowser.ui.MainViewModel
import com.github.cheeriotb.uiccbrowser.usecase.EditAccessUseCase
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.launch
import kotlin.coroutines.resume

class EfDetailFragment : Fragment() {

    private var _binding: FragmentEfDetailBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var viewModel: EfDetailViewModel
    private lateinit var binaryViewModel: BinaryViewModel
    private var editModeEnabled = false

    /** True while a READ failure is being recovered by VERIFY and refresh. */
    private var readAccessRecoveryInProgress = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEfDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = requireArguments()
        val efName = args.getString(ARG_EF_NAME) ?: ""
        val efFileId = args.getString(ARG_EF_FILE_ID) ?: ""
        val aid = args.getString(ARG_AID) ?: FileId.AID_NONE
        val parentPath = args.getString(ARG_PARENT_PATH) ?: ""

        viewModel = ViewModelProvider(
            this,
            EfDetailViewModel.Factory(efName, efFileId, aid, parentPath)
        )[EfDetailViewModel::class.java]

        val slotId = mainViewModel.selectedSlot.value?.slotId ?: 0
        binaryViewModel = ViewModelProvider(
            this,
            BinaryViewModel.Factory(requireActivity().application, viewModel.fileId, slotId)
        )[BinaryViewModel::class.java]

        binding.viewPager.adapter = EfDetailPagerAdapter(this, viewModel.hasDecoder)

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when {
                position == 0 -> {
                    tab.text = "Binary"
                    tab.icon = AppCompatResources.getDrawable(
                        requireContext(),
                        R.drawable.ic_binary
                    )
                }
                viewModel.hasDecoder && position == 1 -> {
                    tab.text = "Info"
                    tab.icon = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_info)
                }
                else -> {
                    tab.text = "FCP"
                    tab.icon = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_fcp)
                }
            }
        }.attach()

        setupRecordSelector()
        setupOptionsMenu()
        observeBinaryViewModel()
        observeKeyboardEvents()
    }

    private fun setupOptionsMenu() {
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.ef_detail, menu)
                    val editItem = menu.findItem(R.id.action_edit)
                    editItem.isVisible = mainViewModel.isProModeEnabled.value
                    editItem.isEnabled = !editModeEnabled
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.action_refresh -> {
                            if (editModeEnabled) {
                                stopEditMode(refresh = true)
                            } else {
                                binaryViewModel.refresh()
                            }
                            true
                        }
                        R.id.action_edit -> {
                            startEditMode()
                            true
                        }
                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun startEditMode() {
        val slotId = mainViewModel.selectedSlot.value?.slotId ?: return
        val repo = CardRepository.from(requireContext(), slotId) ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            if (enableEditMode(slotId, repo)) {
                editModeEnabled = true
                viewModel.enableEditMode()
                binaryViewModel.startEditMode()
                if (binaryViewModel.data.value == null) {
                    binaryViewModel.refresh()
                }
                requireActivity().invalidateOptionsMenu()
                showMessage(getString(R.string.edit_mode_enabled))
            }
        }
    }

    private suspend fun enableEditMode(
        slotId: Int,
        repo: CardRepository
    ): Boolean = verifyAccessRequirements(
        slotId,
        repo,
        requireReadAccess = requiresReadAccessForEdit(binaryViewModel.readError.value)
    )

    private suspend fun enableReadAccess(
        slotId: Int,
        repo: CardRepository
    ): Boolean = verifyAccessRequirements(slotId, repo, EditAccessUseCase.RequiredAccess.READ)

    /**
     * Verifies all key references needed for the requested EF access mode.
     */
    private suspend fun verifyAccessRequirements(
        slotId: Int,
        repo: CardRepository,
        requireReadAccess: Boolean
    ): Boolean {
        val requiredAccess = if (requireReadAccess) {
            EditAccessUseCase.RequiredAccess.READ_UPDATE
        } else {
            EditAccessUseCase.RequiredAccess.UPDATE
        }
        return verifyAccessRequirements(slotId, repo, requiredAccess)
    }

    private suspend fun verifyAccessRequirements(
        slotId: Int,
        repo: CardRepository,
        requiredAccess: EditAccessUseCase.RequiredAccess
    ): Boolean {
        while (true) {
            val outcome = EditAccessUseCase(requireContext()).execute(
                slotId,
                viewModel.fileId,
                requiredAccess
            )
            val failure = outcome.failure
            if (failure != null) {
                showMessage(getString(messageResId(failure)))
                return false
            }

            if (outcome.exploreKeyReferenceOptions.isNotEmpty()) {
                if (!verifyArrAccessKeyOptions(repo, outcome.exploreKeyReferenceOptions)) {
                    return false
                }
                continue
            }

            val verified = verifyKeyReferenceOptions(
                repo,
                viewModel.fileId.aid,
                outcome.keyReferenceOptions
            )
            return verified
        }
    }

    private suspend fun verifyKeyReferenceOptions(
        repo: CardRepository,
        aid: String,
        keyReferenceOptions: List<List<KeyReference>>
    ): Boolean {
        if (keyReferenceOptions.isEmpty()) return true

        val candidates = mutableListOf<VerifyCandidate>()
        val unavailable = mutableListOf<VerifyStatus.Unavailable>()
        for (option in keyReferenceOptions) {
            if (option.isEmpty()) return true
            val requirements = mutableListOf<VerifyRequirement>()
            val trustedKeyReferences = mutableSetOf<KeyReference>()
            var optionUsable = true
            for (keyReference in option) {
                if (repo.isPinVerified(keyReference, viewModel.fileId)) {
                    trustedKeyReferences.add(keyReference)
                    continue
                }
                when (val status = verifyStatus(
                    repo.queryVerifyPinRetries(keyReference, aid).sw,
                    keyReference
                )) {
                    VerifyStatus.Verified -> Unit
                    is VerifyStatus.Available ->
                        requirements.add(VerifyRequirement(keyReference, status.retries))
                    is VerifyStatus.Unavailable -> {
                        unavailable.add(status)
                        requirements.clear()
                        optionUsable = false
                        break
                    }
                }
            }
            if (optionUsable && requirements.isEmpty()) {
                repo.markVerifiedPinsTrustedForNextAccess(
                    trustedKeyReferences,
                    viewModel.fileId
                )
                return true
            }
            if (optionUsable && requirements.isNotEmpty()) {
                candidates.add(VerifyCandidate(requirements, trustedKeyReferences))
            }
        }

        if (candidates.isEmpty()) {
            unavailable.firstOrNull()?.let { showVerifyUnavailableMessage(it) }
            return false
        }

        val selected = if (candidates.size == 1) {
            candidates.first()
        } else {
            showVerifyOptionDialog(candidates) ?: return false
        }
        for (requirement in selected.requirements) {
            if (!verifyKeyReference(repo, aid, requirement)) return false
        }
        repo.markVerifiedPinsTrustedForNextAccess(
            selected.trustedKeyReferences,
            viewModel.fileId
        )
        return true
    }

    private suspend fun verifyArrAccessKeyOptions(
        repo: CardRepository,
        keyReferences: List<KeyReference>
    ): Boolean {
        val trustedKeyReferences = mutableSetOf<KeyReference>()
        val statuses = keyReferences.map { keyReference ->
            if (repo.isPinVerified(keyReference, viewModel.fileId)) {
                trustedKeyReferences.add(keyReference)
                VerifyStatus.Verified
            } else {
                verifyStatus(repo.queryVerifyPinRetries(keyReference).sw, keyReference)
            }
        }
        if (trustedKeyReferences.isNotEmpty()) {
            repo.markVerifiedPinsTrustedForNextAccess(trustedKeyReferences, viewModel.fileId)
            return true
        }

        val candidates = statuses.filterIsInstance<VerifyStatus.Available>()
            .map { VerifyRequirement(it.keyReference, it.retries) }
        if (candidates.isEmpty()) {
            showArrAccessKeyUnavailableMessage(statuses)
            return false
        }

        val selected = showVerifySingleOptionDialog(
            R.string.edit_mode_arr_verify_option_title,
            candidates
        ) ?: return false
        return verifyKeyReference(repo, FileId.AID_NONE, selected)
    }

    private suspend fun verifyKeyReference(
        repo: CardRepository,
        aid: String,
        requirement: VerifyRequirement
    ): Boolean {
        val keyReference = requirement.keyReference
        var retries = requirement.retries

        while (true) {
            val code = showVerifyDialog(keyReference, retries) ?: return false
            val response = repo.verifyPin(keyReference, code, aid, viewModel.fileId)
            if (response.isOk) return true

            when (val status = verifyStatus(response.sw, keyReference)) {
                VerifyStatus.Verified -> return true
                is VerifyStatus.Available -> retries = status.retries
                is VerifyStatus.Unavailable -> {
                    showVerifyUnavailableMessage(status)
                    return false
                }
            }
            showMessage(getString(R.string.edit_mode_verify_failed, retries))
        }
    }

    private fun verifyStatus(sw: Int, keyReference: KeyReference): VerifyStatus {
        return when {
            sw == Result.SW_NORMAL -> VerifyStatus.Verified
            sw == Result.SW_AUTH_METHOD_BLOCKED ->
                VerifyStatus.Unavailable(keyReference, VerifyUnavailableReason.BLOCKED)
            sw == 0x63C1 ->
                VerifyStatus.Unavailable(keyReference, VerifyUnavailableReason.LAST_ATTEMPT)
            sw in 0x63C2..0x63CF -> VerifyStatus.Available(sw and 0x0F, keyReference)
            else -> VerifyStatus.Unavailable(keyReference, VerifyUnavailableReason.UNKNOWN)
        }
    }

    private fun showVerifyUnavailableMessage(status: VerifyStatus.Unavailable) {
        val name = getString(keyReferenceDisplayNameResId(status.keyReference))
        val message = when (status.reason) {
            VerifyUnavailableReason.BLOCKED -> getString(R.string.edit_mode_blocked, name)
            VerifyUnavailableReason.LAST_ATTEMPT ->
                getString(R.string.edit_mode_last_attempt, name)
            VerifyUnavailableReason.UNKNOWN ->
                getString(R.string.edit_mode_retries_unknown, name)
        }
        showMessage(message)
    }

    private fun showArrAccessKeyUnavailableMessage(statuses: List<VerifyStatus>) {
        val messageResId = arrAccessKeyUnavailableMessageResId(statuses)
        if (messageResId != null) {
            showMessage(getString(messageResId))
            return
        }
        if (statuses.any { it is VerifyStatus.Verified }) {
            showMessage(getString(R.string.edit_mode_arr_access_keys_unavailable))
            return
        }

        statuses.filterIsInstance<VerifyStatus.Unavailable>()
            .firstOrNull()
            ?.let { showVerifyUnavailableMessage(it) }
    }

    private suspend fun showVerifyOptionDialog(
        candidates: List<VerifyCandidate>
    ): VerifyCandidate? = suspendCancellableCoroutine { continuation ->
        val labels = candidates.map { candidate ->
            candidate.requirements.joinToString(" + ") {
                getString(keyReferenceDisplayNameResId(it.keyReference))
            }
        }.toTypedArray()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_mode_verify_option_title)
            .setItems(labels) { _, which ->
                if (continuation.isActive) continuation.resume(candidates[which])
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                if (continuation.isActive) continuation.resume(null)
            }
            .setOnCancelListener {
                if (continuation.isActive) continuation.resume(null)
            }
            .show()

        continuation.invokeOnCancellation { dialog.dismiss() }
    }

    private suspend fun showVerifySingleOptionDialog(
        titleResId: Int,
        candidates: List<VerifyRequirement>
    ): VerifyRequirement? = suspendCancellableCoroutine { continuation ->
        val labels = candidates.map {
            getString(keyReferenceDisplayNameResId(it.keyReference))
        }.toTypedArray()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleResId)
            .setItems(labels) { _, which ->
                if (continuation.isActive) continuation.resume(candidates[which])
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                if (continuation.isActive) continuation.resume(null)
            }
            .setOnCancelListener {
                if (continuation.isActive) continuation.resume(null)
            }
            .show()

        continuation.invokeOnCancellation { dialog.dismiss() }
    }

    private suspend fun showVerifyDialog(
        keyReference: KeyReference,
        retries: Int
    ): String? = suspendCancellableCoroutine { continuation ->
        val input = EditText(requireContext()).apply {
            typeface = Typeface.MONOSPACE
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            filters = arrayOf(hexInputFilter(), InputFilter.LengthFilter(MAX_VERIFY_CODE_DIGITS))
            isSingleLine = true
        }
        val message = TextView(requireContext()).apply {
            text = getString(R.string.edit_mode_verify_message, retries)
        }
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val padding = resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin)
            setPadding(padding, padding, padding, 0)
            addView(message)
            addView(input)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(
                R.string.edit_mode_verify_title,
                getString(keyReferenceDisplayNameResId(keyReference))
            ))
            .setView(layout)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                if (continuation.isActive) continuation.resume(null)
            }
            .setOnCancelListener {
                if (continuation.isActive) continuation.resume(null)
            }
            .show()

        continuation.invokeOnCancellation { dialog.dismiss() }
        val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
        positiveButton.isEnabled = false
        positiveButton.setOnClickListener {
            if (continuation.isActive) continuation.resume(input.text.toString())
            dialog.dismiss()
        }
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No-op.
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                positiveButton.isEnabled = length in MIN_VERIFY_CODE_DIGITS..MAX_VERIFY_CODE_DIGITS
                        && length % 2 == 0
            }

            override fun afterTextChanged(s: Editable?) {
                // No-op.
            }
        })
    }

    private fun hexInputFilter() = InputFilter { source, _, _, _, _, _ ->
        val sourceText = source.toString()
        val filtered = sourceText.filter { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
            .uppercase()
        if (filtered.length == sourceText.length) filtered else filtered
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun setupRecordSelector() {
        binding.recordDropdown.setOnItemClickListener { _, _, position, _ ->
            if (editModeEnabled) stopEditMode(refresh = false)
            binaryViewModel.loadRecord(position + 1)
        }
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.recordSelectorLayout.isEnabled =
                    buildRecordSelectorState(
                        binaryViewModel.recordCount.value,
                        position,
                        viewModel.hasDecoder
                    ).enabled
            }
        })
    }

    private fun observeBinaryViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    binaryViewModel.recordCount.collect { count ->
                        val state = buildRecordSelectorState(
                            count,
                            binding.viewPager.currentItem,
                            viewModel.hasDecoder
                        )
                        binding.recordSelectorLayout.visibility =
                            if (state.visible) View.VISIBLE else View.GONE
                        if (count > 0) {
                            val items = (1..count).map { "#$it" }
                            binding.recordDropdown.setAdapter(
                                ArrayAdapter(
                                    requireContext(),
                                    android.R.layout.simple_list_item_1,
                                    items
                                )
                            )
                            binding.recordDropdown.setText(items[0], false)
                        }
                        binding.recordSelectorLayout.isEnabled = state.enabled
                    }
                }
                launch {
                    binaryViewModel.error.collect { result ->
                        if (result != null) {
                            handleReadError(result)
                        }
                    }
                }
            }
        }
    }

    private fun observeKeyboardEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.keyboardEvent.collect { event ->
                    when (event) {
                        EfDetailViewModel.KeyboardEvent.QUIT -> quitEditMode()
                        EfDetailViewModel.KeyboardEvent.SAVE -> saveEditMode()
                    }
                }
            }
        }
    }

    private suspend fun quitEditMode() {
        if (confirm(R.string.edit_quit_confirm_message)) {
            stopEditMode(refresh = true)
        }
    }

    private suspend fun saveEditMode() {
        if (!confirm(R.string.edit_save_confirm_message)) return
        val slotId = mainViewModel.selectedSlot.value?.slotId
        val repo = slotId?.let { CardRepository.from(requireContext(), it) }
        if (slotId == null || repo == null) {
            showMessage(getString(R.string.edit_mode_card_unavailable))
            return
        }
        val result = binaryViewModel.save(repo)
        if (result == null) {
            showMessage(getString(R.string.edit_save_failed_unknown_file_type))
            return
        }
        if (result.sw == Result.SW_INSUFFICIENT_SECURITY
            && verifyAccessRequirements(slotId, repo, EditAccessUseCase.RequiredAccess.UPDATE)
        ) {
            showSaveResult(binaryViewModel.save(repo) ?: result)
            return
        }
        showSaveResult(result)
    }

    private fun showSaveResult(result: Result) {
        if (result.isOk) {
            showMessage(getString(R.string.edit_save_success))
        } else {
            showReadError(result)
        }
    }

    private fun stopEditMode(refresh: Boolean) {
        editModeEnabled = false
        viewModel.disableEditMode()
        binaryViewModel.cancelEditMode()
        requireActivity().invalidateOptionsMenu()
        if (refresh) binaryViewModel.refresh()
    }

    private suspend fun confirm(messageResId: Int): Boolean =
        suspendCancellableCoroutine { continuation ->
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setMessage(messageResId)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    if (continuation.isActive) continuation.resume(true)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    if (continuation.isActive) continuation.resume(false)
                }
                .setOnCancelListener {
                    if (continuation.isActive) continuation.resume(false)
                }
                .show()

            continuation.invokeOnCancellation { dialog.dismiss() }
        }

    private fun handleReadError(result: Result) {
        if (shouldAttemptReadAccessRecovery(result, readAccessRecoveryInProgress)) {
            readAccessRecoveryInProgress = true
            binaryViewModel.clearError()
            viewLifecycleOwner.lifecycleScope.launch {
                val recovered = try {
                    val slotId = mainViewModel.selectedSlot.value?.slotId
                    val repo = slotId?.let { CardRepository.from(requireContext(), it) }
                    if (slotId != null && repo != null) {
                        enableReadAccess(slotId, repo)
                    } else {
                        false
                    }
                } finally {
                    readAccessRecoveryInProgress = false
                }
                if (recovered) {
                    binaryViewModel.refresh()
                } else {
                    showReadError(result)
                }
            }
            return
        }

        showReadError(result)
        binaryViewModel.clearError()
    }

    private fun showReadError(result: Result) {
        Snackbar.make(
            binding.root,
            buildErrorMessage(result, getString(errorMessageResId(result))),
            Snackbar.LENGTH_LONG
        ).show()
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            requireActivity().title = viewModel.title
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        data class RecordSelectorState(val visible: Boolean, val enabled: Boolean)

        internal fun buildRecordSelectorState(
            recordCount: Int,
            tabPosition: Int,
            hasDecoder: Boolean
        ) =
            RecordSelectorState(
                visible = recordCount > 0,
                enabled = tabPosition != if (hasDecoder) 2 else 1
            )

        internal fun errorMessageResId(result: Result) = when (result.sw) {
            Result.SW_INSUFFICIENT_SECURITY -> R.string.sw6982_insufficient_security
            Result.SW_AUTH_METHOD_BLOCKED -> R.string.sw6983_auth_method_blocked
            Result.SW_REF_DATA_INVALIDATED -> R.string.sw6984_ref_data_invalidated
            Result.SW_NOT_FOUND -> R.string.sw6a82_file_not_found
            else -> R.string.sw_unhandled_error
        }

        internal fun buildErrorMessage(result: Result, errorMessage: String): String =
            "SW %04X: %s".format(result.sw, errorMessage)

        internal fun requiresReadAccessForEdit(readError: Result?): Boolean =
            readError?.sw == Result.SW_INSUFFICIENT_SECURITY

        internal fun shouldAttemptReadAccessRecovery(
            readError: Result,
            inProgress: Boolean
        ): Boolean = readError.sw == Result.SW_INSUFFICIENT_SECURITY && !inProgress

        internal fun messageResId(failure: EditAccessUseCase.Failure) = when (failure) {
            EditAccessUseCase.Failure.CARD_UNAVAILABLE -> R.string.edit_mode_card_unavailable
            EditAccessUseCase.Failure.FCP_UNAVAILABLE -> R.string.edit_mode_fcp_unavailable
            EditAccessUseCase.Failure.SECURITY_ATTRIBUTES_UNAVAILABLE ->
                R.string.edit_mode_security_attributes_unavailable
            EditAccessUseCase.Failure.SECURITY_CONDITION_UNSUPPORTED ->
                R.string.edit_mode_security_condition_unsupported
            EditAccessUseCase.Failure.ARR_READ_FAILED -> R.string.edit_mode_arr_read_failed
            EditAccessUseCase.Failure.ARR_ACCESS_KEYS_UNAVAILABLE ->
                R.string.edit_mode_arr_access_keys_unavailable
            EditAccessUseCase.Failure.ARR_RECORD_UNAVAILABLE ->
                R.string.edit_mode_arr_record_unavailable
        }

        internal fun arrAccessKeyUnavailableMessageResId(statuses: List<VerifyStatus>) =
            when {
                statuses.isNotEmpty() && statuses.all {
                    it is VerifyStatus.Unavailable
                            && it.reason == VerifyUnavailableReason.BLOCKED
                } -> R.string.edit_mode_arr_no_verifiable_key
                statuses.isNotEmpty() && statuses.all {
                    it is VerifyStatus.Unavailable
                            && (it.reason == VerifyUnavailableReason.BLOCKED
                            || it.reason == VerifyUnavailableReason.LAST_ATTEMPT)
                } -> R.string.edit_mode_arr_no_safe_key
                else -> null
            }

        internal fun keyReferenceDisplayNameResId(keyReference: KeyReference): Int =
            when (keyReference) {
                KeyReference.APPLICATION_PIN1 -> R.string.key_reference_application_pin1
                KeyReference.APPLICATION_PIN2 -> R.string.key_reference_application_pin2
                KeyReference.APPLICATION_PIN3 -> R.string.key_reference_application_pin3
                KeyReference.APPLICATION_PIN4 -> R.string.key_reference_application_pin4
                KeyReference.APPLICATION_PIN5 -> R.string.key_reference_application_pin5
                KeyReference.APPLICATION_PIN6 -> R.string.key_reference_application_pin6
                KeyReference.APPLICATION_PIN7 -> R.string.key_reference_application_pin7
                KeyReference.APPLICATION_PIN8 -> R.string.key_reference_application_pin8
                KeyReference.ADM1 -> R.string.key_reference_adm1
                KeyReference.ADM2 -> R.string.key_reference_adm2
                KeyReference.ADM3 -> R.string.key_reference_adm3
                KeyReference.ADM4 -> R.string.key_reference_adm4
                KeyReference.ADM5 -> R.string.key_reference_adm5
                KeyReference.UNIVERSAL_PIN -> R.string.key_reference_universal_pin
                KeyReference.LOCAL_PIN1 -> R.string.key_reference_local_pin1
                KeyReference.LOCAL_PIN2 -> R.string.key_reference_local_pin2
                KeyReference.LOCAL_PIN3 -> R.string.key_reference_local_pin3
                KeyReference.LOCAL_PIN4 -> R.string.key_reference_local_pin4
                KeyReference.LOCAL_PIN5 -> R.string.key_reference_local_pin5
                KeyReference.LOCAL_PIN6 -> R.string.key_reference_local_pin6
                KeyReference.LOCAL_PIN7 -> R.string.key_reference_local_pin7
                KeyReference.LOCAL_PIN8 -> R.string.key_reference_local_pin8
                KeyReference.ADM6 -> R.string.key_reference_adm6
                KeyReference.ADM7 -> R.string.key_reference_adm7
                KeyReference.ADM8 -> R.string.key_reference_adm8
                KeyReference.ADM9 -> R.string.key_reference_adm9
                KeyReference.ADM10 -> R.string.key_reference_adm10
            }

        const val ARG_EF_NAME = "efName"
        const val ARG_EF_FILE_ID = "efFileId"
        const val ARG_AID = "aid"
        const val ARG_PARENT_PATH = "parentPath"
        private const val MIN_VERIFY_CODE_DIGITS = 2
        private const val MAX_VERIFY_CODE_DIGITS = 16
    }

    private data class VerifyRequirement(
        val keyReference: KeyReference,
        val retries: Int
    )

    private data class VerifyCandidate(
        val requirements: List<VerifyRequirement>,
        val trustedKeyReferences: Set<KeyReference>
    )

    internal sealed class VerifyStatus {
        object Verified : VerifyStatus()
        data class Available(
            val retries: Int,
            val keyReference: KeyReference
        ) : VerifyStatus()
        data class Unavailable(
            val keyReference: KeyReference,
            val reason: VerifyUnavailableReason
        ) : VerifyStatus()
    }

    internal enum class VerifyUnavailableReason {
        BLOCKED,
        LAST_ATTEMPT,
        UNKNOWN
    }
}
