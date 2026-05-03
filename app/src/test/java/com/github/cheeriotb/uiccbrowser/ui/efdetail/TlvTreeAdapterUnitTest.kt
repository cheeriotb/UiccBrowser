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

class TlvTreeAdapterUnitTest {

    private fun element(
        label: String = "",
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
        override fun toString() = label
    }

    @Test
    fun flatten_singleNode_returnsOneNodeAtDepthZero() {
        val root = element("root")
        val nodes = TlvTreeAdapter.flatten(root)
        assertThat(nodes).hasSize(1)
        assertThat(nodes[0].depth).isEqualTo(0)
        assertThat(nodes[0].element.label).isEqualTo("root")
    }

    @Test
    fun flatten_withOneChild_returnsTwoNodes() {
        val child = element("child")
        val root = element("root", primitive = false, children = listOf(child))
        val nodes = TlvTreeAdapter.flatten(root)
        assertThat(nodes).hasSize(2)
        assertThat(nodes[0].depth).isEqualTo(0)
        assertThat(nodes[1].depth).isEqualTo(1)
        assertThat(nodes[1].element.label).isEqualTo("child")
    }

    @Test
    fun flatten_withTwoLevels_correctDepths() {
        val grandchild = element("gc")
        val child = element("c", primitive = false, children = listOf(grandchild))
        val root = element("r", primitive = false, children = listOf(child))
        val nodes = TlvTreeAdapter.flatten(root)
        assertThat(nodes).hasSize(3)
        assertThat(nodes[0].depth).isEqualTo(0)
        assertThat(nodes[1].depth).isEqualTo(1)
        assertThat(nodes[2].depth).isEqualTo(2)
    }

    @Test
    fun flatten_withMultipleChildren_returnsInOrder() {
        val c1 = element("c1")
        val c2 = element("c2")
        val root = element("r", primitive = false, children = listOf(c1, c2))
        val nodes = TlvTreeAdapter.flatten(root)
        assertThat(nodes).hasSize(3)
        assertThat(nodes[1].element.label).isEqualTo("c1")
        assertThat(nodes[2].element.label).isEqualTo("c2")
    }

    @Test
    fun indentPx_depthZero_returnsZero() {
        assertThat(TlvTreeAdapter.indentPx(0, 160f)).isEqualTo(0)
    }

    @Test
    fun indentPx_depthOne_mdpi_returns16px() {
        assertThat(TlvTreeAdapter.indentPx(1, 160f)).isEqualTo(16)
    }

    @Test
    fun indentPx_depthTwo_xhdpi_returns64px() {
        assertThat(TlvTreeAdapter.indentPx(2, 320f)).isEqualTo(64)
    }
}
