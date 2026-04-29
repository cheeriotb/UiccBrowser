/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui.filebrowser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.cheeriotb.uiccbrowser.R
import com.github.cheeriotb.uiccbrowser.databinding.FragmentFileBrowserBinding
import com.github.cheeriotb.uiccbrowser.repository.FileId
import com.github.cheeriotb.uiccbrowser.ui.MainViewModel
import com.github.cheeriotb.uiccbrowser.ui.NavLevel
import com.github.cheeriotb.uiccbrowser.ui.NavItem
import kotlinx.coroutines.launch

class FileBrowserFragment : Fragment() {

    private var _binding: FragmentFileBrowserBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var viewModel: FileBrowserViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFileBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments
        if (args == null) {
            val navItem = mainViewModel.selectedNavItem.value
            if (navItem == null) {
                observeAndInitOnNavItemAvailable()
                return
            }
            initFromNavItem(navItem)
        } else {
            initFromArgs(args)
        }
    }

    private fun observeAndInitOnNavItemAvailable() {
        viewLifecycleOwner.lifecycleScope.launch {
            mainViewModel.selectedNavItem.collect { navItem ->
                if (navItem != null && !::viewModel.isInitialized) {
                    initFromNavItem(navItem)
                }
            }
        }
    }

    private fun initFromNavItem(navItem: NavItem) {
        val slotId = mainViewModel.selectedSlot.value?.slotId ?: return
        val rawResId = navItem.rawResId()
        val aid = navItem.aid ?: FileId.AID_NONE
        val parentPath = navItem.initialPath()
        val displayPath = navItem.displayPath()

        val (rootName, rootId) = try {
            com.github.cheeriotb.uiccbrowser.usecase.GetFileListUseCase(requireContext())
                .parseRootMeta(rawResId)
        } catch (_: Exception) {
            navItem.label to ""
        }
        val title = FileBrowserViewModel.buildRootTitle(rootName, rootId)

        viewModel = ViewModelProvider(
            this,
            FileBrowserViewModel.Factory(requireActivity().application, rawResId, slotId, aid, parentPath, title)
        )[FileBrowserViewModel::class.java]

        setupUi(displayPath)
    }

    private fun initFromArgs(args: Bundle) {
        val rawResId = args.getInt(ARG_RAW_RES_ID)
        val aid = args.getString(ARG_AID) ?: FileId.AID_NONE
        val parentPath = args.getString(ARG_PARENT_PATH) ?: ""
        val title = args.getString(ARG_TITLE) ?: ""
        val displayPath = args.getString(ARG_DISPLAY_PATH) ?: ""
        val slotId = mainViewModel.selectedSlot.value?.slotId ?: 0

        viewModel = ViewModelProvider(
            this,
            FileBrowserViewModel.Factory(requireActivity().application, rawResId, slotId, aid, parentPath, title)
        )[FileBrowserViewModel::class.java]

        setupUi(displayPath)
    }

    private fun setupUi(displayPath: String) {
        val adapter = FileEntryAdapter { entry ->
            val childPath = viewModel.parentPath + entry.id
            val childDisplayPath = FileBrowserViewModel.formatDisplayPath(childPath)
            val childTitle = FileBrowserViewModel.buildSubTitle(entry.name, entry.id, displayPath)

            findNavController().navigate(
                R.id.action_file_browser_to_sublevel,
                bundleOf(
                    ARG_RAW_RES_ID to viewModel.rawResId,
                    ARG_AID to viewModel.aid,
                    ARG_PARENT_PATH to childPath,
                    ARG_TITLE to childTitle,
                    ARG_DISPLAY_PATH to childDisplayPath
                )
            )
        }

        binding.recyclerView.apply {
            this.adapter = adapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.progressIndicator.isVisible = loading
                        binding.recyclerView.isVisible = !loading
                    }
                }
                launch {
                    viewModel.entries.collect { entries ->
                        adapter.submitList(entries)
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
        const val ARG_RAW_RES_ID = "rawResId"
        const val ARG_AID = "aid"
        const val ARG_PARENT_PATH = "parentPath"
        const val ARG_TITLE = "title"
        const val ARG_DISPLAY_PATH = "displayPath"
    }
}

private fun NavItem.rawResId(): Int = when (level) {
    NavLevel.MF -> R.raw.level_mf
    NavLevel.USIM -> R.raw.level_adf_usim
    NavLevel.ISIM -> R.raw.level_adf_isim
}

private fun NavItem.initialPath(): String = when (level) {
    NavLevel.MF -> FileId.PATH_MF
    NavLevel.USIM, NavLevel.ISIM -> FileId.PATH_ADF
}

private fun NavItem.displayPath(): String = when (level) {
    NavLevel.MF -> ""
    NavLevel.USIM, NavLevel.ISIM -> FileId.PATH_ADF
}
