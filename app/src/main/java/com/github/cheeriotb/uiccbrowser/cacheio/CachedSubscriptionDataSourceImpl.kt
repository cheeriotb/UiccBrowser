/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.cacheio

import android.content.Context

class CachedSubscriptionDataSourceImpl private constructor(
    private val dao: CachedSubscriptionDao
) : CachedSubscriptionDataSource {

    companion object {
        fun from(context: Context): CachedSubscriptionDataSource {
            return CachedSubscriptionDataSourceImpl(
                    CachedSubscriptionDatabase.from(context).getDao())
        }
    }

    override suspend fun insert(cachedSubscription: CachedSubscription) {
        return dao.insert(cachedSubscription)
    }

    override suspend fun get(iccId: String): CachedSubscription {
        return dao.get(iccId)
    }

    override suspend fun getAll(): List<CachedSubscription> {
        return dao.getAll()
    }

    override suspend fun delete(iccId: String) {
        dao.delete(iccId)
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }
}
