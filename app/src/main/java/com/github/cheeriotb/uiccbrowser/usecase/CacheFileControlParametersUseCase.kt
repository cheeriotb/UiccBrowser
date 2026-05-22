/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.usecase

import android.content.Context
import androidx.annotation.RawRes
import com.github.cheeriotb.uiccbrowser.repository.CardRepository
import com.github.cheeriotb.uiccbrowser.repository.FileId
import org.json.JSONArray
import org.json.JSONObject

class CacheFileControlParametersUseCase(private val context: Context) {

    /**
     * Reads the given raw JSON resource and caches FCP for each listed file via CardRepository.
     *
     * The JSON "files" array may be nested: items with a "files" key are DFs whose children
     * are processed recursively, building the path as parentPath + fileId at each level.
     * Each file is skipped if already present in the DB (idempotent).
     *
     * @param rawResId  R.raw.* resource ID of the JSON file describing the files to cache
     * @param slotId    Target slot ID
     * @param aid       AID of the application (empty string for MF-level files).
     *                  Root path is derived from this: AID_NONE → PATH_MF, otherwise → PATH_ADF
     * @return true on success, false if caching could not be completed
     */
    suspend fun execute(
        @RawRes rawResId: Int,
        slotId: Int,
        aid: String = FileId.AID_NONE
    ): Boolean {
        val repo = CardRepository.from(context, slotId) ?: return false

        val rootPath = if (aid == FileId.AID_NONE) FileId.PATH_MF else FileId.PATH_ADF
        val fileIds = mutableListOf<FileId>()
        collectFileIds(parseJson(rawResId), aid, rootPath, fileIds)

        for (fileId in fileIds) {
            if (!repo.cacheFileControlParameters(fileId)) return false
        }

        return true
    }

    /**
     * Refreshes FCP cache for files directly under [parentPath].
     *
     * Existing SELECT responses for that directory are deleted first so that failed or stale
     * entries are probed again. Nested child directories are not refreshed here.
     */
    suspend fun refreshDirectory(
        @RawRes rawResId: Int,
        slotId: Int,
        aid: String,
        parentPath: String
    ): Boolean {
        val repo = CardRepository.from(context, slotId) ?: return false
        if (!repo.deleteFileControlParametersInDirectory(aid, parentPath)) return false

        val rootPath = if (aid == FileId.AID_NONE) FileId.PATH_MF else FileId.PATH_ADF
        val nodes = findNodesAt(parseJson(rawResId), rootPath, parentPath) ?: return false
        for (i in 0 until nodes.length()) {
            val node = nodes.getJSONObject(i)
            val fileId = FileId.Builder()
                .aid(aid)
                .path(parentPath)
                .fileId(node.getString("id"))
                .build()
            if (!repo.cacheFileControlParameters(fileId)) return false
        }

        return true
    }

    private fun collectFileIds(
        filesArray: JSONArray,
        aid: String,
        parentPath: String,
        result: MutableList<FileId>
    ) {
        for (i in 0 until filesArray.length()) {
            val item = filesArray.getJSONObject(i)
            val fileId = item.getString("id")
            result.add(
                FileId.Builder().aid(aid).path(parentPath).fileId(fileId).build()
            )
            if (item.has("files")) {
                collectFileIds(
                    item.getJSONArray("files"),
                    aid,
                    parentPath + fileId,
                    result
                )
            }
        }
    }

    private fun findNodesAt(
        filesArray: JSONArray,
        currentPath: String,
        targetPath: String
    ): JSONArray? {
        if (currentPath == targetPath) return filesArray
        for (i in 0 until filesArray.length()) {
            val item = filesArray.getJSONObject(i)
            if (item.has("files")) {
                val childPath = currentPath + item.getString("id")
                val result = findNodesAt(item.getJSONArray("files"), childPath, targetPath)
                if (result != null) return result
            }
        }
        return null
    }

    private fun parseJson(@RawRes rawResId: Int): JSONArray =
        JSONObject(
            context.resources.openRawResource(rawResId).bufferedReader().use { it.readText() }
        ).getJSONArray("files")
}
