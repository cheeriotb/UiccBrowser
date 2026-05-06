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
import com.github.cheeriotb.uiccbrowser.repository.VerifyPinQualifier
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
                            binaryViewModel.refresh()
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
                requireActivity().invalidateOptionsMenu()
                showMessage(getString(R.string.edit_mode_enabled))
            }
        }
    }

    private suspend fun enableEditMode(
        slotId: Int,
        repo: CardRepository
    ): Boolean {
        while (true) {
            val outcome = EditAccessUseCase(requireContext()).execute(slotId, viewModel.fileId)
            val failure = outcome.failure
            if (failure != null) {
                showMessage(getString(messageResId(failure)))
                return false
            }

            if (outcome.exploreQualifierOptions.isNotEmpty()) {
                if (!verifyArrAccessKeyOptions(repo, outcome.exploreQualifierOptions)) {
                    return false
                }
                continue
            }

            val verified = verifyQualifierOptions(
                repo,
                viewModel.fileId.aid,
                outcome.qualifierOptions
            )
            return verified
        }
    }

    private suspend fun verifyQualifierOptions(
        repo: CardRepository,
        aid: String,
        qualifierOptions: List<List<VerifyPinQualifier>>
    ): Boolean {
        if (qualifierOptions.isEmpty()) return true

        val candidates = mutableListOf<List<VerifyRequirement>>()
        val unavailable = mutableListOf<VerifyStatus.Unavailable>()
        for (option in qualifierOptions) {
            if (option.isEmpty()) return true
            val requirements = mutableListOf<VerifyRequirement>()
            var optionUsable = true
            for (qualifier in option) {
                when (val status = verifyStatus(
                    repo.queryVerifyPinRetries(qualifier, aid).sw,
                    qualifier
                )) {
                    VerifyStatus.Verified -> Unit
                    is VerifyStatus.Available ->
                        requirements.add(VerifyRequirement(qualifier, status.retries))
                    is VerifyStatus.Unavailable -> {
                        unavailable.add(status)
                        requirements.clear()
                        optionUsable = false
                        break
                    }
                }
            }
            if (optionUsable && requirements.isEmpty()) return true
            if (optionUsable && requirements.isNotEmpty()) candidates.add(requirements)
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
        for (requirement in selected) {
            if (!verifyQualifier(repo, aid, requirement)) return false
        }
        return true
    }

    private suspend fun verifyArrAccessKeyOptions(
        repo: CardRepository,
        qualifiers: List<VerifyPinQualifier>
    ): Boolean {
        val statuses = qualifiers.map { qualifier ->
            verifyStatus(repo.queryVerifyPinRetries(qualifier).sw, qualifier)
        }

        val candidates = statuses.filterIsInstance<VerifyStatus.Available>()
            .map { VerifyRequirement(it.qualifier, it.retries) }
        if (candidates.isEmpty()) {
            showArrAccessKeyUnavailableMessage(statuses)
            return false
        }

        val selected = showVerifySingleOptionDialog(
            R.string.edit_mode_arr_verify_option_title,
            candidates
        ) ?: return false
        return verifyQualifier(repo, FileId.AID_NONE, selected)
    }

    private suspend fun verifyQualifier(
        repo: CardRepository,
        aid: String,
        requirement: VerifyRequirement
    ): Boolean {
        val qualifier = requirement.qualifier
        var retries = requirement.retries

        while (true) {
            val code = showVerifyDialog(qualifier, retries) ?: return false
            val response = repo.verifyPin(qualifier, code, aid)
            if (response.isOk) return true

            when (val status = verifyStatus(response.sw, qualifier)) {
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

    private fun verifyStatus(sw: Int, qualifier: VerifyPinQualifier): VerifyStatus {
        return when {
            sw == Result.SW_NORMAL -> VerifyStatus.Verified
            sw == Result.SW_AUTH_METHOD_BLOCKED ->
                VerifyStatus.Unavailable(qualifier, VerifyUnavailableReason.BLOCKED)
            sw == 0x63C1 ->
                VerifyStatus.Unavailable(qualifier, VerifyUnavailableReason.LAST_ATTEMPT)
            sw in 0x63C2..0x63CF -> VerifyStatus.Available(sw and 0x0F, qualifier)
            else -> VerifyStatus.Unavailable(qualifier, VerifyUnavailableReason.UNKNOWN)
        }
    }

    private fun showVerifyUnavailableMessage(status: VerifyStatus.Unavailable) {
        val name = verifyPinQualifierDisplayName(status.qualifier)
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
        candidates: List<List<VerifyRequirement>>
    ): List<VerifyRequirement>? = suspendCancellableCoroutine { continuation ->
        val labels = candidates.map { requirements ->
            requirements.joinToString(" + ") {
                verifyPinQualifierDisplayName(it.qualifier)
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
            verifyPinQualifierDisplayName(it.qualifier)
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
        qualifier: VerifyPinQualifier,
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
                verifyPinQualifierDisplayName(qualifier)
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
                            Snackbar.make(
                                binding.root,
                                buildErrorMessage(result, getString(errorMessageResId(result))),
                                Snackbar.LENGTH_LONG
                            ).show()
                            binaryViewModel.clearError()
                        }
                    }
                }
            }
        }
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

        internal fun verifyPinQualifierDisplayName(qualifier: VerifyPinQualifier): String =
            when (qualifier) {
                VerifyPinQualifier.GLOBAL_PIN1 -> "Global PIN1"
                VerifyPinQualifier.GLOBAL_PIN2 -> "Global PIN2"
                VerifyPinQualifier.GLOBAL_PIN3 -> "Global PIN3"
                VerifyPinQualifier.GLOBAL_PIN4 -> "Global PIN4"
                VerifyPinQualifier.GLOBAL_PIN5 -> "Global PIN5"
                VerifyPinQualifier.GLOBAL_PIN6 -> "Global PIN6"
                VerifyPinQualifier.GLOBAL_PIN7 -> "Global PIN7"
                VerifyPinQualifier.GLOBAL_PIN8 -> "Global PIN8"
                VerifyPinQualifier.ADM1 -> "ADM1"
                VerifyPinQualifier.ADM2 -> "ADM2"
                VerifyPinQualifier.ADM3 -> "ADM3"
                VerifyPinQualifier.ADM4 -> "ADM4"
                VerifyPinQualifier.ADM5 -> "ADM5"
                VerifyPinQualifier.LOCAL_PIN1 -> "Local PIN1"
                VerifyPinQualifier.LOCAL_PIN2 -> "Local PIN2"
                VerifyPinQualifier.LOCAL_PIN3 -> "Local PIN3"
                VerifyPinQualifier.LOCAL_PIN4 -> "Local PIN4"
                VerifyPinQualifier.LOCAL_PIN5 -> "Local PIN5"
                VerifyPinQualifier.LOCAL_PIN6 -> "Local PIN6"
                VerifyPinQualifier.LOCAL_PIN7 -> "Local PIN7"
                VerifyPinQualifier.LOCAL_PIN8 -> "Local PIN8"
                VerifyPinQualifier.ADM6 -> "ADM6"
                VerifyPinQualifier.ADM7 -> "ADM7"
                VerifyPinQualifier.ADM8 -> "ADM8"
                VerifyPinQualifier.ADM9 -> "ADM9"
                VerifyPinQualifier.ADM10 -> "ADM10"
            }

        const val ARG_EF_NAME = "efName"
        const val ARG_EF_FILE_ID = "efFileId"
        const val ARG_AID = "aid"
        const val ARG_PARENT_PATH = "parentPath"
        private const val MIN_VERIFY_CODE_DIGITS = 2
        private const val MAX_VERIFY_CODE_DIGITS = 16
    }

    private data class VerifyRequirement(
        val qualifier: VerifyPinQualifier,
        val retries: Int
    )

    internal sealed class VerifyStatus {
        object Verified : VerifyStatus()
        data class Available(
            val retries: Int,
            val qualifier: VerifyPinQualifier
        ) : VerifyStatus()
        data class Unavailable(
            val qualifier: VerifyPinQualifier,
            val reason: VerifyUnavailableReason
        ) : VerifyStatus()
    }

    internal enum class VerifyUnavailableReason {
        BLOCKED,
        LAST_ATTEMPT,
        UNKNOWN
    }
}
