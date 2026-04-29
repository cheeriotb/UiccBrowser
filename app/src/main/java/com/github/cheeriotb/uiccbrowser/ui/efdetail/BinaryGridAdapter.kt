/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui.efdetail

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.cheeriotb.uiccbrowser.databinding.ItemBinaryCellBinding

class BinaryGridAdapter : RecyclerView.Adapter<BinaryGridAdapter.ViewHolder>() {

    private var data: ByteArray = ByteArray(0)
    private lateinit var context: Context

    companion object {
        private const val VIEW_TYPE_OFFSET = 0
        private const val VIEW_TYPE_BYTE = 1

        /** Returns the total item count for a grid representing [dataSize] bytes. */
        fun itemCount(dataSize: Int): Int =
            if (dataSize == 0) 0 else ((dataSize + 7) / 8) * 9

        /** Formats [rowIndex] (0-based) as a 4-digit uppercase hex row offset. */
        fun formatOffset(rowIndex: Int): String = String.format("%04X", rowIndex * 8)

        /** Formats a single byte as a 2-digit uppercase hex string. */
        fun formatByte(b: Byte): String = String.format("%02X", b.toInt() and 0xFF)
    }

    class ViewHolder(val binding: ItemBinaryCellBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemCount(): Int = itemCount(data.size)

    override fun getItemViewType(position: Int): Int =
        if (position % 9 == 0) VIEW_TYPE_OFFSET else VIEW_TYPE_BYTE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        return ViewHolder(
            ItemBinaryCellBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = position / 9
        val col = position % 9
        holder.binding.text.text = if (col == 0) {
            formatOffset(row)
        } else {
            val byteIndex = row * 8 + col - 1
            if (byteIndex < data.size) formatByte(data[byteIndex]) else ""
        }
    }

    fun updateData(newData: ByteArray) {
        data = newData
        notifyDataSetChanged()
    }
}
