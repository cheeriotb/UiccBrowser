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
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.github.cheeriotb.uiccbrowser.databinding.ActivityMainBinding
import com.github.cheeriotb.uiccbrowser.ui.MainViewModel
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
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        requestReadPhoneStatePermissionIfNeeded()

        val headerView = navView.getHeaderView(0)
        val imageView = headerView.findViewById<ImageView>(R.id.imageView)
        val titleTextView = headerView.findViewById<TextView>(R.id.navHeaderTitle)
        val subtitleTextView = headerView.findViewById<TextView>(R.id.navHeaderSubtitle)

        imageView.setOnClickListener { showSlotSelectionDialog() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedSlot.collect { slotInfo ->
                    titleTextView.text = slotInfo?.iccId ?: getString(R.string.nav_header_no_sim)
                    subtitleTextView.text = if (slotInfo != null) {
                        getString(R.string.slot_label, slotInfo.slotId)
                    } else {
                        ""
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun showSlotSelectionDialog() {
        val slots = viewModel.availableSlots.value
        if (slots.isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.slot_dialog_title)
                .setMessage(R.string.nav_header_no_sim)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }
        val labels = slots.map { it.iccId }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.slot_dialog_title)
            .setItems(labels) { _, which -> viewModel.selectSlot(slots[which].slotId) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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
