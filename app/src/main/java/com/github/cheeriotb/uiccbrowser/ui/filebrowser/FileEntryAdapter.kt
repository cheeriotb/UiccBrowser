/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui.filebrowser

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.cheeriotb.uiccbrowser.R
import com.github.cheeriotb.uiccbrowser.databinding.ItemFileEntryBinding
import com.github.cheeriotb.uiccbrowser.usecase.FileEntry

class FileEntryAdapter(
    private val onDirectoryClick: (FileEntry) -> Unit,
    private val onFileClick: (FileEntry) -> Unit
) : ListAdapter<FileEntry, FileEntryAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(val binding: ItemFileEntryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFileEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = getItem(position)
        with(holder.binding) {
            icon.setImageResource(
                if (entry.isDirectory) R.drawable.folder_dedicated else R.drawable.file)
            mainText.text = "${entry.name} (${entry.id})"
            subText.text = entry.description
            root.setOnClickListener {
                if (entry.isDirectory) onDirectoryClick(entry) else onFileClick(entry)
            }
            root.isClickable = true
            root.isFocusable = true
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<FileEntry>() {
        override fun areItemsTheSame(oldItem: FileEntry, newItem: FileEntry): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: FileEntry, newItem: FileEntry): Boolean =
            oldItem == newItem
    }
}
