/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.usecase

import android.content.Context
import com.github.cheeriotb.uiccbrowser.repository.CardRepository

class GetAvailableCardsUseCase(private val context: Context) {

    /**
     * Returns a list of CardInfo for slots where a SIM card is accessible.
     * Enumerates slots by incrementing slotId until CardRepository.from() returns null,
     * then calls initialize() on each to confirm a SIM card is present and readable.
     */
    suspend fun execute(): List<CardInfo> {
        val available = mutableListOf<CardInfo>()
        var slotId = 0
        while (true) {
            val repo = CardRepository.from(context, slotId) ?: break
            if (repo.initialize()) available.add(CardInfo(slotId, repo.iccId!!))
            slotId++
        }
        return available
    }
}
