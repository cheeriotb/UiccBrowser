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
import com.github.cheeriotb.uiccbrowser.element.EfDecoderRegistry
import com.github.cheeriotb.uiccbrowser.repository.FileId
import com.github.cheeriotb.uiccbrowser.util.hexStringToByteArray
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IsimEfDecodersUnitTest {

    private lateinit var resources: Resources

    companion object {
        private const val AID_ISIM = AppTemplate.RID + AppTemplate.APP_ISIM
        private const val IMPI = "801075736572406578616D706C652E636F6D"
        private const val DOMAIN = "800B6578616D706C652E636F6D"
        private const val IMPU = "80087369703A75736572800874656C3A2B313233"
    }

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    }

    @Test
    fun decodeImpi_validData_returnsPrivateUserIdentity() {
        val element = IsimEfDecoders.decodeImpi(resources, hexStringToByteArray(IMPI))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_impi_label))
        assertThat(element.subElements).hasSize(1)
        assertThat(element.subElements[0].label)
                .isEqualTo(resources.getString(R.string.private_user_identity_label))
        assertThat(element.subElements[0].toString())
                .isEqualTo("75736572406578616D706C652E636F6D (user@example.com)")
    }

    @Test
    fun decodeDomain_validData_returnsHomeNetworkDomainName() {
        val element = IsimEfDecoders.decodeDomain(resources, hexStringToByteArray(DOMAIN))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_domain_label))
        assertThat(element.subElements[0].label)
                .isEqualTo(resources.getString(R.string.home_network_domain_name_label))
        assertThat(element.subElements[0].toString())
                .isEqualTo("6578616D706C652E636F6D (example.com)")
    }

    @Test
    fun decodeImpu_validData_returnsIndexedPublicUserIdentities() {
        val element = IsimEfDecoders.decodeImpu(resources, hexStringToByteArray(IMPU))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_impu_label))
        assertThat(element.subElements).hasSize(2)
        assertThat(element.subElements[0].label).isEqualTo("Public user identity 1")
        assertThat(element.subElements[0].toString()).isEqualTo("7369703A75736572 (sip:user)")
        assertThat(element.subElements[1].label).isEqualTo("Public user identity 2")
        assertThat(element.subElements[1].toString()).isEqualTo("74656C3A2B313233 (tel:+123)")
    }

    @Test
    fun decodeInvalidData_returnsNull() {
        assertThat(IsimEfDecoders.decodeImpi(resources, byteArrayOf())).isNull()
        assertThat(IsimEfDecoders.decodeDomain(resources, hexStringToByteArray("810100"))).isNull()
        assertThat(IsimEfDecoders.decodeImpu(resources, byteArrayOf())).isNull()
    }

    @Test
    fun efDecoderRegistry_isimEfDecodersAreRegistered() {
        val registered = listOf(
                FileId.EF_ISIM_IMPI,
                FileId.EF_ISIM_DOMAIN,
                FileId.EF_ISIM_IMPU
        )

        registered.forEach { fileId ->
            assertThat(EfDecoderRegistry.has(AID_ISIM, FileId.PATH_ADF + fileId)).isTrue()
            assertThat(EfDecoderRegistry.find(AID_ISIM, FileId.PATH_ADF + fileId)).isNotNull()
        }
    }
}
