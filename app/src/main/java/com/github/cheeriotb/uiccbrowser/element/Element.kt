/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.element

interface Element {
    // True if this element itself does not consist of sub-element(s), false if constructed.
    val primitive: Boolean
    // This byte array is a primitive data or constructed from sub-element(s).
    val data: ByteArray
    // Contains sub-element(s) if this is not an element for a primitive data.
    val subElements: List<Element>
    // The root element of this element tree structure.
    val rootElement: Element
    // True if this element is editable independently.
    val editable: Boolean

    companion object {
        const val NO_ID_SPECIFIED = -1
    }

    val labelId: Int
    val descriptionId: Int

    fun setData(newData: ByteArray): Boolean
    fun toByteArray(): ByteArray
}
