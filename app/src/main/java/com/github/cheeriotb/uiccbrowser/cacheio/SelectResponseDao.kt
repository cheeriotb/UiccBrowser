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
interface SelectResponseDao {
    @Insert
    suspend fun insert(selectResponse: SelectResponse)
    @Query("select * from response where icc_id = :iccId and aid = :aid and path = :path and file_id = :fileId")
    suspend fun get(iccId: String, aid: String, path: String, fileId: String): SelectResponse
    @Query("select * from response where icc_id = :iccId and aid = :aid and path = :path ORDER BY file_id ASC")
    suspend fun getAll(iccId: String, aid: String, path: String): List<SelectResponse>
    @Query("delete from response")
    suspend fun deleteAll()
}
