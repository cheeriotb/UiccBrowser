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
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.cheeriotb.uiccbrowser.R
import com.github.cheeriotb.uiccbrowser.databinding.FragmentEfDetailBinding
import com.github.cheeriotb.uiccbrowser.repository.FileId
import com.google.android.material.tabs.TabLayoutMediator

class EfDetailFragment : Fragment() {

    private var _binding: FragmentEfDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: EfDetailViewModel

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

        binding.viewPager.adapter = EfDetailPagerAdapter(this)

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = "Binary"
                    tab.icon = AppCompatResources.getDrawable(requireContext(), R.drawable.binary)
                }
                1 -> {
                    tab.text = "Info"
                    tab.icon = AppCompatResources.getDrawable(requireContext(), R.drawable.info)
                }
                else -> {
                    tab.text = "FCP"
                    tab.icon = AppCompatResources.getDrawable(requireContext(), R.drawable.fcp)
                }
            }
        }.attach()

        if (!viewModel.hasDecoder) {
            (binding.tabLayout.getChildAt(0) as? ViewGroup)?.getChildAt(1)?.let {
                it.isEnabled = false
                it.alpha = 0.38f
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
        const val ARG_EF_NAME = "efName"
        const val ARG_EF_FILE_ID = "efFileId"
        const val ARG_AID = "aid"
        const val ARG_PARENT_PATH = "parentPath"
    }
}
