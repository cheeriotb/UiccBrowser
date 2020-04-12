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

@Entity(tableName = "subscription", primaryKeys = ["icc_id"])
data class CachedSubscription(
    @ColumnInfo(name = "icc_id") val iccId: String,
    val name: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CachedSubscription

        if (iccId != other.iccId) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = iccId.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}
