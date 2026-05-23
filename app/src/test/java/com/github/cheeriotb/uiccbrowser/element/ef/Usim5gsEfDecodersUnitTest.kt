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
class Usim5gsEfDecodersUnitTest {

    private lateinit var resources: Resources

    companion object {
        private const val AID_USIM = AppTemplate.RID + AppTemplate.APP_USIM
        private const val FIVEGS_LOCI = "000102030405060708090A0B0C13006200000100"
        private const val FIVEGS_NSC =
                "A03F8001038120000102030405060708090A0B0C0D0E0F101112131415161718191A1B" +
                        "1C1D1E1F8204000000018304000000028401018504010203048603130062"
        private const val AUTH_KEYS =
                "8020000102030405060708090A0B0C0D0E0F101112131415161718191A1B1C1D1E1F" +
                        "8120202122232425262728292A2B2C2D2E2F303132333435363738393A" +
                        "3B3C3D3E3F8220404142434445464748494A4B4C4D4E4F505152535455" +
                        "565758595A5B5C5D5E5F8302000184020002"
        private const val UAC_AIC = "01000080"
        private const val SUCI_CALC_INFO = "A00401000201A1088001018103010203"
        private const val OPL5G = "1300620000010000FF01"
        private const val SUPI_NAI = "800F616263406578616D706C652E636F6D"
        private const val ROUTING_INDICATOR = "21F3FFFF"
        private const val TN3GPPSNN = "01800C746573742E6E6574776F726B"
        private const val CAG = "000D0D130062030000000100000002"
        private const val SOR_CMCI = "8003010203"
        private const val DRI = "01FE00010002018003130062"
        private const val EDRX = "0305"
        private const val NSWO_CONF = "01"
        private const val MCHPPLMN = "05"
        private const val KAUSF_DERIVATION = "01FF"
    }

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    }

    @Test
    fun decode5gsLoci_validData_returnsLocationFields() {
        val element = Usim5gsEfDecoders.decode5gs3gppLoci(
                resources,
                hexStringToByteArray(FIVEGS_LOCI)
        )

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_5gs3gpp_loci_label))
        assertThat(element.subElements).hasSize(3)
        assertThat(element.subElements[1].toString())
                .contains(resources.getString(R.string.plmn_interpretation, "310", "260"))
        assertThat(element.subElements[2].toString()).isEqualTo("00 (Registered)")
    }

    @Test
    fun decode5gsNsc_validData_returnsSecurityContextFields() {
        val element = Usim5gsEfDecoders.decode5gs3gppNsc(
                resources,
                hexStringToByteArray(FIVEGS_NSC)
        )

        assertThat(element).isNotNull()
        assertThat(element!!.subElements).hasSize(1)
        assertThat(element.subElements[0].label)
                .isEqualTo(resources.getString(R.string.fivegs_nas_security_context_label))
        assertThat(element.subElements[0].subElements).hasSize(7)
        assertThat(element.subElements[0].subElements[0].subElements[0].toString())
                .isEqualTo("03 (3)")
        assertThat(element.subElements[0].subElements[6].subElements[0].toString())
                .isEqualTo("130062 (MCC 310, MNC 260)")
    }

    @Test
    fun decode5gAuthKeys_validData_returnsKeyAndCounterTlvs() {
        val element = Usim5gsEfDecoders.decode5gAuthKeys(
                resources,
                hexStringToByteArray(AUTH_KEYS)
        )

        assertThat(element).isNotNull()
        assertThat(element!!.subElements).hasSize(5)
        assertThat(element.subElements[0].label)
                .isEqualTo(resources.getString(R.string.kausf_label))
        assertThat(element.subElements[3].subElements[0].toString()).isEqualTo("0001 (1)")
    }

    @Test
    fun decodeUacAic_validData_returnsAccessIdentityBits() {
        val element = Usim5gsEfDecoders.decodeUacAic(resources, hexStringToByteArray(UAC_AIC))

        assertThat(element).isNotNull()
        assertThat(element!!.subElements).hasSize(33)
        assertThat(element.subElements[1].toString()).isEqualTo("01 (Present)")
        assertThat(element.subElements[32].toString()).isEqualTo("01 (Present)")
    }

    @Test
    fun decodeSuciCalcInfo_validData_returnsProtectionSchemesAndPublicKeys() {
        val element = Usim5gsEfDecoders.decodeSuciCalcInfo(
                resources,
                hexStringToByteArray(SUCI_CALC_INFO)
        )

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_suci_calc_info_label))
        assertThat(element.subElements).hasSize(2)
        assertThat(element.subElements[0].label)
                .isEqualTo(resources.getString(R.string.protection_scheme_identifier_list_label))
        assertThat(element.subElements[0].subElements).hasSize(2)
        assertThat(element.subElements[0].subElements[0].label).isEqualTo("Protection scheme 1")
        assertThat(element.subElements[0].subElements[0].subElements[0].toString())
                .isEqualTo("01 (1)")
        assertThat(element.subElements[0].subElements[0].subElements[1].toString())
                .isEqualTo("00 (0)")
        assertThat(element.subElements[1].label)
                .isEqualTo(resources.getString(R.string.home_network_public_key_list_label))
        assertThat(element.subElements[1].subElements).hasSize(2)
        assertThat(element.subElements[1].subElements[0].label)
                .isEqualTo(resources.getString(R.string.home_network_public_key_identifier_label))
        assertThat(element.subElements[1].subElements[0].subElements[0].toString())
                .isEqualTo("01")
        assertThat(element.byteArray).isEqualTo(hexStringToByteArray(SUCI_CALC_INFO))
    }

    @Test
    fun decodeOpl5g_validData_returnsIndexedRecords() {
        val element = Usim5gsEfDecoders.decodeOpl5g(resources, hexStringToByteArray(OPL5G))

        assertThat(element).isNotNull()
        assertThat(element!!.label).isEqualTo(resources.getString(R.string.ef_opl5g_label))
        assertThat(element.subElements).hasSize(1)
        assertThat(element.subElements[0].label).isEqualTo("Operator PLMN list entry 1")
        assertThat(element.subElements[0].subElements[0].toString())
                .isEqualTo("130062 (MCC 310, MNC 260)")
        assertThat(element.subElements[0].subElements[1].toString()).isEqualTo("000001")
        assertThat(element.subElements[0].subElements[2].toString()).isEqualTo("0000FF")
        assertThat(element.subElements[0].subElements[3].toString()).isEqualTo("01 (1)")
    }

    @Test
    fun decodeTransparent5gsFiles_validData_returnsDecodedFields() {
        assertThat(Usim5gsEfDecoders.decodeSupiNai(
                resources,
                hexStringToByteArray(SUPI_NAI)
        )!!.subElements[0].subElements[0].toString()).contains("abc@example.com")
        assertThat(Usim5gsEfDecoders.decodeRoutingIndicator(
                resources,
                hexStringToByteArray(ROUTING_INDICATOR)
        )!!.subElements[0].toString()).isEqualTo("21F3 (123)")
        assertThat(Usim5gsEfDecoders.decodeTn3gppsnn(
                resources,
                hexStringToByteArray(TN3GPPSNN)
        )!!.subElements[1].subElements[0].toString()).contains("test.network")
        assertThat(Usim5gsEfDecoders.decodeCag(
                resources,
                hexStringToByteArray(CAG)
        )!!.subElements[1].subElements).hasSize(5)
        assertThat(Usim5gsEfDecoders.decodeSorCmci(
                resources,
                hexStringToByteArray(SOR_CMCI)
        )!!.subElements[0].label).isEqualTo(resources.getString(R.string.sor_cmci_parameters_label))
        assertThat(Usim5gsEfDecoders.decodeDri(
                resources,
                hexStringToByteArray(DRI)
        )!!.subElements[5].subElements).hasSize(1)
        assertThat(Usim5gsEfDecoders.decode5gsEdrx(
                resources,
                hexStringToByteArray(EDRX)
        )!!.subElements[0].toString()).contains(resources.getString(R.string.ng_ran_label))
        assertThat(Usim5gsEfDecoders.decode5gNswoConf(
                resources,
                hexStringToByteArray(NSWO_CONF)
        )!!.subElements[0].toString()).isEqualTo("01 (Enabled)")
        assertThat(Usim5gsEfDecoders.decodeMchpplmn(
                resources,
                hexStringToByteArray(MCHPPLMN)
        )!!.subElements[0].toString()).isEqualTo("05 (5)")
        assertThat(Usim5gsEfDecoders.decodeKausfDerivation(
                resources,
                hexStringToByteArray(KAUSF_DERIVATION)
        )!!.subElements[0].toString()).contains("Use MSK")
    }

    @Test
    fun decodeInvalidData_returnsNull() {
        assertThat(Usim5gsEfDecoders.decode5gs3gppLoci(resources, hexStringToByteArray("00")))
                .isNull()
        assertThat(Usim5gsEfDecoders.decode5gs3gppNsc(resources, hexStringToByteArray("A100")))
                .isNull()
        assertThat(Usim5gsEfDecoders.decode5gAuthKeys(resources, hexStringToByteArray("8000")))
                .isNull()
        assertThat(Usim5gsEfDecoders.decodeUacAic(resources, hexStringToByteArray("0102")))
                .isNull()
        assertThat(Usim5gsEfDecoders.decodeSuciCalcInfo(resources, hexStringToByteArray("A100")))
                .isNull()
        assertThat(Usim5gsEfDecoders.decodeOpl5g(resources, hexStringToByteArray("130062")))
                .isNull()
    }

    @Test
    fun efDecoderRegistry_usim5gsEfDecodersAreRegistered() {
        val registered = listOf(
                FileId.EF_USIM_5GS_3GPP_LOCI,
                FileId.EF_USIM_5GS_N3GPP_LOCI,
                FileId.EF_USIM_5GS_3GPP_NSC,
                FileId.EF_USIM_5GS_N3GPP_NSC,
                FileId.EF_USIM_5GS_AUTH_KEYS,
                FileId.EF_USIM_5GS_UAC_AIC,
                FileId.EF_USIM_5GS_SUCI_CALC_INFO,
                FileId.EF_USIM_5GS_OPL5G,
                FileId.EF_USIM_5GS_SUPI_NAI,
                FileId.EF_USIM_5GS_ROUTING_INDICATOR,
                FileId.EF_USIM_5GS_TN3GPPSNN,
                FileId.EF_USIM_5GS_CAG,
                FileId.EF_USIM_5GS_SOR_CMCI,
                FileId.EF_USIM_5GS_DRI,
                FileId.EF_USIM_5GS_EDRX,
                FileId.EF_USIM_5GS_NSWO_CONF,
                FileId.EF_USIM_5GS_MCHPPLMN,
                FileId.EF_USIM_5GS_KAUSF_DERIVATION
        )

        registered.forEach { fileId ->
            val path = FileId.PATH_ADF + FileId.DF_USIM_5GS + fileId
            assertThat(EfDecoderRegistry.has(AID_USIM, path)).isTrue()
            assertThat(EfDecoderRegistry.find(AID_USIM, path)).isNotNull()
        }
        assertThat(EfDecoderRegistry.has(
                AID_USIM,
                FileId.PATH_ADF + FileId.DF_USIM_5GS + FileId.EF_USIM_5GS_URSP
        )).isFalse()
    }
}
