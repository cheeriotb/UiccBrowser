/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui.efdetail

import android.content.res.Resources
import com.github.cheeriotb.uiccbrowser.element.Element
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EfDetailClipboardFormatterUnitTest {

    @Test
    fun format_binaryData_buildsSixteenByteMarkdownRows() {
        val data = ByteArray(20) { it.toByte() }

        val result = EfDetailClipboardFormatter.format(data, null)

        assertThat(result).contains(
            "|DATA|00|01|02|03|04|05|06|07|08|09|0A|0B|0C|0D|0E|0F|"
        )
        assertThat(result).contains(
            "|0000|00|01|02|03|04|05|06|07|08|09|0A|0B|0C|0D|0E|0F|"
        )
        assertThat(result).contains(
            "|0010|10|11|12|13| | | | | | | | | | | | |"
        )
    }

    @Test
    fun format_withoutInfo_omitsInfoSection() {
        val result = EfDetailClipboardFormatter.format(byteArrayOf(0x00), null)

        assertThat(result).startsWith("## Binary")
        assertThat(result).doesNotContain("Record:")
        assertThat(result).doesNotContain("## Info")
    }

    @Test
    fun format_withRecordNumber_addsRecordNumberToBinarySection() {
        val result = EfDetailClipboardFormatter.format(
            byteArrayOf(0x00),
            info = null,
            recordNumber = 12
        )

        assertThat(result).startsWith("## Binary\n\nRecord: 12\n\n|DATA|")
    }

    @Test
    fun format_withInfo_buildsIndentedInformationTree() {
        val grandchild = element("Grandchild", value = "Value")
        val child = element("Child", primitive = false, children = listOf(grandchild))
        val root = element("Root", primitive = false, children = listOf(child))

        val result = EfDetailClipboardFormatter.format(byteArrayOf(0x00), root)
        val infoLines = result.substringAfter("## Info\n\n").lines()

        assertThat(infoLines).containsExactly(
            "- Root",
            "  - Child",
            "    - Grandchild: Value"
        ).inOrder()
    }

    private fun element(
        label: String,
        value: String = label,
        primitive: Boolean = true,
        children: List<Element> = emptyList()
    ): Element = object : Element {
        override val primitive = primitive
        override val data = byteArrayOf()
        override val subElements = children
        override val rootElement: Element get() = this
        override val editable = false
        override val label = label
        override val byteArray = byteArrayOf()
        override fun setData(resources: Resources, newData: ByteArray) = false
        override fun toString() = value
    }
}
