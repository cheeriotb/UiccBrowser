/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.cacheio

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CachedSubscription::class], version = 1, exportSchema = false)
abstract class CachedSubscriptionDatabase : RoomDatabase() {
    abstract fun getDao(): CachedSubscriptionDao

    companion object {
        @Volatile
        private var instance: CachedSubscriptionDatabase? = null

        fun from(context: Context, forTest: Boolean = false): CachedSubscriptionDatabase {
            if (forTest) {
                return Room.inMemoryDatabaseBuilder(context.applicationContext,
                        CachedSubscriptionDatabase::class.java).build()
            }
            return instance ?: synchronized(this) {
                val database = Room.databaseBuilder(context.applicationContext,
                        CachedSubscriptionDatabase::class.java, "subscription")
                        .fallbackToDestructiveMigration().build()
                instance = database
                database
            }
        }
    }
}
