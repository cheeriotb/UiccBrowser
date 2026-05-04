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
import com.github.cheeriotb.uiccbrowser.databinding.ItemTlvNodeBinding
import com.github.cheeriotb.uiccbrowser.element.Element

class TlvTreeAdapter : RecyclerView.Adapter<TlvTreeAdapter.ViewHolder>() {

    data class Node(val element: Element, val depth: Int)

    private var nodes: List<Node> = emptyList()

    fun updateTree(root: Element) {
        nodes = flatten(root)
        notifyDataSetChanged()
    }

    companion object {
        fun flatten(element: Element, depth: Int = 0): List<Node> {
            val result = mutableListOf(Node(element, depth))
            element.subElements.forEach { result += flatten(it, depth + 1) }
            return result
        }

        fun indentPx(depth: Int, densityDpi: Float): Int =
            (depth * 16 * densityDpi / 160f + 0.5f).toInt()
    }

    class ViewHolder(val binding: ItemTlvNodeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTlvNodeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val node = nodes[position]
        val context = holder.itemView.context

        val params = holder.binding.root.layoutParams as ViewGroup.MarginLayoutParams
        params.leftMargin = indentPx(node.depth, context.resources.displayMetrics.densityDpi.toFloat())
        holder.binding.root.layoutParams = params

        holder.binding.labelText.text = node.element.label
        holder.binding.valueText.text =
            if (node.element.primitive) node.element.toString() else ""
    }

    override fun getItemCount(): Int = nodes.size
}
