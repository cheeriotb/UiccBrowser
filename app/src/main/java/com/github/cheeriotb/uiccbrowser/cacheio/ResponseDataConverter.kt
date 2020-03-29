/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.cacheio

import androidx.room.TypeConverter
import com.github.cheeriotb.uiccbrowser.util.byteArrayToHexString
import com.github.cheeriotb.uiccbrowser.util.hexStringToByteArray

class ResponseDataConverter {
    @TypeConverter
    fun byteArrayDataToString(data: ByteArray) = byteArrayToHexString(data)
    @TypeConverter
    fun stringDataToByteArray(data: String) = hexStringToByteArray(data)
}
