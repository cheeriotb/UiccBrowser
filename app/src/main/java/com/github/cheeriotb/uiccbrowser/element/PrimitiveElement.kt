/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.element

import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString

class PrimitiveElement private constructor(
    private var primitiveData: ByteArray,
    override val editable: Boolean,
    override val labelId: Int,
    override val descriptionId: Int,
    private val parent: Element?,
    private val validator: (ByteArray) -> Boolean,
    private val interpreter: (ByteArray) -> String
) : Element {

    class Builder(
        private var primitiveData: ByteArray,
        private var editable: Boolean = false,
        private var labelId: Int = Element.NO_ID_SPECIFIED,
        private var descriptionId: Int = Element.NO_ID_SPECIFIED,
        private var parent: Element? = null,
        private var validator: (ByteArray) -> Boolean = { true },
        private var interpreter: (ByteArray) -> String = { byteArrayToHexString(it) }
    ) {
        fun editable(editable: Boolean) = also { it.editable = editable }
        fun labelId(labelId: Int) = also { it.labelId = labelId }
        fun descriptionId(descriptionId: Int) = also { it.descriptionId = descriptionId }
        fun parent(parent: Element?) = also { it.parent = parent }
        fun validator(validator: (ByteArray) -> Boolean) = also { it.validator = validator }
        fun interpreter(interpreter: (ByteArray) -> String) = also { it.interpreter = interpreter }
        fun build() = PrimitiveElement(primitiveData, editable, labelId, descriptionId, parent,
                validator, interpreter)
    }

    override val primitive: Boolean = true

    override val data: ByteArray
        get() = primitiveData

    override val subElements: List<Element> = listOf()

    override val rootElement: Element
        get() {
            return parent?.rootElement ?: this
        }

    override fun setData(newData: ByteArray): Boolean {
        if (!editable || !validator(newData)) return false

        val backup = primitiveData
        primitiveData = newData
        if (newData.size > backup.size) {
            if (rootElement.toByteArray().isEmpty()) {
                primitiveData = backup
                return false
            }
        }
        return true
    }

    // Returns a byte array of the already validated primitive data
    override fun toByteArray(): ByteArray {
        if (!validator(data)) return byteArrayOf()
        return primitiveData
    }

    override fun toString(): String = interpreter(primitiveData)
}
