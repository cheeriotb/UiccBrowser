/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui

import android.app.Application
import android.content.res.Resources
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.cheeriotb.uiccbrowser.R
import com.github.cheeriotb.uiccbrowser.element.ef.AppTemplate
import com.github.cheeriotb.uiccbrowser.repository.CardRepository
import com.github.cheeriotb.uiccbrowser.usecase.CacheFileControlParametersUseCase
import com.github.cheeriotb.uiccbrowser.usecase.GetAvailableCardsUseCase
import com.github.cheeriotb.uiccbrowser.usecase.CardInfo
import com.github.cheeriotb.uiccbrowser.usecase.ReadApplicationsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SlotIconState(val visible: Boolean, val selected: Boolean)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val getAvailableSlots = GetAvailableCardsUseCase(application.applicationContext)
    private val cacheFiles = CacheFileControlParametersUseCase(application.applicationContext)
    private val readApplications = ReadApplicationsUseCase(application.applicationContext)

    private val _availableSlots = MutableStateFlow<List<CardInfo>>(emptyList())
    val availableSlots: StateFlow<List<CardInfo>> = _availableSlots.asStateFlow()

    private val _selectedSlot = MutableStateFlow<CardInfo?>(null)
    val selectedSlot: StateFlow<CardInfo?> = _selectedSlot.asStateFlow()

    private val _isProModeEnabled = MutableStateFlow(false)
    val isProModeEnabled: StateFlow<Boolean> = _isProModeEnabled.asStateFlow()

    private val _isCachingMf = MutableStateFlow(false)
    val isCachingMf: StateFlow<Boolean> = _isCachingMf.asStateFlow()

    private val _navItems = MutableStateFlow<List<NavItem>>(emptyList())
    val navItems: StateFlow<List<NavItem>> = _navItems.asStateFlow()

    private val _selectedNavItem = MutableStateFlow<NavItem?>(null)
    val selectedNavItem: StateFlow<NavItem?> = _selectedNavItem.asStateFlow()

    fun selectNavItem(item: NavItem) {
        _selectedNavItem.value = item
    }

    fun loadAvailableSlots() {
        if (_availableSlots.value.isNotEmpty()) return
        viewModelScope.launch {
            val slots = getAvailableSlots.execute()
            _availableSlots.value = slots
            if (slots.isNotEmpty() && _selectedSlot.value == null) {
                val first = slots.first()
                _isProModeEnabled.value = proModeEnabledFor(first.slotId)
                _selectedSlot.value = first
                startMfCaching(first)
            }
        }
    }

    fun selectSlot(slotId: Int) {
        val slotInfo = _availableSlots.value.find { it.slotId == slotId } ?: return
        _isProModeEnabled.value = proModeEnabledFor(slotId)
        _selectedSlot.value = slotInfo
        startMfCaching(slotInfo)
    }

    fun setProModeEnabled(enabled: Boolean) {
        val slotId = _selectedSlot.value?.slotId ?: return
        setProModeEnabled(slotId, enabled)
    }

    fun setProModeEnabled(slotId: Int, enabled: Boolean) {
        CardRepository.from(getApplication<Application>().applicationContext, slotId)
            ?.isProModeEnabled = enabled
        if (_selectedSlot.value?.slotId == slotId) {
            _isProModeEnabled.value = proModeEnabledFor(slotId)
        }
    }

    private fun proModeEnabledFor(slotId: Int): Boolean =
        CardRepository.from(getApplication<Application>().applicationContext, slotId)
            ?.isProModeEnabled ?: false

    private fun startMfCaching(cardInfo: CardInfo) {
        viewModelScope.launch {
            _isCachingMf.value = true
            _navItems.value = emptyList()
            cacheFiles.execute(R.raw.level_mf, cardInfo.slotId)
            val aids = readApplications.execute(cardInfo.slotId)
            _navItems.value = buildNavItems(getApplication<Application>().resources, aids)
            _selectedNavItem.value = _navItems.value.firstOrNull()
            _isCachingMf.value = false
        }
    }

    companion object {
        /**
         * Converts a list of AID strings from EF DIR into navigation items.
         *
         * Always prepends an MF item. AIDs starting with 3GPP RID + USIM/ISIM app code are
         * categorised accordingly; other AIDs are ignored. When more than one item of the same
         * type is present each is given a numeric suffix ("USIM 1", "USIM 2", …).
         */
        internal fun buildNavItems(resources: Resources, aids: List<String>): List<NavItem> {
            val items = mutableListOf<NavItem>()

            items.add(NavItem(
                label = resources.getString(R.string.nav_item_mf),
                iconResId = R.drawable.ic_folder,
                level = NavLevel.MF,
            ))

            val usimPrefix = (AppTemplate.RID + AppTemplate.APP_USIM).uppercase()
            val isimPrefix = (AppTemplate.RID + AppTemplate.APP_ISIM).uppercase()

            val usimAids = aids.filter { it.uppercase().startsWith(usimPrefix) }
            val isimAids = aids.filter { it.uppercase().startsWith(isimPrefix) }

            usimAids.forEachIndexed { index, aid ->
                val label = if (usimAids.size == 1) resources.getString(R.string.nav_item_usim)
                            else resources.getString(R.string.nav_item_usim_numbered, index + 1)
                items.add(NavItem(
                    label = label,
                    iconResId = R.drawable.ic_folder_usim,
                    level = NavLevel.USIM,
                    aid = aid,
                ))
            }

            isimAids.forEachIndexed { index, aid ->
                val label = if (isimAids.size == 1) resources.getString(R.string.nav_item_isim)
                            else resources.getString(R.string.nav_item_isim_numbered, index + 1)
                items.add(NavItem(
                    label = label,
                    iconResId = R.drawable.ic_folder_isim,
                    level = NavLevel.ISIM,
                    aid = aid,
                ))
            }

            return items
        }

        /**
         * Returns visibility and selection state for each slot icon (indices 0..maxSlots-1).
         *
         * An icon is visible when a CardInfo with a matching slotId exists in availableSlots.
         * An icon is selected when its index equals selectedSlotId.
         */
        internal fun buildSlotIconStates(
            availableSlots: List<CardInfo>,
            selectedSlotId: Int?,
            maxSlots: Int = 3
        ): List<SlotIconState> = (0 until maxSlots).map { i ->
            SlotIconState(
                visible  = availableSlots.any { it.slotId == i },
                selected = selectedSlotId == i
            )
        }
    }
}
