/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.cacheio

interface SelectResponseDataSource {
    suspend fun insert(selectResponse: SelectResponse)
    suspend fun get(iccId: String, aid: String, path: String, fileId: String): SelectResponse
    suspend fun getAll(iccId: String, aid: String, path: String): List<SelectResponse>
    suspend fun delete(iccId: String)
    suspend fun deleteAll()
}
