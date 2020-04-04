/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.cardio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.IccOpenLogicalChannelResponse
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString
import com.github.cheeriotb.uiccbrowser.util.byteToHexString
import com.github.cheeriotb.uiccbrowser.util.extendedBytesToHexString
import com.github.cheeriotb.uiccbrowser.util.hexStringToByteArray
import java.lang.Exception

class TelephonyInterface private constructor (
    context: Context,
    slotId: Int
) : Interface {
    private val tag = TelephonyInterface::class.java.simpleName + slotId
    private val telephony: TelephonyManager?
    private var aid = BASIC_CHANNEL_AID
    private var channelId = BASIC_CHANNEL_ID

    companion object {
        private const val BASIC_CHANNEL_ID = 0
        val BASIC_CHANNEL_AID = ByteArray(0)
        const val CASE1_P3 = -1
        fun from(context: Context, slotId: Int) = TelephonyInterface(context, slotId)
    }

    init {
        check(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED) { "READ_PHONE_STATE shall be granted" }

        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                as SubscriptionManager?
        val subscriptionInfo = subscriptionManager?.getActiveSubscriptionInfoForSimSlotIndex(slotId)

        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        if (telephony != null && subscriptionInfo != null) {
            this.telephony = telephony.createForSubscriptionId(subscriptionInfo.subscriptionId)
        } else {
            this.telephony = null
        }
    }

    override fun openChannel(aid: ByteArray): Interface.OpenChannelResult {
        if (telephony == null) {
            Log.w(tag, "No subscription info is available")
            return Interface.OpenChannelResult.GENERIC_FAILURE
        }

        if (!this.aid.contentEquals(aid)) {
            closeRemainingChannel()
            if (!aid.contentEquals(BASIC_CHANNEL_AID)) {
                val aidString = byteArrayToHexString(aid)
                val result = telephony.iccOpenLogicalChannel(aidString, Interface.OPEN_P2)
                when (result.status) {
                    IccOpenLogicalChannelResponse.STATUS_NO_ERROR -> {
                        this.aid = aid
                        channelId = result.channel
                        Log.i(tag, "Opened the logical channel #$channelId")
                        Log.d(tag, "AID: $aidString")
                    }
                    IccOpenLogicalChannelResponse.STATUS_MISSING_RESOURCE -> {
                        Log.w(tag, "No logical channel is currently available")
                        return Interface.OpenChannelResult.MISSING_RESOURCE
                    }
                    IccOpenLogicalChannelResponse.STATUS_NO_SUCH_ELEMENT -> {
                        Log.w(tag, "The specified AID $aidString was not found")
                        return Interface.OpenChannelResult.NO_SUCH_ELEMENT
                    }
                    else -> {
                        Log.e(tag, "Unknown error happened")
                        return Interface.OpenChannelResult.GENERIC_FAILURE
                    }
                }
            }
        }

        return Interface.OpenChannelResult.SUCCESS
    }

    override fun transmit(command: Command): Response {
        if (telephony == null) {
            Log.w(tag, "No subscription info is available")
            return Response(Interface.SW_INTERNAL_EXCEPTION)
        }

        var apdu = Command(command)
        var response = Response(Interface.SW_SUCCESS)

        do {
            val p3 = if (apdu.lc > 0) apdu.lc /* Lc set as P3 if there is a command data */
                    else if (apdu.le == 0) CASE1_P3 /* No Lc/Le means Case 1 command */
                    else if (apdu.extended || apdu.le < 0x100) apdu.le
                    else 0x00 /* Le (P3) 0x00 means 256 bytes of expected data */

            val dataBuilder = StringBuilder().append(byteArrayToHexString(apdu.data))
            if ((apdu.lc > 0) and (apdu.le > 0)) {
                // Le field of Case 4 command shall be appended to the end of data.
                dataBuilder.append(if (apdu.extended) extendedBytesToHexString(apdu.le)
                        else byteToHexString(apdu.le))
            }

            try {
                Log.v(tag, "Sent: " + byteArrayToHexString(apdu.build()))
                val receivedString = if (!aid.contentEquals(BASIC_CHANNEL_AID)) {
                    telephony.iccTransmitApduLogicalChannel(channelId, apdu.cla(channelId),
                            apdu.ins, apdu.p1, apdu.p2, p3, dataBuilder.toString())
                } else {
                    telephony.iccTransmitApduBasicChannel(apdu.cla(channelId), apdu.ins,
                            apdu.p1, apdu.p2, p3, dataBuilder.toString())
                }
                Log.v(tag, "Received: $receivedString")
                val receivedArray = hexStringToByteArray(receivedString)

                if (receivedArray.size < Response.SW_SIZE) {
                    Log.e(tag, "At least two bytes shall be responded for status word")
                    return Response(Interface.SW_INTERNAL_EXCEPTION)
                }

                response = Response(response.data + receivedArray)
            } catch (ex: Exception) {
                Log.e(tag, "Unexpected error happened during command execution", ex)
                return Response(Interface.SW_INTERNAL_EXCEPTION)
            }

            apdu = when (response.sw1) {
                // Send the same command except for replacing Le with SW2 received.
                Iso7816.SW1_WRONG_LE -> Command(apdu.ins, apdu.p1, apdu.p2, apdu.data,
                        if (response.sw2 > 0x00) response.sw2 else 0x100)
                // Send GET RESPONSE command with Le specified by SW2.
                Iso7816.SW1_DATA_AVAILABLE -> Command(
                    Iso7816.INS_GET_RESPONSE,
                        le = if (response.sw2 > 0x00) response.sw2 else 0x100)
                else -> return response
            }
        } while (true)
    }

    override fun closeRemainingChannel() {
        if (telephony != null && !aid.contentEquals(BASIC_CHANNEL_AID)) {
            telephony.iccCloseLogicalChannel(channelId)
            Log.i(tag, "Closed the logical channel #$channelId")
            aid = BASIC_CHANNEL_AID
            channelId = BASIC_CHANNEL_ID
        }
    }

    override fun dispose() {
        closeRemainingChannel()
        Log.d(tag, "Disposed")
    }
}
