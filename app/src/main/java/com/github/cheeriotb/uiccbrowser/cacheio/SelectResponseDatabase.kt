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

@Database(entities = [SelectResponse::class], version = 1, exportSchema = false)
abstract class SelectResponseDatabase : RoomDatabase() {
    abstract fun getDao(): SelectResponseDao

    companion object {
        @Volatile
        private var instance: SelectResponseDatabase? = null

        fun from(context: Context, forTest: Boolean = false): SelectResponseDatabase {
            if (forTest) {
                return Room.inMemoryDatabaseBuilder(context.applicationContext,
                        SelectResponseDatabase::class.java).build()
            }
            return instance ?: synchronized(this) {
                val database = Room.databaseBuilder(context.applicationContext,
                        SelectResponseDatabase::class.java, "response")
                        .fallbackToDestructiveMigration().build()
                instance = database
                database
            }
        }
    }
}
