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
import com.github.cheeriotb.uiccbrowser.usecase.GetAvailableSlotsUseCase
import com.github.cheeriotb.uiccbrowser.usecase.SlotInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val getAvailableSlots = GetAvailableSlotsUseCase(application.applicationContext)

    private val _availableSlots = MutableStateFlow<List<SlotInfo>>(emptyList())
    val availableSlots: StateFlow<List<SlotInfo>> = _availableSlots.asStateFlow()

    private val _selectedSlot = MutableStateFlow<SlotInfo?>(null)
    val selectedSlot: StateFlow<SlotInfo?> = _selectedSlot.asStateFlow()

    fun loadAvailableSlots() {
        if (_availableSlots.value.isNotEmpty()) return
        viewModelScope.launch {
            val slots = getAvailableSlots.execute()
            _availableSlots.value = slots
            if (slots.isNotEmpty() && _selectedSlot.value == null) {
                _selectedSlot.value = slots.first()
            }
        }
    }

    fun selectSlot(slotId: Int) {
        _selectedSlot.value = _availableSlots.value.find { it.slotId == slotId }
    }
}
