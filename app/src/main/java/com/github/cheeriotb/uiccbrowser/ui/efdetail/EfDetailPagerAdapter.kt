/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui.efdetail

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class EfDetailPagerAdapter(
    fragment: Fragment,
    private val hasDecoder: Boolean
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = if (hasDecoder) 3 else 2

    override fun createFragment(position: Int): Fragment = when {
        position == 0 -> BinaryFragment()
        hasDecoder && position == 1 -> InfoFragment()
        else -> FcpFragment()
    }
}
