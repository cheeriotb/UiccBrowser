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
    @Query("SELECT * FROM response"
            + " WHERE icc_id = :iccId AND aid = :aid AND path = :path AND file_id = :fileId")
    suspend fun get(iccId: String, aid: String, path: String, fileId: String): SelectResponse?
    @Query("SELECT * FROM response "
            + "WHERE icc_id = :iccId AND aid = :aid AND path = :path AND LENGTH(data) > 0 "
            + "ORDER BY file_id ASC")
    suspend fun getAll(iccId: String, aid: String, path: String): List<SelectResponse>
    @Query("DELETE FROM response WHERE icc_id = :iccId")
    suspend fun delete(iccId: String)
    @Query("DELETE FROM response")
    suspend fun deleteAll()
}
