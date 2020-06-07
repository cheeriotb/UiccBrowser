/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.cardio

import android.content.Context
import android.telephony.IccOpenLogicalChannelResponse
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString
import com.github.cheeriotb.uiccbrowser.util.byteToHexString
import com.github.cheeriotb.uiccbrowser.util.extendedBytesToHexString
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class TelephonyInterfaceUnitTest {
    @Rule @JvmField
    val grantPermissionRule: GrantPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.READ_PHONE_STATE)

    private val context = ApplicationProvider.getApplicationContext() as Context
    private val smShadow = shadowOf(context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
            as SubscriptionManager)
    private val tmShadow = shadowOf(context.getSystemService(Context.TELEPHONY_SERVICE)
            as TelephonyManager)

    @Mock
    private lateinit var subInfoMock: SubscriptionInfo
    @Mock
    private lateinit var respMock: IccOpenLogicalChannelResponse
    @Mock
    private lateinit var tmMock: TelephonyManager

    private lateinit var tif: TelephonyInterface

    companion object {
        private val AID1 = byteArrayOf(b(0x10), b(0x11), b(0x12), b(0x13), b(0x14), b(0x15),
                b(0x16), b(0x17), b(0x18), b(0x19))
        private const val AID1_STRING = "10111213141516171819"

        private val AID2 = byteArrayOf(b(0x20), b(0x21), b(0x22), b(0x23), b(0x24), b(0x25),
                b(0x26), b(0x27), b(0x28), b(0x29))
        private const val AID2_STRING = "20212223242526272829"

        private const val TEST_SLOT_ID = 0 /* SIM #1 */
        private const val TEST_SLOT_ID_NOT_AVAILABLE = 1 /* SIM #2 */
        private const val TEST_SUBS_ID = 0

        private const val TEST_CLA_LOGICAL = 0x01
        private const val TEST_INS = 0xF0
        private const val TEST_P1 = 0x01
        private const val TEST_P2 = 0x02
        private const val TEST_DATA_NONE = ""

        private val SW_SUCCESS = byteArrayOf(b(0x90), b(0x00))
        private const val SW_SUCCESS_STRING = "9000"

        private const val TEST_OPEN_P2 = Interface.OPEN_P2
        private const val TEST_CHANNEL_ID = 0x01

        private fun b(byte: Int) = byte.toByte()
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        // Associate the subscription info #TEST_SUBS_ID with the slot #TEST_SLOT_ID.
        `when`(subInfoMock.simSlotIndex).thenReturn(TEST_SLOT_ID)
        `when`(subInfoMock.subscriptionId).thenReturn(TEST_SUBS_ID)
        smShadow.setActiveSubscriptionInfos(subInfoMock)
        tmShadow.setTelephonyManagerForSubscriptionId(TEST_SUBS_ID, tmMock)

        // Create the interface for the slot #TEST_SLOT_ID.
        tif = TelephonyInterface.from(context, TEST_SLOT_ID)

        // By default, both AID1 and AID2 are accessible.
        `when`(respMock.channel).thenReturn(TEST_CHANNEL_ID)
        `when`(respMock.status).thenReturn(IccOpenLogicalChannelResponse.STATUS_NO_ERROR)
        `when`(tmMock.iccOpenLogicalChannel(null, TEST_OPEN_P2)).thenReturn(respMock)
        `when`(tmMock.iccOpenLogicalChannel(AID1_STRING, TEST_OPEN_P2)).thenReturn(respMock)
        `when`(tmMock.iccOpenLogicalChannel(AID2_STRING, TEST_OPEN_P2)).thenReturn(respMock)
    }

    @After
    fun tearDown() {
        tif.dispose()
    }

    @Test
    fun available() {
        assertThat(tif.isAvailable).isEqualTo(true)
    }

    @Test
    fun unavailable() {
        tif = TelephonyInterface.from(context, TEST_SLOT_ID_NOT_AVAILABLE)

        assertThat(tif.isAvailable).isEqualTo(false)
        assertThat(tif.openChannel(AID1)).isEqualTo(Interface.OpenChannelResult.GENERIC_FAILURE)
        assertThat(tif.transmit(Command(TEST_INS, TEST_P1, TEST_P2)).sw)
                .isEqualTo(Response(Interface.SW_INTERNAL_EXCEPTION).sw)
        tif.closeRemainingChannel()

        verify(tmMock, never()).iccOpenLogicalChannel(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyInt())
        verify(tmMock, never()).iccTransmitApduLogicalChannel(ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyString())
        verify(tmMock, never()).iccCloseLogicalChannel(ArgumentMatchers.anyInt())
    }

    @Test
    fun openClose_logicalChannel_success() {
        assertThat(tif.openChannel(Interface.NO_AID_SPECIFIED)).isEqualTo(
                Interface.OpenChannelResult.SUCCESS)
        assertThat(tif.openChannel(AID1)).isEqualTo(Interface.OpenChannelResult.SUCCESS)
        assertThat(tif.openChannel(AID2)).isEqualTo(Interface.OpenChannelResult.SUCCESS)
        tif.closeRemainingChannel()

        verify(tmMock, times(1)).iccOpenLogicalChannel(null, TEST_OPEN_P2)
        verify(tmMock, times(1)).iccOpenLogicalChannel(AID1_STRING, TEST_OPEN_P2)
        verify(tmMock, times(1)).iccOpenLogicalChannel(AID2_STRING, TEST_OPEN_P2)
        verify(tmMock, times(3)).iccCloseLogicalChannel(TEST_CHANNEL_ID)
    }

    @Test
    fun openClose_logicalChannel_share() {
        assertThat(tif.openChannel(AID1)).isEqualTo(Interface.OpenChannelResult.SUCCESS)
        assertThat(tif.openChannel(AID1)).isEqualTo(Interface.OpenChannelResult.SUCCESS)
        tif.closeRemainingChannel()

        verify(tmMock, times(1)).iccOpenLogicalChannel(AID1_STRING, TEST_OPEN_P2)
        verify(tmMock, times(1)).iccCloseLogicalChannel(TEST_CHANNEL_ID)
    }

    @Test
    fun openClose_logicalChannel_failures() {
        `when`(respMock.status).thenReturn(IccOpenLogicalChannelResponse.STATUS_MISSING_RESOURCE)
        assertThat(tif.openChannel(AID1)).isEqualTo(Interface.OpenChannelResult.MISSING_RESOURCE)
        tif.closeRemainingChannel()

        `when`(respMock.status).thenReturn(IccOpenLogicalChannelResponse.STATUS_NO_SUCH_ELEMENT)
        assertThat(tif.openChannel(AID1)).isEqualTo(Interface.OpenChannelResult.NO_SUCH_ELEMENT)
        tif.closeRemainingChannel()

        `when`(respMock.status).thenReturn(IccOpenLogicalChannelResponse.STATUS_UNKNOWN_ERROR)
        assertThat(tif.openChannel(AID1)).isEqualTo(Interface.OpenChannelResult.GENERIC_FAILURE)
        tif.closeRemainingChannel()

        verify(tmMock, times(3)).iccOpenLogicalChannel(AID1_STRING, TEST_OPEN_P2)
        verify(tmMock, never()).iccCloseLogicalChannel(ArgumentMatchers.anyInt())
    }

    @Test
    fun transmit_logicalChannel_tooShortResponse() {
        val le = 0x100
        `when`(tmMock.iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL, TEST_INS,
                TEST_P1, TEST_P2, p3s(le), TEST_DATA_NONE))
                .thenReturn(byteArrayToHexString(ByteArray(Response.SW_SIZE - 1)))

        tif.openChannel(AID1)
        val result = tif.transmit(Command(TEST_INS, TEST_P1, TEST_P2, le))
        assertThat(result.sw1).isEqualTo(Iso7816.SW1_INTERNAL_EXCEPTION)
        tif.closeRemainingChannel()

        verify(tmMock, times(1)).iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL,
                TEST_INS, TEST_P1, TEST_P2, p3s(le), TEST_DATA_NONE)
    }

    @Test
    fun transmit_logicalChannel_tooLongResponse() {
        val le = Iso7816.MAX_LE
        `when`(tmMock.iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL, TEST_INS,
                TEST_P1, TEST_P2, le, TEST_DATA_NONE))
                .thenReturn(byteArrayToHexString(ByteArray(le + Response.SW_SIZE + 1)))

        tif.openChannel(AID1)
        val result = tif.transmit(Command(TEST_INS, TEST_P1, TEST_P2, le))
        assertThat(result.sw1).isEqualTo(Iso7816.SW1_INTERNAL_EXCEPTION)
        tif.closeRemainingChannel()

        verify(tmMock, times(1)).iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL,
            TEST_INS, TEST_P1, TEST_P2, le, TEST_DATA_NONE)
    }

    @Test
    fun transmit_logicalChannel_case1() {
        `when`(tmMock.iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL, TEST_INS,
                TEST_P1, TEST_P2, TelephonyInterface.CASE1_P3, TEST_DATA_NONE))
                .thenReturn(SW_SUCCESS_STRING)

        tif.openChannel(AID1)
        assertThat(tif.transmit(Command(TEST_INS, TEST_P1, TEST_P2)).sw)
                .isEqualTo(Response(SW_SUCCESS).sw)
        tif.closeRemainingChannel()

        verify(tmMock, times(1)).iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL,
                TEST_INS, TEST_P1, TEST_P2, TelephonyInterface.CASE1_P3, TEST_DATA_NONE)
    }

    @Test
    fun transmit_logicalChannel_case2s() {
        val le = 0x100
        val data = ByteArray(le) { i -> i.toByte() }
        `when`(tmMock.iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL, TEST_INS,
                TEST_P1, TEST_P2, p3s(le), TEST_DATA_NONE))
                .thenReturn(byteArrayToHexString(data) + SW_SUCCESS_STRING)

        tif.openChannel(AID1)
        val result = tif.transmit(Command(TEST_INS, TEST_P1, TEST_P2, le))
        assertThat(result.sw).isEqualTo(SW_SUCCESS_STRING.toInt(16))
        assertThat(result.data).isEqualTo(data)
        tif.closeRemainingChannel()

        verify(tmMock, times(1)).iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL,
                TEST_INS, TEST_P1, TEST_P2, p3s(le), TEST_DATA_NONE)
    }

    @Test
    fun transmit_logicalChannel_case2e() {
        val le = 0x101
        val data = ByteArray(le) { i -> i.toByte() }
        `when`(tmMock.iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL, TEST_INS,
                TEST_P1, TEST_P2, le, TEST_DATA_NONE))
                .thenReturn(byteArrayToHexString(data) + SW_SUCCESS_STRING)

        tif.openChannel(AID1)
        val result = tif.transmit(Command(TEST_INS, TEST_P1, TEST_P2, le))
        assertThat(result.sw).isEqualTo(SW_SUCCESS_STRING.toInt(16))
        assertThat(result.data).isEqualTo(data)
        tif.closeRemainingChannel()

        verify(tmMock, times(1)).iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL,
            TEST_INS, TEST_P1, TEST_P2, le, TEST_DATA_NONE)
    }

    @Test
    fun transmit_logicalChannel_case3s() {
        val lc = 0xFF
        val data = ByteArray(lc) { i -> i.toByte() }
        val dataString = byteArrayToHexString(data)
        `when`(tmMock.iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL, TEST_INS,
                TEST_P1, TEST_P2, lc, dataString)).thenReturn(SW_SUCCESS_STRING)

        tif.openChannel(AID1)
        assertThat(tif.transmit(Command(TEST_INS, TEST_P1, TEST_P2, data)).sw)
                .isEqualTo(Response(SW_SUCCESS).sw)
        tif.closeRemainingChannel()

        verify(tmMock, times(1)).iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL,
                TEST_INS, TEST_P1, TEST_P2, lc, dataString)
    }

    @Test
    fun transmit_logicalChannel_case3e() {
        val lc = 0x100
        val data = ByteArray(lc) { i -> i.toByte() }
        val dataString = byteArrayToHexString(data)
        `when`(tmMock.iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL, TEST_INS,
                TEST_P1, TEST_P2, lc, dataString)).thenReturn(SW_SUCCESS_STRING)

        tif.openChannel(AID1)
        assertThat(tif.transmit(Command(TEST_INS, TEST_P1, TEST_P2, data)).sw)
                .isEqualTo(Response(SW_SUCCESS).sw)
        tif.closeRemainingChannel()

        verify(tmMock, times(1)).iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL,
                TEST_INS, TEST_P1, TEST_P2, lc, dataString)
    }

    @Test
    fun transmit_logicalChannel_case4s() {
        val lcAndLe = 0xFF
        val data = ByteArray(lcAndLe) { i -> i.toByte() }
        val dataString = byteArrayToHexString(data)
        `when`(tmMock.iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL, TEST_INS,
                TEST_P1, TEST_P2, lcAndLe, dataString + byteToHexString(lcAndLe)))
                .thenReturn(dataString + SW_SUCCESS_STRING)

        tif.openChannel(AID1)
        val result = tif.transmit(Command(TEST_INS, TEST_P1, TEST_P2, data, lcAndLe))
        assertThat(result.sw).isEqualTo(SW_SUCCESS_STRING.toInt(16))
        assertThat(result.data).isEqualTo(data)
        tif.closeRemainingChannel()

        verify(tmMock, times(1)).iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL,
                TEST_INS, TEST_P1, TEST_P2, lcAndLe, dataString + byteToHexString(lcAndLe))
    }

    @Test
    fun transmit_logicalChannel_case4e() {
        val lcAndLe = 0x100
        val data = ByteArray(lcAndLe) { i -> i.toByte() }
        val dataString = byteArrayToHexString(data)
        `when`(tmMock.iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL, TEST_INS,
                TEST_P1, TEST_P2, lcAndLe, dataString + extendedBytesToHexString(lcAndLe)))
                .thenReturn(dataString + SW_SUCCESS_STRING)

        tif.openChannel(AID1)
        val result = tif.transmit(Command(TEST_INS, TEST_P1, TEST_P2, data, lcAndLe))
        assertThat(result.sw).isEqualTo(SW_SUCCESS_STRING.toInt(16))
        assertThat(result.data).isEqualTo(data)
        tif.closeRemainingChannel()

        verify(tmMock, times(1)).iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL,
                TEST_INS, TEST_P1, TEST_P2, lcAndLe, dataString + extendedBytesToHexString(lcAndLe))
    }

    @Test
    fun transmit_logicalChannel_wrongLe() {
        val wrongLe = 0x100
        val correctLe = 0x10
        val dataByteArray = ByteArray(correctLe) { i -> i.toByte() }

        `when`(tmMock.iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL, TEST_INS,
                TEST_P1, TEST_P2, p3s(wrongLe), TEST_DATA_NONE)).thenReturn(byteArrayToHexString(
                byteArrayOf(b(Iso7816.SW1_WRONG_LE), b(correctLe))))
        `when`(tmMock.iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL, TEST_INS,
                TEST_P1, TEST_P2, correctLe, TEST_DATA_NONE)).thenReturn(byteArrayToHexString(
                dataByteArray) + SW_SUCCESS_STRING)

        tif.openChannel(AID1)
        val result = tif.transmit(Command(TEST_INS, TEST_P1, TEST_P2, wrongLe))
        assertThat(result.sw).isEqualTo(SW_SUCCESS_STRING.toInt(16))
        assertThat(result.data).isEqualTo(dataByteArray)
        tif.closeRemainingChannel()

        verify(tmMock, times(1)).iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL,
                TEST_INS, TEST_P1, TEST_P2, p3s(wrongLe), TEST_DATA_NONE)
        verify(tmMock, times(1)).iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL,
                TEST_INS, TEST_P1, TEST_P2, correctLe, TEST_DATA_NONE)
    }

    @Test
    fun transmit_logicalChannel_dataAvailable() {
        val availableLe1 = 0x100
        val swDataAvailable1 = byteArrayToHexString(byteArrayOf(b(Iso7816.SW1_DATA_AVAILABLE),
                b(availableLe1)))
        val dataByteArray1 = ByteArray(availableLe1) { i -> i.toByte() }
        val availableLe2 = 0x01
        val swDataAvailable2 = byteArrayToHexString(byteArrayOf(b(Iso7816.SW1_DATA_AVAILABLE),
                b(availableLe2)))
        val dataByteArray2 = ByteArray(availableLe2) { i -> i.toByte() }

        `when`(tmMock.iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL, TEST_INS,
                TEST_P1, TEST_P2, Iso7816.MAX_LE, TEST_DATA_NONE)).thenReturn(swDataAvailable1)
        `when`(tmMock.iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL,
                Iso7816.INS_GET_RESPONSE, 0x00, 0x00, p3s(availableLe1), TEST_DATA_NONE))
                .thenReturn(byteArrayToHexString(dataByteArray1) + swDataAvailable2)
        `when`(tmMock.iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL,
                Iso7816.INS_GET_RESPONSE, 0x00, 0x00, availableLe2, TEST_DATA_NONE))
                .thenReturn(byteArrayToHexString(dataByteArray2) + SW_SUCCESS_STRING)

        tif.openChannel(AID1)
        val result = tif.transmit(Command(TEST_INS, TEST_P1, TEST_P2, Iso7816.MAX_LE))
        assertThat(result.sw).isEqualTo(SW_SUCCESS_STRING.toInt(16))
        assertThat(result.data).isEqualTo(dataByteArray1 + dataByteArray2)
        tif.closeRemainingChannel()

        verify(tmMock, times(1)).iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL,
                TEST_INS, TEST_P1, TEST_P2, Iso7816.MAX_LE, TEST_DATA_NONE)
        verify(tmMock, times(1)).iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL,
                Iso7816.INS_GET_RESPONSE, 0x00, 0x00, p3s(availableLe1), TEST_DATA_NONE)
        verify(tmMock, times(1)).iccTransmitApduLogicalChannel(TEST_CHANNEL_ID, TEST_CLA_LOGICAL,
                Iso7816.INS_GET_RESPONSE, 0x00, 0x00, availableLe2, TEST_DATA_NONE)
    }

    private fun p3s(le: Int) = if (le == 0x100) 0x00 else le
}
