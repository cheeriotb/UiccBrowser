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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.cheeriotb.uiccbrowser.databinding.FragmentInfoBinding
import com.github.cheeriotb.uiccbrowser.ui.MainViewModel
import kotlinx.coroutines.launch

class InfoFragment : Fragment() {

    private var _binding: FragmentInfoBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var binaryViewModel: BinaryViewModel
    private lateinit var infoViewModel: InfoViewModel
    private lateinit var treeAdapter: TlvTreeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val efDetailViewModel =
            ViewModelProvider(requireParentFragment())[EfDetailViewModel::class.java]
        val slotId = mainViewModel.selectedSlot.value?.slotId ?: 0

        binaryViewModel = ViewModelProvider(
            requireParentFragment(),
            BinaryViewModel.Factory(requireActivity().application, efDetailViewModel.fileId, slotId)
        )[BinaryViewModel::class.java]

        infoViewModel = ViewModelProvider(
            this,
            InfoViewModel.Factory(requireActivity().application, efDetailViewModel.fileId)
        )[InfoViewModel::class.java]

        treeAdapter = TlvTreeAdapter()
        binding.tlvTreeRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = treeAdapter
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    binaryViewModel.isLoading.collect { loading ->
                        binding.progressIndicator.isVisible = loading
                    }
                }
                launch {
                    binaryViewModel.data.collect { data ->
                        if (data != null) infoViewModel.decode(data)
                    }
                }
                launch {
                    infoViewModel.element.collect { element ->
                        if (element != null) treeAdapter.updateTree(element)
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
