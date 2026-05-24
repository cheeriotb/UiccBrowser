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
        private const val PCSCF =
                "800E0070637363662E6578616D706C65800501C0000201"
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
    fun decodeIst_validData_returnsServiceBits() {
        val element = IsimEfDecoders.decodeIst(resources, hexStringToByteArray("21"))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_ist_label))
        assertThat(element.subElements[0].subElements[0].label)
                .isEqualTo("n°1: P-CSCF address")
        assertThat(element.subElements[0].subElements[0].toString()).isEqualTo("Available")
        assertThat(element.subElements[0].subElements[5].label)
                .isEqualTo("n°6: Short Message Storage (SMS)")
        assertThat(element.subElements[0].subElements[5].toString()).isEqualTo("Available")
    }

    @Test
    fun decodePcscf_validData_returnsAddressRecords() {
        val element = IsimEfDecoders.decodePcscf(resources, hexStringToByteArray(PCSCF))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_pcscf_label))
        assertThat(element.subElements).hasSize(2)
        assertThat(element.subElements[0].label).isEqualTo("P-CSCF address record 1")
        assertThat(element.subElements[0].subElements[0].toString()).isEqualTo("00 (FQDN)")
        assertThat(element.subElements[0].subElements[1].toString())
                .isEqualTo("70637363662E6578616D706C65 (pcscf.example)")
        assertThat(element.subElements[1].subElements[0].toString()).isEqualTo("01 (IPv4 address)")
        assertThat(element.subElements[1].subElements[1].toString())
                .isEqualTo("C0000201 (192.0.2.1)")
    }

    @Test
    fun decodeAdAndSmss_validData_returnsFields() {
        val ad = IsimEfDecoders.decodeAd(resources, hexStringToByteArray("0100FFEE"))
        val smss = IsimEfDecoders.decodeSmss(resources, hexStringToByteArray("0501FFFF"))

        assertThat(ad).isNotNull()
        assertThat(ad!!.subElements.map { it.label }).containsExactly(
                "UE operation mode",
                "Additional information",
                "RFU",
                "RFU"
        ).inOrder()
        assertThat(smss).isNotNull()
        assertThat(smss!!.subElements[0].toString()).isEqualTo("05 (5)")
        assertThat(smss.subElements[1].toString()).isEqualTo("01 (Memory capacity exceeded)")
        assertThat(smss.subElements[2].label).isEqualTo("RFU")
    }

    @Test
    fun decodeCommonIsimFiles_validData_delegatesToSharedDecoders() {
        val gbabp = IsimEfDecoders.decodeGbabp(
                resources,
                hexStringToByteArray("04AABBCCDD03627469083230323630353233")
        )
        val uicciari = IsimEfDecoders.decodeUicciari(
                resources,
                hexStringToByteArray("800D75726E3A746573743A69617269")
        )
        val webrtc = IsimEfDecoders.decodeWebrtcuri(
                resources,
                hexStringToByteArray("800E7369703A6131406578616D706C65")
        )
        val imsdci = IsimEfDecoders.decodeImsdci(resources, hexStringToByteArray("02"))

        assertThat(gbabp).isNotNull()
        assertThat(gbabp!!.subElements[3].toString()).isEqualTo("627469 (bti)")
        assertThat(uicciari).isNotNull()
        assertThat(uicciari!!.subElements[0].toString()).contains("urn:test:iari")
        assertThat(webrtc).isNotNull()
        assertThat(webrtc!!.subElements[0].toString()).contains("sip:a1@example")
        assertThat(imsdci).isNotNull()
        assertThat(imsdci!!.subElements[0].toString()).contains("simultaneous setup")
    }

    @Test
    fun decodeInvalidData_returnsNull() {
        assertThat(IsimEfDecoders.decodeImpi(resources, byteArrayOf())).isNull()
        assertThat(IsimEfDecoders.decodeDomain(resources, hexStringToByteArray("810100"))).isNull()
        assertThat(IsimEfDecoders.decodeImpu(resources, byteArrayOf())).isNull()
        assertThat(IsimEfDecoders.decodeAd(resources, hexStringToByteArray("0102"))).isNull()
        assertThat(IsimEfDecoders.decodeIst(resources, byteArrayOf())).isNull()
        assertThat(IsimEfDecoders.decodePcscf(resources, hexStringToByteArray("810100"))).isNull()
        assertThat(IsimEfDecoders.decodeSmss(resources, hexStringToByteArray("00"))).isNull()
    }

    @Test
    fun efDecoderRegistry_isimEfDecodersAreRegistered() {
        val registered = listOf(
                FileId.EF_ISIM_IMPI,
                FileId.EF_ISIM_DOMAIN,
                FileId.EF_ISIM_IMPU,
                FileId.EF_ISIM_ARR,
                FileId.EF_ISIM_IST,
                FileId.EF_ISIM_P_CSCF,
                FileId.EF_ISIM_AC_GBAUAPI,
                FileId.EF_ISIM_IMSDCI,
                FileId.EF_ISIM_SMS,
                FileId.EF_ISIM_SMSP,
                FileId.EF_ISIM_SMSS,
                FileId.EF_ISIM_SMSR,
                FileId.EF_ISIM_AD,
                FileId.EF_ISIM_GBABP,
                FileId.EF_ISIM_GBANL,
                FileId.EF_ISIM_NAFKCA,
                FileId.EF_ISIM_UICCIARI,
                FileId.EF_ISIM_FROMPREFERRED,
                FileId.EF_ISIM_WEBRTCURI
        )

        registered.forEach { fileId ->
            assertThat(EfDecoderRegistry.has(AID_ISIM, FileId.PATH_ADF + fileId)).isTrue()
            assertThat(EfDecoderRegistry.find(AID_ISIM, FileId.PATH_ADF + fileId)).isNotNull()
        }
    }
}
