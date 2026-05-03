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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.github.cheeriotb.uiccbrowser.databinding.FragmentBinaryBinding
import com.github.cheeriotb.uiccbrowser.ui.MainViewModel
import kotlinx.coroutines.launch

class BinaryFragment : Fragment() {

    private var _binding: FragmentBinaryBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var viewModel: BinaryViewModel
    private lateinit var gridAdapter: BinaryGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBinaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val efDetailViewModel =
            ViewModelProvider(requireParentFragment())[EfDetailViewModel::class.java]
        val fileId = efDetailViewModel.fileId
        val slotId = mainViewModel.selectedSlot.value?.slotId ?: 0

        viewModel = ViewModelProvider(
            requireParentFragment(),
            BinaryViewModel.Factory(requireActivity().application, fileId, slotId)
        )[BinaryViewModel::class.java]

        setupRecordSpinner()
        setupHeaderRecyclerView()
        setupDataRecyclerView()
        observeViewModel()
    }

    private fun setupRecordSpinner() {
        binding.recordDropdown.setOnItemClickListener { _, _, position, _ ->
            viewModel.loadRecord(position + 1)
        }
    }

    private fun setupHeaderRecyclerView() {
        val spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) = if (position % 9 == 0) 2 else 1
        }
        binding.headerRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 10).apply {
                this.spanSizeLookup = spanSizeLookup
            }
            adapter = BinaryHeaderAdapter()
            isNestedScrollingEnabled = false
        }
    }

    private fun setupDataRecyclerView() {
        val spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) = if (position % 9 == 0) 2 else 1
        }
        gridAdapter = BinaryGridAdapter()
        binding.dataRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 10).apply {
                this.spanSizeLookup = spanSizeLookup
            }
            adapter = gridAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.progressIndicator.isVisible = loading
                        binding.headerRecyclerView.isVisible = !loading
                        binding.dataRecyclerView.isVisible = !loading
                    }
                }
                launch {
                    viewModel.data.collect { data ->
                        if (data != null) gridAdapter.updateData(data)
                    }
                }
                launch {
                    viewModel.recordCount.collect { count ->
                        if (count > 0) {
                            val items = (1..count).map { "#$it" }
                            val adapter = ArrayAdapter(
                                requireContext(),
                                android.R.layout.simple_list_item_1,
                                items
                            )
                            binding.recordDropdown.setAdapter(adapter)
                            binding.recordDropdown.setText(items[0], false)
                            binding.recordSelectorLayout.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
