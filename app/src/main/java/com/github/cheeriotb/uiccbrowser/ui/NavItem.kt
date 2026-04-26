/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui

enum class NavLevel { MF, USIM, ISIM }

data class NavItem(
    val label: String,
    val iconResId: Int,
    val level: NavLevel,
    val aid: String? = null,   // null for MF; AID string needed later to SELECT the ADF
)
