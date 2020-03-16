/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.io

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
            if (aid.isNotEmpty()) {
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

        val p3 = if (command.lc > 0) command.lc else if (command.le > 0) command.le else CASE1_P3

        val builder = StringBuilder().append(byteArrayToHexString(command.data))
        if ((command.lc > 0) and (command.le > 0)) {
            // Le field of Case 4 command shall be appended to the end of data.
            builder.append(if (command.extended) extendedBytesToHexString(command.le)
                    else byteToHexString(command.le))
        }

        val response = if (aid.isNotEmpty()) {
            telephony.iccTransmitApduLogicalChannel(channelId, command.cla(channelId), command.ins,
                    command.p1, command.p2, p3, builder.toString())
        } else {
            telephony.iccTransmitApduBasicChannel(command.cla(channelId), command.ins, command.p1,
                    command.p2, p3, builder.toString())
        }

        return Response(hexStringToByteArray(response))
    }

    override fun closeRemainingChannel() {
        if (telephony != null && this.aid.isNotEmpty()) {
            telephony.iccCloseLogicalChannel(channelId)
            Log.i(tag, "Closed the logical channel #$channelId")
            this.aid = BASIC_CHANNEL_AID
            channelId = BASIC_CHANNEL_ID
        }
    }

    override fun dispose() {
        closeRemainingChannel()
        Log.d(tag, "Disposed")
    }
}
