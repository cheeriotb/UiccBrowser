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
import com.github.cheeriotb.uiccbrowser.databinding.FragmentFcpBinding
import com.github.cheeriotb.uiccbrowser.ui.MainViewModel
import kotlinx.coroutines.launch

class FcpFragment : Fragment() {

    private var _binding: FragmentFcpBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var fcpDataViewModel: FcpDataViewModel
    private lateinit var fcpViewModel: FcpViewModel
    private lateinit var treeAdapter: TlvTreeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFcpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val efDetailViewModel =
            ViewModelProvider(requireParentFragment())[EfDetailViewModel::class.java]
        val slotId = mainViewModel.selectedSlot.value?.slotId ?: 0

        fcpDataViewModel = ViewModelProvider(
            requireParentFragment(),
            FcpDataViewModel.Factory(requireActivity().application, efDetailViewModel.fileId, slotId)
        )[FcpDataViewModel::class.java]

        fcpViewModel = ViewModelProvider(
            this,
            FcpViewModel.Factory(requireActivity().application, efDetailViewModel.fileId)
        )[FcpViewModel::class.java]

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
                    fcpDataViewModel.isLoading.collect { loading ->
                        binding.progressIndicator.isVisible = loading
                    }
                }
                launch {
                    fcpDataViewModel.data.collect { data ->
                        if (data != null) fcpViewModel.decode(data)
                    }
                }
                launch {
                    fcpViewModel.element.collect { element ->
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
