/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.element.ef

import android.content.res.Resources
import com.github.cheeriotb.uiccbrowser.R
import com.github.cheeriotb.uiccbrowser.element.Element

// ETSI TS 131 102, clause 4.6.4.
class McsEfDecoders {
    companion object {
        /** Decodes EFMST coding and service availability bits. */
        fun decodeMst(resources: Resources, bytes: ByteArray): Element? =
                TelecomSubDfDecoderUtils.decodeServiceTable(
                        resources, bytes, R.string.ef_mst_label,
                        listOf(
                                R.string.mcs_mcptt_ue_config_service_label,
                                R.string.mcs_mcptt_user_profile_service_label,
                                R.string.mcs_group_config_service_label,
                                R.string.mcs_mcptt_service_config_service_label,
                                R.string.mcs_ue_initial_config_service_label,
                                R.string.mcs_mcdata_ue_config_service_label,
                                R.string.mcs_mcdata_user_profile_service_label,
                                R.string.mcs_mcdata_service_config_service_label,
                                R.string.mcs_mcvideo_ue_config_service_label,
                                R.string.mcs_mcvideo_user_profile_service_label,
                                R.string.mcs_mcvideo_service_config_service_label),
                        R.string.mcs_management_objects_coding_label)
    }
}
