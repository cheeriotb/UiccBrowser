/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui.efdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.github.cheeriotb.uiccbrowser.R
import com.github.cheeriotb.uiccbrowser.databinding.FragmentEfDetailBinding
import com.github.cheeriotb.uiccbrowser.repository.FileId
import com.github.cheeriotb.uiccbrowser.repository.Result
import com.github.cheeriotb.uiccbrowser.ui.MainViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class EfDetailFragment : Fragment() {

    private var _binding: FragmentEfDetailBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var viewModel: EfDetailViewModel
    private lateinit var binaryViewModel: BinaryViewModel

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
                    tab.icon = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_binary)
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
        observeBinaryViewModel()
    }

    private fun setupRecordSelector() {
        binding.recordDropdown.setOnItemClickListener { _, _, position, _ ->
            binaryViewModel.loadRecord(position + 1)
        }
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.recordSelectorLayout.isEnabled =
                    buildRecordSelectorState(binaryViewModel.recordCount.value, position, viewModel.hasDecoder).enabled
            }
        })
    }

    private fun observeBinaryViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    binaryViewModel.recordCount.collect { count ->
                        val state = buildRecordSelectorState(count, binding.viewPager.currentItem, viewModel.hasDecoder)
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

        internal fun buildRecordSelectorState(recordCount: Int, tabPosition: Int, hasDecoder: Boolean) =
            RecordSelectorState(
                visible = recordCount > 0,
                enabled = tabPosition != if (hasDecoder) 2 else 1
            )

        internal fun errorMessageResId(result: Result) = when (result.sw) {
            Result.SW_INSUFFICIENT_SECURITY -> R.string.sw6982_insufficient_security
            Result.SW_NOT_FOUND -> R.string.sw6a82_file_not_found
            else -> R.string.sw_unknown_error
        }

        internal fun buildErrorMessage(result: Result, errorMessage: String): String =
            "SW %04X: %s".format(result.sw, errorMessage)

        const val ARG_EF_NAME = "efName"
        const val ARG_EF_FILE_ID = "efFileId"
        const val ARG_AID = "aid"
        const val ARG_PARENT_PATH = "parentPath"
    }
}
