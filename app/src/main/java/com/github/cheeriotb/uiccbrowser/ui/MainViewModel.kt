/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui

import android.app.Application
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.telephony.SubscriptionManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.cheeriotb.uiccbrowser.R
import com.github.cheeriotb.uiccbrowser.element.ef.AppTemplate
import com.github.cheeriotb.uiccbrowser.repository.CardRepository
import com.github.cheeriotb.uiccbrowser.usecase.CacheFileControlParametersUseCase
import com.github.cheeriotb.uiccbrowser.usecase.GetAvailableCardsUseCase
import com.github.cheeriotb.uiccbrowser.usecase.CardInfo
import com.github.cheeriotb.uiccbrowser.usecase.ReadApplicationsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SlotIconState(val visible: Boolean, val selected: Boolean)

internal data class SlotRefreshDecision(
    val selectedSlot: CardInfo?,
    val selectedSimUnavailable: Boolean
)

private data class SlotRefreshResult(
    val slots: List<CardInfo>,
    val selectedSimUnavailable: Boolean
)

internal data class SubscriptionSnapshot(
    val slotId: Int,
    val subscriptionId: Int,
    val iccId: String
)

internal enum class MainEvent {
    SELECTED_SIM_UNAVAILABLE
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val getAvailableSlots = GetAvailableCardsUseCase(application.applicationContext)
    private val cacheFiles = CacheFileControlParametersUseCase(application.applicationContext)
    private val readApplications = ReadApplicationsUseCase(application.applicationContext)
    private val subscriptionManager =
        application.applicationContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                as SubscriptionManager?

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

    private val _events = MutableSharedFlow<MainEvent>()
    internal val events: SharedFlow<MainEvent> = _events.asSharedFlow()

    private var subscriptionListener: SubscriptionManager.OnSubscriptionsChangedListener? = null
    private var lastSubscriptionSnapshot: List<SubscriptionSnapshot>? = null
    private var refreshJob: Job? = null
    private var cachingJob: Job? = null

    fun selectNavItem(item: NavItem) {
        releaseSelectedSlotChannel()
        _selectedNavItem.value = item
    }

    fun loadAvailableSlots() {
        if (_availableSlots.value.isNotEmpty()) return
        refreshAvailableSlotsNow(forceRefresh = false, notifySelectedUnavailable = false)
    }

    /**
     * Starts listening for Android subscription changes until this ViewModel is cleared.
     */
    fun startSubscriptionMonitoring() {
        if (subscriptionListener != null) return
        val manager = subscriptionManager ?: return
        lastSubscriptionSnapshot = currentSubscriptionSnapshot()
        val listener = object : SubscriptionManager.OnSubscriptionsChangedListener() {
            override fun onSubscriptionsChanged() {
                handleSubscriptionsChanged()
            }
        }

        manager.addOnSubscriptionsChangedListener(
            getApplication<Application>().applicationContext.mainExecutor,
            listener
        )
        subscriptionListener = listener
    }

    /**
     * Stops subscription monitoring and is safe to call repeatedly.
     */
    fun stopSubscriptionMonitoring() {
        val listener = subscriptionListener ?: return
        subscriptionManager?.removeOnSubscriptionsChangedListener(listener)
        subscriptionListener = null
        lastSubscriptionSnapshot = null
    }

