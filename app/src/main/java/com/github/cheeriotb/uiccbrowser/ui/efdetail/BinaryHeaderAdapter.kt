/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui.efdetail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.cheeriotb.uiccbrowser.databinding.ItemBinaryHeaderBinding

class BinaryHeaderAdapter : RecyclerView.Adapter<BinaryHeaderAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemBinaryHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    // Position 0: blank offset-column placeholder (2 spans); positions 1-8: column offsets "00"-"07"
    override fun getItemCount(): Int = 9

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ItemBinaryHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.text.text = if (position == 0) "" else String.format("%02X", position - 1)
    }
}
