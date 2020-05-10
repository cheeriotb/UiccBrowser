/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.element

import kotlin.text.StringBuilder

class ConstructedElement private constructor(
    rawData: ByteArray,
    override val editable: Boolean,
    override val labelId: Int,
    override val descriptionId: Int,
    private val parent: Element?,
    private val decoder: (ByteArray, Element?) -> List<Element>,
    private val validator: (ByteArray) -> Boolean,
    private val interpreter: (ByteArray) -> String
) : Element {
    private var elementList: List<Element> = decoder(rawData, this)

    companion object {
        fun defaultDecoder(rawData: ByteArray, parent: Element?): List<Element> {
            val element = PrimitiveElement.Builder(rawData)
                    .parent(parent)
                    .validator { false }
                    .build()
            return listOf(element)
        }

        const val STRING_SEPARATOR = ", "
        const val NO_INTERPRETER = ""
    }

    class Builder(
        private var rawData: ByteArray,
        private var editable: Boolean = false,
        private var labelId: Int = Element.NO_ID_SPECIFIED,
        private var descriptionId: Int = Element.NO_ID_SPECIFIED,
        private var parent: Element? = null,
        private var decoder: (ByteArray, Element?) -> List<Element> = ::defaultDecoder,
        private var validator: (ByteArray) -> Boolean = { true },
        private var interpreter: (ByteArray) -> String = { NO_INTERPRETER }
    ) {
        fun editable(editable: Boolean) = also { it.editable = editable }
        fun labelId(labelId: Int) = also { it.labelId = labelId }
        fun descriptionId(descriptionId: Int) = also { it.descriptionId = descriptionId }
        fun parent(parent: Element?) = also { it.parent = parent }
        fun decoder(decoder: (ByteArray, Element?) -> List<Element>) = also { it.decoder = decoder }
        fun validator(validator: (ByteArray) -> Boolean) = also { it.validator = validator }
        fun interpreter(interpreter: (ByteArray) -> String) = also { it.interpreter = interpreter }
        fun build() = ConstructedElement(rawData, editable, labelId, descriptionId, parent, decoder,
                validator, interpreter)
    }

    override val primitive: Boolean = false

    override val data: ByteArray
        get() {
            var array = byteArrayOf()
            elementList.forEach { array += it.data }
            return array
        }

    override val subElements: List<Element>
        get() = elementList

    override val rootElement: Element
        get() {
            return parent?.rootElement ?: this
        }

    override fun setData(newData: ByteArray): Boolean {
        if (!editable || !validator(newData)) return false

        val newList = decoder(newData, this)
        if (newList.isEmpty()) return false

        val oldList = elementList
        val oldSize = data.size
        elementList = newList
        if (data.size > oldSize) {
            if (rootElement.toByteArray().isEmpty()) {
                elementList = oldList
                return false
            }
        }

        return true
    }

    override fun toByteArray(): ByteArray {
        if (!validator(data)) return byteArrayOf()
        return data
    }

    override fun toString(): String {
        val interpretation = interpreter(data)
        if (interpretation.isNotEmpty()) return interpretation

        val builder = StringBuilder()
        val iterator = elementList.iterator()
        while (iterator.hasNext()) {
            builder.append(iterator.next().toString())
            if (iterator.hasNext()) {
                builder.append(STRING_SEPARATOR)
            }
        }
        return builder.toString()
    }
}
