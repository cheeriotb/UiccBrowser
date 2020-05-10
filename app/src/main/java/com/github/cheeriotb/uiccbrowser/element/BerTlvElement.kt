/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.element

import com.github.cheeriotb.uiccbrowser.util.BerTlv
import com.github.cheeriotb.uiccbrowser.util.Tlv
import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString

class BerTlvElement private constructor(
    tlv: Tlv,
    override val editable: Boolean,
    override val labelId: Int,
    override val descriptionId: Int,
    private val parent: Element?,
    private val decoder: (List<Tlv>, Element?) -> List<Element>,
    private val validator: (ByteArray) -> Boolean,
    private val interpreter: (ByteArray) -> String
) : Element {
    override val primitive: Boolean = !tlv.isConstructed

    private val tag: Int = tlv.tag
    private var primitiveData =
            if (!tlv.isConstructed) tlv.value else byteArrayOf()
    private var elementList: List<Element> =
            if (tlv.tlvs.isNotEmpty()) decoder(tlv.tlvs, this) else listOf()

    companion object {
        fun defaultDecoder(tlvs: List<Tlv>, parent: Element?): List<Element> {
            val list = mutableListOf<Element>()

            tlvs.forEach {
                val element = Builder(it)
                        .parent(parent)
                        .decoder(::defaultDecoder)
                        .build()
                list.add(element)
            }

            return list
        }

        const val STRING_SEPARATOR = ", "
        const val NO_INTERPRETER = ""
    }

    class Builder(
        private var tlv: Tlv,
        private var editable: Boolean = false,
        private var labelId: Int = Element.NO_ID_SPECIFIED,
        private var descriptionId: Int = Element.NO_ID_SPECIFIED,
        private var parent: Element? = null,
        private var decoder: (List<Tlv>, Element?) -> List<Element> = ::defaultDecoder,
        private var validator: (ByteArray) -> Boolean = { true },
        private var interpreter: (ByteArray) -> String = { NO_INTERPRETER }
    ) {
        fun editable(editable: Boolean) = also { it.editable = editable }
        fun labelId(labelId: Int) = also { it.labelId = labelId }
        fun descriptionId(descriptionId: Int) = also { it.descriptionId = descriptionId }
        fun parent(parent: Element?) = also { it.parent = parent }
        fun decoder(decoder: (List<Tlv>, Element?) -> List<Element>) = also { it.decoder = decoder }
        fun validator(validator: (ByteArray) -> Boolean) = also { it.validator = validator }
        fun interpreter(interpreter: (ByteArray) -> String) = also { it.interpreter = interpreter }
        fun build() = BerTlvElement(tlv, editable, labelId, descriptionId, parent, decoder,
                validator, interpreter)
    }

    override val data: ByteArray
        get() {
            if (primitive) return primitiveData

            var array = byteArrayOf()
            elementList.forEach {
                val append = it.toByteArray()
                if (append.isEmpty()) return byteArrayOf()
                array += append
            }

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

        if (primitive) {
            val oldData = primitiveData
            primitiveData = newData
            if (primitiveData.size > oldData.size) {
                if (rootElement.toByteArray().isEmpty()) {
                    primitiveData = oldData
                    return false
                }
            }
        } else {
            val tlvs = BerTlv.listFrom(newData)
            val newList = decoder(tlvs, this)
            if (newList.isEmpty()) return false
            newList.forEach { if (it.toByteArray().isEmpty()) return false }

            val oldList = elementList
            val oldSize = data.size
            elementList = newList
            if (data.size > oldSize) {
                if (rootElement.toByteArray().isEmpty()) {
                    elementList = oldList
                    return false
                }
            }
        }

        return true
    }

    override fun toByteArray(): ByteArray {
        if (!validator(data)) return byteArrayOf()
        return BerTlv.encode(tag, data)
    }

    override fun toString(): String {
        val interpretation = interpreter(data)
        if (interpretation.isNotEmpty()) return interpretation

        if (primitive) return byteArrayToHexString(data)

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
