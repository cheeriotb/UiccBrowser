/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui.efdetail

import com.github.cheeriotb.uiccbrowser.element.Element

object EfDetailClipboardFormatter {

    /**
     * Builds Markdown containing the binary table, optional record number, and information tree.
     */
    fun format(data: ByteArray, info: Element?, recordNumber: Int? = null): String = buildString {
        appendLine("## Binary")
        appendLine()
        if (recordNumber != null) {
            appendLine("Record: $recordNumber")
            appendLine()
        }
        appendLine(binaryHeader())
        appendLine(binarySeparator())
        data.asList().chunked(BYTES_PER_ROW).forEachIndexed { rowIndex, row ->
            appendLine(binaryRow(rowIndex * BYTES_PER_ROW, row))
        }

        if (info != null) {
            appendLine()
            appendLine("## Info")
            appendLine()
            TlvTreeAdapter.flatten(info).forEach { node ->
                append(INFO_INDENT.repeat(node.depth))
                append("- ")
                append(node.element.label)
                if (node.element.primitive && node.element.subElements.isEmpty()) {
                    append(": ")
                    append(node.element.toString())
                }
                appendLine()
            }
        }
    }.trimEnd()

    private fun binaryHeader(): String =
        (listOf("DATA") + (0 until BYTES_PER_ROW).map { "%02X".format(it) })
            .joinToString("|", prefix = "|", postfix = "|")

    private fun binarySeparator(): String =
        List(BYTES_PER_ROW + 1) { "---" }
            .joinToString("|", prefix = "|", postfix = "|")

    private fun binaryRow(offset: Int, data: List<Byte>): String =
        (listOf("%04X".format(offset)) + data.map { "%02X".format(it.toInt() and 0xFF) }
                + List(BYTES_PER_ROW - data.size) { " " })
            .joinToString("|", prefix = "|", postfix = "|")

    private const val BYTES_PER_ROW = 16
    private const val INFO_INDENT = "  "
}
