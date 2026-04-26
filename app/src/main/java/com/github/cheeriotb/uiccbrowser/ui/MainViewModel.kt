/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.cheeriotb.uiccbrowser.R
import com.github.cheeriotb.uiccbrowser.usecase.CacheFileControlParametersUseCase
import com.github.cheeriotb.uiccbrowser.usecase.GetAvailableCardsUseCase
import com.github.cheeriotb.uiccbrowser.usecase.CardInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val getAvailableSlots = GetAvailableCardsUseCase(application.applicationContext)
    private val cacheFiles = CacheFileControlParametersUseCase(application.applicationContext)

    private val _availableSlots = MutableStateFlow<List<CardInfo>>(emptyList())
    val availableSlots: StateFlow<List<CardInfo>> = _availableSlots.asStateFlow()

    private val _selectedSlot = MutableStateFlow<CardInfo?>(null)
    val selectedSlot: StateFlow<CardInfo?> = _selectedSlot.asStateFlow()

    private val _isCachingMf = MutableStateFlow(false)
    val isCachingMf: StateFlow<Boolean> = _isCachingMf.asStateFlow()

    fun loadAvailableSlots() {
        if (_availableSlots.value.isNotEmpty()) return
        viewModelScope.launch {
            val slots = getAvailableSlots.execute()
            _availableSlots.value = slots
            if (slots.isNotEmpty() && _selectedSlot.value == null) {
                val first = slots.first()
                _selectedSlot.value = first
                startMfCaching(first)
            }
        }
    }

    fun selectSlot(slotId: Int) {
        val slotInfo = _availableSlots.value.find { it.slotId == slotId } ?: return
        _selectedSlot.value = slotInfo
        startMfCaching(slotInfo)
    }

    private fun startMfCaching(cardInfo: CardInfo) {
        viewModelScope.launch {
            _isCachingMf.value = true
            cacheFiles.execute(R.raw.level_mf, cardInfo.slotId)
            _isCachingMf.value = false
        }
    }
}
