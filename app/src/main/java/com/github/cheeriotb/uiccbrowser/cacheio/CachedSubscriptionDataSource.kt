/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.cacheio

interface CachedSubscriptionDataSource {
    suspend fun insert(cachedSubscription: CachedSubscription)
    suspend fun get(iccId: String): CachedSubscription
    suspend fun getAll(): List<CachedSubscription>
    suspend fun delete(iccId: String)
    suspend fun deleteAll()
}
