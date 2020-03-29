/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.cacheio

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.TypeConverters

@Entity(tableName = "response", primaryKeys = ["icc_id", "aid", "path", "file_id"])
@TypeConverters(ResponseDataConverter::class)
data class SelectResponse(
    @ColumnInfo(name = "icc_id") val iccId: String,
    val aid: String,
    val path: String,
    @ColumnInfo(name = "file_id") val fileId: String,
    val data: ByteArray,
    val sw: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SelectResponse

        if (iccId != other.iccId) return false
        if (aid != other.aid) return false
        if (path != other.path) return false
        if (fileId != other.fileId) return false
        if (!data.contentEquals(other.data)) return false
        if (sw != other.sw) return false

        return true
    }

    override fun hashCode(): Int {
        var result = iccId.hashCode()
        result = 31 * result + aid.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + fileId.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + sw
        return result
    }
}
