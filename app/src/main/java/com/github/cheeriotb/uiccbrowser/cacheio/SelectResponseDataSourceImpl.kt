/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.cacheio

import android.content.Context

class SelectResponseDataSourceImpl private constructor(
    private val dao: SelectResponseDao
) : SelectResponseDataSource {

    companion object {
        fun from(context: Context): SelectResponseDataSource {
            return SelectResponseDataSourceImpl(SelectResponseDatabase.from(context).getDao())
        }
    }

    override suspend fun insert(selectResponse: SelectResponse) {
        dao.insert(selectResponse)
    }

    override suspend fun get(
        iccId: String,
        aid: String,
        path: String,
        fileId: String
    ): SelectResponse {
        return dao.get(iccId, aid, path, fileId)
    }

    override suspend fun getAll(
        iccId: String,
        aid: String,
        path: String
    ): List<SelectResponse> {
        return dao.getAll(iccId, aid, path)
    }

    override suspend fun delete(iccId: String) {
        dao.delete(iccId)
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }
}
