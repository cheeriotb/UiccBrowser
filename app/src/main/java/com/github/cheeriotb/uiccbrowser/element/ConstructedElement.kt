/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.element

import android.content.res.Resources
import com.github.cheeriotb.uiccbrowser.R
import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString

class ConstructedElement private constructor(
    resources: Resources,
    rawData: ByteArray,
    override val editable: Boolean,
    labelId: Int,
    labelArgs: Array<out Any>,
    private val parent: Element?,
    private val decoder: (Resources, ByteArray, Element?) -> List<Element>,
    private val dataComposer: (List<Element>) -> ByteArray,
    private val validator: (ByteArray) -> Boolean,
    private val interpreter: (Resources, ByteArray) -> String
) : Element {

    override val primitive: Boolean = false
    override val label: String = if (labelArgs.isEmpty()) {
        resources.getString(labelId)
    } else {
        resources.getString(labelId, *labelArgs)
    }

    private var elementList: List<Element> = decoder(resources, rawData, this)
    private var interpretation = interpreter(resources, rawData)

    companion object {
        fun defaultDecoder(
            resources: Resources,
            rawData: ByteArray,
            parent: Element?
        ): List<Element> {
            val element = PrimitiveElement.Builder(rawData)
                    .parent(parent)
                    .validator { false }
                    .build(resources)
            return listOf(element)
        }

        fun defaultInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            return byteArrayToHexString(rawData)
        }

        fun defaultDataComposer(
            elements: List<Element>
        ): ByteArray {
            var array = byteArrayOf()
            elements.forEach { array += it.data }
            return array
        }
    }

    class Builder(
        private var rawData: ByteArray,
        private var editable: Boolean = false,
        private var labelId: Int = R.string.unknown_label,
        private var labelArgs: Array<out Any> = emptyArray(),
        private var parent: Element? = null,
        private var decoder: (Resources, ByteArray, Element?) -> List<Element> = ::defaultDecoder,
        private var dataComposer: (List<Element>) -> ByteArray = ::defaultDataComposer,
        private var validator: (ByteArray) -> Boolean = { true },
        private var interpreter: (Resources, ByteArray) -> String = ::defaultInterpreter
    ) {
        fun editable(editable: Boolean) = also { it.editable = editable }
        fun labelId(labelId: Int) = also { it.labelId = labelId }
        fun labelArgs(vararg labelArgs: Any) = also { it.labelArgs = labelArgs }
        fun parent(parent: Element?) = also { it.parent = parent }

        fun decoder(decoder: (Resources, ByteArray, Element?) -> List<Element>) =
                also { it.decoder = decoder }
        fun dataComposer(dataComposer: (List<Element>) -> ByteArray) =
                also { it.dataComposer = dataComposer }
        fun validator(validator: (ByteArray) -> Boolean) =
                also { it.validator = validator }
        fun interpreter(interpreter: (Resources, ByteArray) -> String) =
                also { it.interpreter = interpreter }

        fun build(resources: Resources) = ConstructedElement(
                resources, rawData, editable, labelId, labelArgs, parent, decoder, dataComposer,
                validator, interpreter)
    }

    override val data: ByteArray
        get() = dataComposer(elementList)

    override val subElements: List<Element>
        get() = elementList

    override val rootElement: Element
        get() = parent?.rootElement ?: this

    override val byteArray: ByteArray
        get() {
            val rawData = data
            if (!validator(rawData)) return byteArrayOf()
            return rawData
        }

    override fun setData(resources: Resources, newData: ByteArray): Boolean {
        if (!editable || !validator(newData)) return false

        val newList = decoder(resources, newData, this)
        if (newList.isEmpty()) return false

        val oldList = elementList
        val oldSize = data.size
        elementList = newList
        if (data.size > oldSize) {
            if (rootElement.byteArray.isEmpty()) {
                elementList = oldList
                return false
            }
        }

        interpretation = interpreter(resources, newData)
        return true
    }

    override fun toString() = interpretation
}
