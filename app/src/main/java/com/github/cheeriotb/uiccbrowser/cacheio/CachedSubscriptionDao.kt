/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.cacheio

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CachedSubscriptionDao {
    @Insert
    suspend fun insert(cachedSubscription: CachedSubscription)
    @Query("SELECT * FROM subscription WHERE icc_id = :iccId")
    suspend fun get(iccId: String): CachedSubscription?
    @Query("SELECT * FROM subscription")
    suspend fun getAll(): List<CachedSubscription>
    @Query("DELETE FROM subscription WHERE icc_id = :iccId")
    suspend fun delete(iccId: String)
    @Query("DELETE FROM subscription")
    suspend fun deleteAll()
}