    private fun refreshAvailableSlotsAfterSubscriptionChanged(
        expectedSubscriptions: List<SubscriptionSnapshot>
    ) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            var retriesLeft = SUBSCRIPTION_REFRESH_RETRY_COUNT
            var delayMillis = SUBSCRIPTION_REFRESH_DELAY_MILLIS
            do {
                if (delayMillis > 0L) delay(delayMillis)
                val result = refreshSlotsInProgress(forceRefresh = true)
                val shouldRetry = retriesLeft > 0
                    && shouldRetrySubscriptionRefresh(expectedSubscriptions, result.slots)
                if (!shouldRetry) {
                    if (result.selectedSimUnavailable) {
                        _events.emit(MainEvent.SELECTED_SIM_UNAVAILABLE)
                    }
                    return@launch
                }
                retriesLeft--
                delayMillis = SUBSCRIPTION_REFRESH_RETRY_MILLIS
            } while (true)
        }
    }

    private fun refreshAvailableSlotsNow(
        forceRefresh: Boolean,
        notifySelectedUnavailable: Boolean
    ) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val result = refreshSlotsInProgress(forceRefresh)
            if (result.selectedSimUnavailable && notifySelectedUnavailable) {
                _events.emit(MainEvent.SELECTED_SIM_UNAVAILABLE)
            }
        }
    }

    fun selectSlot(slotId: Int) {
        val slotInfo = _availableSlots.value.find { it.slotId == slotId } ?: return
        releaseSelectedSlotChannel()
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

    private fun releaseSelectedSlotChannel() {
        val slotId = _selectedSlot.value?.slotId ?: return
        CardRepository.from(getApplication<Application>().applicationContext, slotId)
            ?.releaseLogicalChannel()
    }

    private fun handleSubscriptionsChanged() {
        val snapshot = currentSubscriptionSnapshot()
        if (snapshot == lastSubscriptionSnapshot) return
        lastSubscriptionSnapshot = snapshot
        refreshAvailableSlotsAfterSubscriptionChanged(snapshot)
    }

    @SuppressLint("MissingPermission")
    private fun currentSubscriptionSnapshot(): List<SubscriptionSnapshot> =
        subscriptionManager?.activeSubscriptionInfoList.orEmpty()
            .map {
                SubscriptionSnapshot(
                    slotId = it.simSlotIndex,
                    subscriptionId = it.subscriptionId,
                    iccId = it.iccId.orEmpty()
                )
            }
            .sortedWith(compareBy(
                SubscriptionSnapshot::slotId,
                SubscriptionSnapshot::subscriptionId,
                SubscriptionSnapshot::iccId
            ))

    private suspend fun refreshSlotsInProgress(forceRefresh: Boolean): SlotRefreshResult {
        val previousSlot = _selectedSlot.value
        val previousProMode = _isProModeEnabled.value
        val slots = getAvailableSlots.execute(forceRefresh)
        val decision = buildSlotRefreshDecision(previousSlot, slots)

        _availableSlots.value = slots

        when (val selectedSlot = decision.selectedSlot) {
            null -> clearSelectedState()
            previousSlot -> {
                _selectedSlot.value = selectedSlot
                setProModeEnabled(selectedSlot.slotId, previousProMode)
                if (_navItems.value.isEmpty()) startMfCaching(selectedSlot)
            }
            else -> {
                _isProModeEnabled.value = proModeEnabledFor(selectedSlot.slotId)
                _selectedSlot.value = selectedSlot
                startMfCaching(selectedSlot)
            }
        }

        return SlotRefreshResult(slots, decision.selectedSimUnavailable)
    }

    private fun startMfCaching(cardInfo: CardInfo) {
        cachingJob?.cancel()
        cachingJob = viewModelScope.launch {
            _isCachingMf.value = true
            _navItems.value = emptyList()
            try {
                cacheFiles.execute(R.raw.level_mf, cardInfo.slotId)
                val aids = readApplications.execute(cardInfo.slotId)
                if (_selectedSlot.value == cardInfo) {
                    _navItems.value = buildNavItems(getApplication<Application>().resources, aids)
                    _selectedNavItem.value = _navItems.value.firstOrNull()
                }
            } finally {
                if (_selectedSlot.value == cardInfo) {
                    _isCachingMf.value = false
                }
            }
        }
    }

    private fun clearSelectedState() {
        cachingJob?.cancel()
        _selectedSlot.value = null
        _isProModeEnabled.value = false
        _isCachingMf.value = false
        _navItems.value = emptyList()
        _selectedNavItem.value = null
    }

    override fun onCleared() {
        stopSubscriptionMonitoring()
        super.onCleared()
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

        /**
         * Selects the next slot after a subscription refresh.
         *
         * The previous selection is retained only when both slot ID and ICCID still match.
         */
        internal fun buildSlotRefreshDecision(
            previousSlot: CardInfo?,
            availableSlots: List<CardInfo>
        ): SlotRefreshDecision {
            val retainedSlot = previousSlot?.let { previous ->
                availableSlots.find {
                    it.slotId == previous.slotId && it.iccId == previous.iccId
                }
            }
            return SlotRefreshDecision(
                selectedSlot = retainedSlot ?: availableSlots.firstOrNull(),
                selectedSimUnavailable = previousSlot != null && retainedSlot == null
            )
        }

        internal fun shouldRetrySubscriptionRefresh(
            expectedSubscriptions: List<SubscriptionSnapshot>,
            availableSlots: List<CardInfo>
        ): Boolean {
            val expectedSlotIds = expectedSubscriptions
                .map { it.slotId }
                .filter { it >= 0 }
                .toSet()
            val availableSlotIds = availableSlots.map { it.slotId }.toSet()
            return expectedSlotIds.isNotEmpty() && !availableSlotIds.containsAll(expectedSlotIds)
        }

        private const val SUBSCRIPTION_REFRESH_DELAY_MILLIS = 1_000L
        private const val SUBSCRIPTION_REFRESH_RETRY_MILLIS = 1_000L
        private const val SUBSCRIPTION_REFRESH_RETRY_COUNT = 5
    }
}
