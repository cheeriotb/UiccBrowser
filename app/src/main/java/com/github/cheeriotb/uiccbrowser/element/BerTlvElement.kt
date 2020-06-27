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
import com.github.cheeriotb.uiccbrowser.util.BerTlv
import com.github.cheeriotb.uiccbrowser.util.Tlv
import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString

class BerTlvElement private constructor(
    resources: Resources,
    tlv: Tlv,
    override val editable: Boolean,
    labelId: Int,
    private val parent: Element?,
    private val decoder: (Resources, List<Tlv>, Element?) -> List<Element>,
    private val separator: (Resources, ByteArray, Element?) -> List<Element>,
    private val validator: (ByteArray) -> Boolean,
    private val interpreter: (Resources, ByteArray) -> String
) : Element {
    override val primitive: Boolean = !tlv.isConstructed
    override val label: String = resources.getString(labelId)

    val tag: Int = tlv.tag

    private var primitiveData =
            if (!tlv.isConstructed) tlv.value else byteArrayOf()
    private val separatedPrimitiveData =
            if (!tlv.isConstructed) separator(resources, primitiveData, this) else listOf()
    private var elementList =
            if (tlv.tlvs.isNotEmpty()) decoder(resources, tlv.tlvs, this) else listOf()
    private var interpretation =
            interpreter(resources, tlv.value)

    companion object {
        fun defaultDecoder(
            resources: Resources,
            tlvs: List<Tlv>,
            parent: Element?
        ): List<Element> {
            val list = mutableListOf<Element>()

            tlvs.forEach {
                val element = Builder(it)
                        .parent(parent)
                        .decoder(::defaultDecoder)
                        .build(resources)
                list.add(element)
            }

            return list
        }

        fun defaultValidator(
            rawData: ByteArray
        ): Boolean = true

        fun defaultSeparator(
            resources: Resources,
            value: ByteArray,
            parent: Element?
        ): List<Element> = listOf()

        fun defaultInterpreter(
            resources: Resources,
            rawData: ByteArray
        ): String {
            return byteArrayToHexString(rawData)
        }
    }

    class Builder(
        private var tlv: Tlv,
        private var editable: Boolean = false,
        private var labelId: Int = R.string.unknown_label,
        private var parent: Element? = null,
        private var decoder: (Resources, List<Tlv>, Element?) -> List<Element> =
                ::defaultDecoder,
        private var separator: (Resources, ByteArray, Element?) -> List<Element> =
                ::defaultSeparator,
        private var validator: (ByteArray) -> Boolean =
                ::defaultValidator,
        private var interpreter: (Resources, ByteArray) -> String =
                ::defaultInterpreter
    ) {
        fun editable(editable: Boolean) = also { it.editable = editable }
        fun labelId(labelId: Int) = also { it.labelId = labelId }
        fun parent(parent: Element?) = also { it.parent = parent }

        fun decoder(decoder: (Resources, List<Tlv>, Element?) -> List<Element>) =
                also { it.decoder = decoder }
        fun separator(separator: (Resources, ByteArray, Element?) -> List<Element>) =
                also { it.separator = separator }
        fun validator(validator: (ByteArray) -> Boolean) =
                also { it.validator = validator }
        fun interpreter(interpreter: (Resources, ByteArray) -> String) =
                also { it.interpreter = interpreter }
        fun build(resources: Resources) = BerTlvElement(
                resources, tlv, editable, labelId, parent, decoder, separator, validator,
                interpreter)
    }

    override val data: ByteArray
        get() {
            if (primitive) return primitiveData

            var array = byteArrayOf()
            elementList.forEach {
                val append = it.byteArray
                if (append.isEmpty()) return byteArrayOf()
                array += append
            }

            return array
        }

    override val subElements: List<Element>
        get() = if (separatedPrimitiveData.isNotEmpty()) separatedPrimitiveData else elementList

    override val rootElement: Element
        get() = parent?.rootElement ?: this

    override val byteArray: ByteArray
        get() {
            if (!validator(data)) return byteArrayOf()
            return BerTlv.encode(tag, data)
        }

    override fun setData(resources: Resources, newData: ByteArray): Boolean {
        if (!editable || !validator(newData)) return false

        if (primitive) {
            val oldData = primitiveData
            primitiveData = newData
            if (primitiveData.size > oldData.size) {
                if (rootElement.byteArray.isEmpty()) {
                    primitiveData = oldData
                    return false
                }
            }
        } else {
            val tlvs = BerTlv.listFrom(newData)
            val newList = decoder(resources, tlvs, this)
            if (newList.isEmpty()) return false
            newList.forEach { if (it.byteArray.isEmpty()) return false }

            val oldList = elementList
            val oldSize = data.size
            elementList = newList
            if (data.size > oldSize) {
                if (rootElement.byteArray.isEmpty()) {
                    elementList = oldList
                    return false
                }
            }
        }

        return true
    }

    override fun toString() = interpretation
}
