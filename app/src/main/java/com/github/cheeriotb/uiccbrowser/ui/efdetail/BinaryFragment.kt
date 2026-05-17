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
import android.widget.HorizontalScrollView
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.github.cheeriotb.uiccbrowser.R
import com.github.cheeriotb.uiccbrowser.databinding.FragmentBinaryBinding
import com.github.cheeriotb.uiccbrowser.ui.MainViewModel
import kotlinx.coroutines.launch

class BinaryFragment : Fragment() {

    private var _binding: FragmentBinaryBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var efDetailViewModel: EfDetailViewModel
    private lateinit var viewModel: BinaryViewModel
    private lateinit var gridAdapter: BinaryGridAdapter

    companion object {
        /** Returns a grid width that fills [availableWidth] without shrinking below [minWidth]. */
        internal fun gridContentWidth(minWidth: Int, availableWidth: Int): Int =
            maxOf(minWidth, availableWidth)
    }

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

        efDetailViewModel =
            ViewModelProvider(requireParentFragment())[EfDetailViewModel::class.java]
        val fileId = efDetailViewModel.fileId
        val slotId = mainViewModel.selectedSlot.value?.slotId ?: 0

        viewModel = ViewModelProvider(
            requireParentFragment(),
            BinaryViewModel.Factory(requireActivity().application, fileId, slotId)
        )[BinaryViewModel::class.java]

        setupHeaderRecyclerView()
        setupDataRecyclerView()
        setupGridWidth()
        setupGridHorizontalScrollSync()
        setupKeyboard()
        observeViewModel()
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
        gridAdapter = BinaryGridAdapter { byteIndex ->
            viewModel.moveCursor(byteIndex)
        }
        binding.dataRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 10).apply {
                this.spanSizeLookup = spanSizeLookup
            }
            adapter = gridAdapter
        }
    }

    /**
     * Expands the binary grid to the viewport while preserving a minimum width for two hex digits.
     */
    private fun setupGridWidth() {
        binding.root.doOnLayout {
            applyGridContentWidth()
        }
        binding.dataScrollView.doOnLayout { view ->
            applyGridContentWidth(view.width)
        }
        binding.dataScrollView.addOnLayoutChangeListener { view, left, _, right, _,
            oldLeft, _, oldRight, _ ->
            val width = right - left
            val oldWidth = oldRight - oldLeft
            if (width != oldWidth) {
                applyGridContentWidth(view.width)
            }
        }
    }

    private fun applyGridContentWidth(availableWidth: Int) {
        if (availableWidth <= 0) return

        val minWidth = resources.getDimensionPixelSize(R.dimen.binary_grid_min_width)
        val width = gridContentWidth(minWidth, availableWidth)
        setViewWidth(binding.headerRecyclerView, width)
        setViewWidth(binding.dataRecyclerView, width)
    }

    private fun applyGridContentWidth() {
        val availableWidth = maxOf(binding.headerScrollView.width, binding.dataScrollView.width)
        applyGridContentWidth(availableWidth)
    }

    private fun setViewWidth(view: View, width: Int) {
        if (view.layoutParams.width != width) {
            view.layoutParams = view.layoutParams.apply {
                this.width = width
            }
        }
    }

    /**
     * Keeps the binary header aligned with the horizontally scrollable data grid.
     */
    private fun setupGridHorizontalScrollSync() {
        syncHorizontalScrollViews(binding.headerScrollView, binding.dataScrollView)
        syncHorizontalScrollViews(binding.dataScrollView, binding.headerScrollView)
    }

    private fun syncHorizontalScrollViews(
        source: HorizontalScrollView,
        target: HorizontalScrollView
    ) {
        source.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            if (target.scrollX != scrollX) {
                target.scrollTo(scrollX, 0)
            }
        }
    }

    private fun setupKeyboard() {
        mapOf(
            binding.keyboardButton0 to '0',
            binding.keyboardButton1 to '1',
            binding.keyboardButton2 to '2',
            binding.keyboardButton3 to '3',
            binding.keyboardButton4 to '4',
            binding.keyboardButton5 to '5',
            binding.keyboardButton6 to '6',
            binding.keyboardButton7 to '7',
            binding.keyboardButton8 to '8',
            binding.keyboardButton9 to '9',
            binding.keyboardButtonA to 'A',
            binding.keyboardButtonB to 'B',
            binding.keyboardButtonC to 'C',
            binding.keyboardButtonD to 'D',
            binding.keyboardButtonE to 'E',
            binding.keyboardButtonF to 'F'
        ).forEach { (button, digit) ->
            button.setOnClickListener { viewModel.inputHexDigit(digit) }
        }
        binding.keyboardButtonIns.setOnClickListener { viewModel.insertByte() }
        binding.keyboardButtonDel.setOnClickListener { viewModel.deleteByte() }
        binding.keyboardButtonCancel.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                efDetailViewModel.requestQuitEditMode()
            }
        }
        binding.keyboardButtonSave.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                efDetailViewModel.requestSaveEditMode()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.progressIndicator.isVisible = loading
                        binding.headerScrollView.isVisible =
                            !loading && viewModel.data.value?.isNotEmpty() == true
                        binding.dataScrollView.isVisible = !loading
                        binding.root.post { applyGridContentWidth() }
                    }
                }
                launch {
                    viewModel.data.collect { data ->
                        if (data != null) {
                            gridAdapter.updateData(data)
                            if (efDetailViewModel.isEditModeEnabled.value) {
                                viewModel.startEditMode()
                            }
                        }
                    }
                }
                launch {
                    viewModel.editState.collect { state ->
                        gridAdapter.updateEditState(state)
                        if (state.enabled && state.cursorIndex >= 0) {
                            binding.dataRecyclerView.scrollToPosition(
                                BinaryViewModel.gridPositionForByteIndex(state.cursorIndex)
                            )
                        }
                    }
                }
                launch {
                    efDetailViewModel.isEditModeEnabled.collect { enabled ->
                        binding.editKeyboardLayout.isVisible = enabled
                        if (enabled) {
                            viewModel.startEditMode()
                        } else {
                            viewModel.cancelEditMode()
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
