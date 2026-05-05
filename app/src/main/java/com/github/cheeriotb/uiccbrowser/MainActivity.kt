/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.navigation.NavigationView
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.github.cheeriotb.uiccbrowser.databinding.ActivityMainBinding
import com.github.cheeriotb.uiccbrowser.ui.MainViewModel
import com.github.cheeriotb.uiccbrowser.ui.NavLevel
import com.github.cheeriotb.uiccbrowser.ui.efdetail.EfDetailFragment
import com.github.cheeriotb.uiccbrowser.ui.filebrowser.FileBrowserFragment
import com.github.cheeriotb.uiccbrowser.ui.filebrowser.FileBrowserViewModel
import com.github.cheeriotb.uiccbrowser.usecase.GetFileListUseCase
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.loadAvailableSlots()
            } else {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.permission_denied_title)
                    .setMessage(R.string.permission_denied_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_file_browser), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        navView.setNavigationItemSelectedListener { menuItem ->
            val navItem = viewModel.navItems.value.getOrNull(menuItem.itemId)
            if (navItem != null) viewModel.selectNavItem(navItem)
            navController.navigate(
                R.id.nav_file_browser, null,
                NavOptions.Builder().setPopUpTo(R.id.nav_file_browser, true).build()
            )
            drawerLayout.closeDrawers()
            menuItem.isChecked = true
            true
        }

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            when (destination.id) {
                R.id.nav_file_browser -> {
                    val navItem = viewModel.selectedNavItem.value
                        ?: return@addOnDestinationChangedListener
                    val rawResId = when (navItem.level) {
                        NavLevel.MF   -> R.raw.level_mf
                        NavLevel.USIM -> R.raw.level_adf_usim
                        NavLevel.ISIM -> R.raw.level_adf_isim
                    }
                    val (name, id) = try {
                        GetFileListUseCase(this).parseRootMeta(rawResId)
                    } catch (_: Exception) {
                        navItem.label to ""
                    }
                    supportActionBar?.title = FileBrowserViewModel.buildRootTitle(name, id)
                }
                R.id.nav_file_sublevel -> {
                    supportActionBar?.title =
                        arguments?.getString(FileBrowserFragment.ARG_TITLE)
                }
                R.id.nav_ef_detail -> {
                    val efName = arguments?.getString(EfDetailFragment.ARG_EF_NAME) ?: ""
                    val efFileId = arguments?.getString(EfDetailFragment.ARG_EF_FILE_ID) ?: ""
                    supportActionBar?.title = "$efName ($efFileId)"
                }
            }
        }

        requestReadPhoneStatePermissionIfNeeded()

        val headerView = navView.getHeaderView(0)
        val slotIconViews = listOf(
            headerView.findViewById<ImageView>(R.id.imageView0),
            headerView.findViewById<ImageView>(R.id.imageView1),
            headerView.findViewById<ImageView>(R.id.imageView2)
        )
        val titleTextView = headerView.findViewById<TextView>(R.id.navHeaderTitle)
        val subtitleTextView = headerView.findViewById<TextView>(R.id.navHeaderSubtitle)
        var isUpdatingProModeSwitch = false

        fun setProModeSwitchChecked(isChecked: Boolean) {
            isUpdatingProModeSwitch = true
            binding.proModeSwitch.isChecked = isChecked
            isUpdatingProModeSwitch = false
        }

        binding.proModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingProModeSwitch) return@setOnCheckedChangeListener

            if (!isChecked) {
                viewModel.setProModeEnabled(false)
                return@setOnCheckedChangeListener
            }

            if (viewModel.selectedSlot.value == null) {
                setProModeSwitchChecked(false)
                return@setOnCheckedChangeListener
            }
            val requestedSlotId = viewModel.selectedSlot.value!!.slotId

            fun resetRequestedSlotSwitchIfCurrent() {
                viewModel.setProModeEnabled(requestedSlotId, false)
                if (viewModel.selectedSlot.value?.slotId == requestedSlotId) {
                    setProModeSwitchChecked(false)
                }
            }

            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pro_mode_dialog_title)
                .setMessage(R.string.pro_mode_dialog_message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    viewModel.setProModeEnabled(requestedSlotId, true)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    resetRequestedSlotSwitchIfCurrent()
                }
                .setOnCancelListener {
                    resetRequestedSlotSwitchIfCurrent()
                }
                .show()
        }

        slotIconViews.forEachIndexed { index, iv ->
            iv.setOnClickListener { viewModel.selectSlot(index) }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.availableSlots,
                    viewModel.selectedSlot
                ) { slots, selected ->
                    MainViewModel.buildSlotIconStates(slots, selected?.slotId)
                }.collect { states ->
                    states.forEachIndexed { i, state ->
                        slotIconViews[i].visibility =
                            if (state.visible) View.VISIBLE else View.INVISIBLE
                        slotIconViews[i].setImageResource(
                            if (state.selected) R.mipmap.ic_selected_card_round
                            else R.mipmap.ic_card_round)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedSlot.collect { slotInfo ->
                    titleTextView.text = if (slotInfo != null) {
                        getString(R.string.iccid_label, slotInfo.iccId)
                    } else {
                        getString(R.string.nav_header_no_sim)
                    }
                    subtitleTextView.text = if (slotInfo != null) {
                        getString(R.string.slot_label, slotInfo.slotId)
                    } else {
                        getString(R.string.nav_header_no_slot)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.selectedSlot,
                    viewModel.isProModeEnabled
                ) { slotInfo, isProModeEnabled ->
                    slotInfo to isProModeEnabled
                }.collect { (slotInfo, isProModeEnabled) ->
                    binding.proModeSwitch.isEnabled = slotInfo != null
                    setProModeSwitchChecked(slotInfo != null && isProModeEnabled)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isCachingMf.collect { isCaching ->
                    binding.appBarMain.progressIndicator.visibility =
                        if (isCaching) View.VISIBLE else View.GONE
                }
            }
        }

        lifecycleScope.launch {
            viewModel.navItems.collect { navItems ->
                val menu = navView.menu
                menu.clear()
                navItems.forEachIndexed { index, item ->
                    menu.add(R.id.nav_group_main, index, Menu.NONE, item.label)
                        .setIcon(item.iconResId)
                }
                menu.setGroupCheckable(R.id.nav_group_main, true, true)
                if (navItems.isNotEmpty()) {
                    val selectedIndex = viewModel.selectedNavItem.value
                        ?.let { navItems.indexOf(it) } ?: -1
                    if (selectedIndex >= 0) {
                        menu.getItem(selectedIndex).isChecked = true
                    } else {
                        menu.getItem(0).isChecked = true
                        viewModel.selectNavItem(navItems.first())
                        navController.navigate(
                            R.id.nav_file_browser, null,
                            NavOptions.Builder().setPopUpTo(R.id.nav_file_browser, true).build()
                        )
                        drawerLayout.closeDrawers()
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun requestReadPhoneStatePermissionIfNeeded() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.loadAvailableSlots()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_STATE) -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.permission_rationale_title)
                    .setMessage(R.string.permission_rationale_message)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            }
        }
    }
}
