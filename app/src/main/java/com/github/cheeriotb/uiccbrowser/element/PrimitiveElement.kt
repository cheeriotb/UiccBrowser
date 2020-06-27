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

class PrimitiveElement private constructor(
    resources: Resources,
    private var primitiveData: ByteArray,
    override val editable: Boolean,
    labelId: Int,
    private val parent: Element?,
    private val validator: (ByteArray) -> Boolean,
    private val interpreter: (Resources, ByteArray) -> String
) : Element {

    override val primitive: Boolean = true
    override val subElements: List<Element> = listOf()
    override val label: String = resources.getString(labelId)

    private var interpretation = interpreter(resources, primitiveData)

    companion object {
        fun defaultInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            return byteArrayToHexString(rawData)
        }
    }

    class Builder(
        private var primitiveData: ByteArray,
        private var editable: Boolean = false,
        private var labelId: Int = R.string.unknown_label,
        private var parent: Element? = null,
        private var validator: (ByteArray) -> Boolean = { true },
        private var interpreter: (Resources, ByteArray) -> String = ::defaultInterpreter
    ) {
        fun editable(editable: Boolean) = also { it.editable = editable }
        fun labelId(labelId: Int) = also { it.labelId = labelId }
        fun parent(parent: Element?) = also { it.parent = parent }

        fun validator(validator: (ByteArray) -> Boolean) =
                also { it.validator = validator }
        fun interpreter(interpreter: (Resources, ByteArray) -> String) =
                also { it.interpreter = interpreter }

        fun build(resources: Resources) = PrimitiveElement(
                resources, primitiveData, editable, labelId, parent, validator, interpreter)
    }

    override val data: ByteArray
        get() = primitiveData

    override val rootElement: Element
        get() = parent?.rootElement ?: this

    override val byteArray: ByteArray
        get() = if (!validator(primitiveData)) byteArrayOf() else primitiveData

    override fun setData(
        resources: Resources,
        newData: ByteArray
    ): Boolean {
        if (!editable || !validator(newData)) return false

        val backup = primitiveData
        primitiveData = newData
        if (newData.size > backup.size) {
            if (rootElement.byteArray.isEmpty()) {
                primitiveData = backup
                return false
            }
        }

        interpretation = interpreter(resources, primitiveData)
        return true
    }

    override fun toString() = interpretation
}
