/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.element.ef

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.github.cheeriotb.uiccbrowser.R
import com.github.cheeriotb.uiccbrowser.element.BerTlvElement
import com.github.cheeriotb.uiccbrowser.element.fcp.SecurityAttrExpanded
import com.github.cheeriotb.uiccbrowser.util.hexStringToByteArray
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EfArrRecordUnitTest {

    private lateinit var resources: Resources

    companion object {
        // AM DO (0x80) + SC DO "always" (0x90).
        private const val RECORD_READ_ALWAYS = "8001019000"

        // Two AM/SC pairs. The second SC DO uses a control reference template (0xA4).
        private const val RECORD_MULTIPLE_PAIRS = "8001019000810102A403830101"

        private const val RECORD_NESTED_SC_DO =
                "80011AA40683010A95010880010190008401D4A010A40683010A950108" +
                "A40683010C950108FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"

        private const val RECORD_UNUSED =
                "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
        private const val RECORD_INVALID_TLV = "8002AA"
    }

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    }

    @Test
    fun decode_accessRuleRecord_returnsElement() {
        val element = EfArrRecord.decode(resources, hexStringToByteArray(RECORD_READ_ALWAYS))
        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_arr_record_label))
    }

    @Test
    fun decode_accessRuleRecord_containsAmAndScChildren() {
        val element = EfArrRecord.decode(resources, hexStringToByteArray(RECORD_READ_ALWAYS))!!
        val children = element.subElements.filterIsInstance<BerTlvElement>()

        assertThat(children).hasSize(2)
        assertThat(children[0].tag).isEqualTo(0x80)
        assertThat(children[0].label).contains(resources.getString(R.string.am_do_label))
        assertThat(children[1].tag).isEqualTo(0x90)
        assertThat(children[1].label).contains(resources.getString(R.string.sc_do_always_label))
    }

    @Test
    fun decode_multipleAccessRulePairs_keepsOrder() {
        val element = EfArrRecord.decode(resources, hexStringToByteArray(RECORD_MULTIPLE_PAIRS))!!
        val children = element.subElements.filterIsInstance<BerTlvElement>()

        assertThat(children.map { it.tag }).containsExactly(0x80, 0x90, 0x81, 0xA4).inOrder()
        assertThat(children[0].label).contains(resources.getString(R.string.am_do_label))
        assertThat(children[1].label).contains(resources.getString(R.string.sc_do_always_label))
        assertThat(children[2].label).contains(resources.getString(R.string.am_do_label))
        assertThat(children[3].label).contains(resources.getString(R.string.control_do_label))
    }

    @Test
    fun decode_nestedScDoRecord_containsNestedSecurityConditionTree() {
        val element = EfArrRecord.decode(resources, hexStringToByteArray(RECORD_NESTED_SC_DO))!!
        val children = element.subElements.filterIsInstance<BerTlvElement>()
        val controlDo = children[1]
        val orDo = children[5]

        assertThat(children.map { it.tag }).containsExactly(
                0x80,
                SecurityAttrExpanded.TAG_CONTROL_DO,
                0x80,
                SecurityAttrExpanded.TAG_SC_ALWAYS_DO,
                0x84,
                SecurityAttrExpanded.TAG_OR_DO
        ).inOrder()
        assertThat(controlDo.label).contains(resources.getString(R.string.control_do_label))
        assertThat(controlDo.subElements.filterIsInstance<BerTlvElement>().map { it.tag })
                .containsExactly(
                        SecurityAttrExpanded.TAG_KEY_REFERENCE,
                        SecurityAttrExpanded.TAG_USAGE_QUALIFIER
                ).inOrder()
        assertThat(orDo.label).contains(resources.getString(R.string.or_do_label))
        assertThat(orDo.subElements.filterIsInstance<BerTlvElement>()[0].label)
                .contains(resources.getString(R.string.control_do_label))
    }

    @Test
    fun decode_unusedRecord_returnsNull() {
        val element = EfArrRecord.decode(resources, hexStringToByteArray(RECORD_UNUSED))
        assertThat(element).isNull()
    }

    @Test
    fun decode_invalidTlv_returnsNull() {
        val element = EfArrRecord.decode(resources, hexStringToByteArray(RECORD_INVALID_TLV))
        assertThat(element).isNull()
    }

    @Test
    fun decode_emptyBytes_returnsNull() {
        val element = EfArrRecord.decode(resources, byteArrayOf())
        assertThat(element).isNull()
    }
}
