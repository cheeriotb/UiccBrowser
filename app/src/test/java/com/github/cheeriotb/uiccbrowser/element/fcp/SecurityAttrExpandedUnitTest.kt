/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.element.fcp

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.github.cheeriotb.uiccbrowser.R
import com.github.cheeriotb.uiccbrowser.element.BerTlvElement
import com.github.cheeriotb.uiccbrowser.util.BerTlv
import com.github.cheeriotb.uiccbrowser.util.hexStringToByteArray
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SecurityAttrExpandedUnitTest {

    private lateinit var resources: Resources

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    }

    @Test
    fun decoder_amTags80To8f_useAmDoLabel() {
        val tlvs = BerTlv.listFrom(hexStringToByteArray("8001008101018F010F"))
        val elements = SecurityAttrExpanded.decoder(resources, tlvs, null)

        assertThat(elements).hasSize(3)
        elements.forEach {
            assertThat(it.label).contains(resources.getString(R.string.am_do_label))
        }
    }

    @Test
    fun decoder_amDo_interpretsAccessModeBits() {
        val tlvs = BerTlv.listFrom(hexStringToByteArray("80011A"))
        val element = SecurityAttrExpanded.decoder(resources, tlvs, null)[0]

        assertThat(element.toString()).isEqualTo("1A (CREATE, DEACTIVATE, UPDATE)")
    }

    @Test
    fun decoder_amDo_interpretsReservedBit() {
        val tlvs = BerTlv.listFrom(hexStringToByteArray("800180"))
        val element = SecurityAttrExpanded.decoder(resources, tlvs, null)[0]

        assertThat(element.toString()).isEqualTo("80 (RESERVED)")
    }

    @Test
    fun decoder_alwaysAndNeverScDo_useSpecificLabels() {
        val tlvs = BerTlv.listFrom(hexStringToByteArray("90009700"))
        val elements = SecurityAttrExpanded.decoder(resources, tlvs, null)

        assertThat(elements[0].label)
                .contains(resources.getString(R.string.sc_do_always_label))
        assertThat(elements[1].label)
                .contains(resources.getString(R.string.sc_do_never_label))
    }

    @Test
    fun decoder_controlDo_decodesKeyReferenceAndUsageQualifierChildren() {
        val tlvs = BerTlv.listFrom(hexStringToByteArray("A40683010A950108"))
        val element = SecurityAttrExpanded.decoder(resources, tlvs, null)[0]
        val children = element.subElements.filterIsInstance<BerTlvElement>()

        assertThat(element.label).contains(resources.getString(R.string.control_do_label))
        assertThat(children.map { it.tag }).containsExactly(
                SecurityAttrExpanded.TAG_KEY_REFERENCE,
                SecurityAttrExpanded.TAG_USAGE_QUALIFIER
        ).inOrder()
        assertThat(children[0].label).contains(resources.getString(R.string.key_reference_label))
        assertThat(children[1].label).contains(resources.getString(R.string.usage_qualifier_label))
    }

    @Test
    fun decoder_keyReference_interpretsGlobalPinAndAdmKeys() {
        val tlvs = BerTlv.listFrom(hexStringToByteArray("A40683010183010A"))
        val element = SecurityAttrExpanded.decoder(resources, tlvs, null)[0]
        val children = element.subElements.filterIsInstance<BerTlvElement>()

        assertThat(children.map { it.toString() })
                .containsExactly("01 (Global PIN1)", "0A (ADM1)")
                .inOrder()
    }

    @Test
    fun decoder_keyReference_interpretsLocalPinAndAdmKeys() {
        val tlvs = BerTlv.listFrom(hexStringToByteArray("A40683018183018D"))
        val element = SecurityAttrExpanded.decoder(resources, tlvs, null)[0]
        val children = element.subElements.filterIsInstance<BerTlvElement>()

        assertThat(children.map { it.toString() })
                .containsExactly("81 (Local PIN1)", "8D (ADM4)")
                .inOrder()
    }

    @Test
    fun decoder_logicalDo_decodesNestedSecurityConditions() {
        val tlvs = BerTlv.listFrom(hexStringToByteArray("AF0AA40683010A9501089000"))
        val element = SecurityAttrExpanded.decoder(resources, tlvs, null)[0]
        val children = element.subElements.filterIsInstance<BerTlvElement>()

        assertThat(element.label).contains(resources.getString(R.string.and_do_label))
        assertThat(children.map { it.tag }).containsExactly(
                SecurityAttrExpanded.TAG_CONTROL_DO,
                SecurityAttrExpanded.TAG_SC_ALWAYS_DO
        ).inOrder()
        assertThat(children[0].label).contains(resources.getString(R.string.control_do_label))
        assertThat(children[1].label).contains(resources.getString(R.string.sc_do_always_label))
    }

    @Test
    fun decoder_unknownTag_keepsUnknownElement() {
        val tlvs = BerTlv.listFrom(hexStringToByteArray("5A0101"))
        val element = SecurityAttrExpanded.decoder(resources, tlvs, null)[0]

        assertThat(element.label).contains(resources.getString(R.string.unknown_label))
    }
}
