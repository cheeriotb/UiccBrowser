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
import com.github.cheeriotb.uiccbrowser.element.EfDecoderRegistry
import com.github.cheeriotb.uiccbrowser.repository.FileId
import com.github.cheeriotb.uiccbrowser.util.hexStringToByteArray
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TelecomSubDfEfDecodersUnitTest {
    private lateinit var resources: Resources

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    }

    @Test
    fun decodeImg_validRecord_splitsAllDescriptorFields() {
        val bytes = hexStringToByteArray("011020114F3000100020")
        val element = GraphicsEfDecoders.decodeImg(resources, bytes)

        assertThat(element).isNotNull()
        assertThat(element!!.subElements).hasSize(2)
        assertThat(element.subElements[0].toString()).isEqualTo("01 (1)")
        assertThat(element.subElements[1].label).isEqualTo("Image instance descriptor 1")
        assertThat(element.subElements[1].subElements).hasSize(6)
        assertThat(element.subElements[1].subElements[2].toString())
                .isEqualTo("11 (Basic image coding scheme)")
        assertThat(element.subElements[1].subElements[4].toString()).isEqualTo("0010 (16)")
        assertThat(element.byteArray).isEqualTo(bytes)
    }

    @Test
    fun decodeIidf_validData_returnsImageInstanceDataElement() {
        val bytes = hexStringToByteArray("01020304")
        val element = GraphicsEfDecoders.decodeIidf(resources, bytes)

        assertThat(element).isNotNull()
        assertThat(element!!.subElements.single().label).isEqualTo("Image instance data")
        assertThat(element.byteArray).isEqualTo(bytes)
    }

    @Test
    fun serviceTables_validData_splitCodingAndServiceBits() {
        val mstBytes = hexStringToByteArray("0005")
        val mst = McsEfDecoders.decodeMst(resources, mstBytes)
        val vst = V2xEfDecoders.decodeVst(resources, hexStringToByteArray("0105"))
        val astBytes = hexStringToByteArray("15")
        val ast = A2xEfDecoders.decodeAst(resources, astBytes)

        assertThat(mst).isNotNull()
        assertThat(mst!!.subElements).hasSize(9)
        assertThat(mst.subElements[0].toString()).isEqualTo("XML format")
        assertThat(mst.subElements[1].label).isEqualTo("MCPTT UE configuration data")
        assertThat(mst.subElements[1].toString()).isEqualTo("Available")
        assertThat(mst.byteArray).isEqualTo(mstBytes)
        assertThat(vst!!.subElements[0].toString()).isEqualTo("Specified binary format")
        assertThat(ast!!.subElements[3].label)
                .isEqualTo("A2X Direct C2 communication policy data over PC5")
        assertThat(ast.byteArray).isEqualTo(astBytes)
    }

    @Test
    fun policyDecoders_validData_splitPrefixNestedTlvsAndPadding() {
        val pc5Bytes = hexStringToByteArray("A0090102030405058001AA")
        val pc5 = V2xEfDecoders.decodeV2xpPc5(resources, pc5Bytes)
        val configBytes = hexStringToByteArray("A004018001AAFF")
        val config = A2xEfDecoders.decodeA2xConfig(resources, configBytes)
        val dc2Bytes = hexStringToByteArray("A0068001AA8101BB")
        val dc2 = A2xEfDecoders.decodeA2xDc2pPc5(resources, dc2Bytes)

        assertThat(pc5).isNotNull()
        assertThat(pc5!!.subElements.single().subElements).hasSize(5)
        assertThat(pc5.subElements.single().subElements[2].label).isEqualTo("Validity timer")
        assertThat(pc5.subElements.single().subElements[3].label).isEqualTo("Indicator bits")
        assertThat(pc5.byteArray).isEqualTo(pc5Bytes)
        assertThat(config!!.subElements.last().label).isEqualTo("Padding")
        assertThat(config.byteArray).isEqualTo(configBytes)
        assertThat(dc2!!.subElements.single().subElements).hasSize(4)
        assertThat(dc2.byteArray).isEqualTo(dc2Bytes)
    }

    @Test
    fun remainingPolicyDecoders_validData_recomposeOriginalBytes() {
        val variableTimerPolicy = hexStringToByteArray("A0090102030405058001AA")
        val ddaapPolicy = hexStringToByteArray("A004018001AA")
        val uuPolicy = hexStringToByteArray("A0090102030405058001AAFF")
        val decoded = listOf(
                V2xEfDecoders.decodeV2xpUu(resources, variableTimerPolicy),
                A2xEfDecoders.decodeA2xpPc5(resources, variableTimerPolicy),
                A2xEfDecoders.decodeA2xDdaapPc5(resources, ddaapPolicy),
                A2xEfDecoders.decodeA2xpUu(resources, uuPolicy))

        decoded.forEach { assertThat(it).isNotNull() }
        assertThat(decoded[0]!!.byteArray).isEqualTo(variableTimerPolicy)
        assertThat(decoded[1]!!.byteArray).isEqualTo(variableTimerPolicy)
        assertThat(decoded[2]!!.byteArray).isEqualTo(ddaapPolicy)
        assertThat(decoded[3]!!.byteArray).isEqualTo(uuPolicy)
    }

    @Test
    fun decodeA2xConfig_emptyObjects_returnsPadding() {
        val bytes = hexStringToByteArray("FFFFFFFF")
        val element = A2xEfDecoders.decodeA2xConfig(resources, bytes)

        assertThat(element).isNotNull()
        assertThat(element!!.subElements.single().label).isEqualTo("Padding")
        assertThat(element.byteArray).isEqualTo(bytes)
    }

    @Test
    fun decoders_invalidData_returnNull() {
        assertThat(GraphicsEfDecoders.decodeImg(resources, byteArrayOf(1))).isNull()
        assertThat(McsEfDecoders.decodeMst(resources, byteArrayOf(0))).isNull()
        assertThat(V2xEfDecoders.decodeV2xpPc5(resources, hexStringToByteArray("A001")))
                .isNull()
        assertThat(A2xEfDecoders.decodeAst(resources, byteArrayOf())).isNull()
        assertThat(A2xEfDecoders.decodeA2xpPc5(resources, hexStringToByteArray("8109000000000000")))
                .isNull()
        assertThat(A2xEfDecoders.decodeA2xpPc5(resources, hexStringToByteArray("A00100")))
                .isNull()
    }

    @Test
    fun registry_containsSupportedDecodersAndOmitsBerTlvFiles() {
        val supported = listOf(
                FileId.DF_GRAPHICS + FileId.EF_GRAPHICS_IMG,
                FileId.DF_MCS + FileId.EF_MCS_MST,
                FileId.DF_V2X + FileId.EF_V2X_VST,
                FileId.DF_V2X + FileId.EF_V2X_POLICY_PC5,
                FileId.DF_V2X + FileId.EF_V2X_POLICY_UU,
                FileId.DF_A2X + FileId.EF_A2X_AST,
                FileId.DF_A2X + FileId.EF_A2X_CONFIG,
                FileId.DF_A2X + FileId.EF_A2X_POLICY_PC5,
                FileId.DF_A2X + FileId.EF_A2X_DDAAP_PC5,
                FileId.DF_A2X + FileId.EF_A2X_DC2P_PC5,
                FileId.DF_A2X + FileId.EF_A2X_POLICY_UU)
        val unsupported = listOf(
                FileId.DF_GRAPHICS + FileId.EF_GRAPHICS_ICE,
                FileId.DF_MULTIMEDIA + FileId.EF_MULTIMEDIA_MML,
                FileId.DF_MULTIMEDIA + FileId.EF_MULTIMEDIA_MMDF,
                FileId.DF_MCS + FileId.EF_MCS_CONFIG,
                FileId.DF_V2X + FileId.EF_V2X_CONFIG)

        supported.forEach {
            assertThat(EfDecoderRegistry.has(FileId.AID_NONE, FileId.DF_TELECOM + it)).isTrue()
        }
        unsupported.forEach {
            assertThat(EfDecoderRegistry.has(FileId.AID_NONE, FileId.DF_TELECOM + it)).isFalse()
        }
    }
}
