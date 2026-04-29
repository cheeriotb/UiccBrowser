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

class GetFileListUseCase(private val context: Context) {

    private data class FileNode(
        val name: String,
        val id: String,
        val description: String,
        val hasChildren: Boolean
    )

    /**
     * Returns the root node metadata (name and id) from the given JSON resource.
     * Used to build the action bar title for the root level.
     */
    fun parseRootMeta(@RawRes rawResId: Int): Pair<String, String> {
        val root = JSONObject(
            context.resources.openRawResource(rawResId).bufferedReader().use { it.readText() }
        )
        val name = root.getString("name")
        val id = if (root.has("id")) root.getString("id") else ""
        return name to id
    }

    /**
     * Resolves the list of [FileEntry] items at the given [parentPath] by:
     * 1. Parsing the JSON layout resource to find nodes at [parentPath].
     * 2. Querying the FCP cache for each node.
     * 3. Returning only entries whose SELECT response was successful (isOk).
     *
     * The [parentPath] uses the same concatenated format as [FileId] (e.g. "7FFF5FC0").
     * The root path for MF is "" and for ADFs is "7FFF".
     */
    suspend fun execute(
        @RawRes rawResId: Int,
        slotId: Int,
        aid: String,
        parentPath: String
    ): List<FileEntry> {
        val repo = CardRepository.from(context, slotId) ?: return emptyList()
        if (!repo.isAccessible) return emptyList()

        val root = JSONObject(
            context.resources.openRawResource(rawResId).bufferedReader().use { it.readText() }
        )
        val rootPath = if (aid == FileId.AID_NONE) FileId.PATH_MF else FileId.PATH_ADF
        val nodes = findNodesAt(root.getJSONArray("files"), rootPath, parentPath)
            ?: return emptyList()

        return nodes.mapNotNull { node ->
            val fileId = FileId.Builder()
                .aid(aid)
                .path(parentPath)
                .fileId(node.id)
                .build()
            val results = repo.queryFileControlParameters(fileId)
            if (results.isNotEmpty() && results.first().isOk) {
                FileEntry(
                    name = node.name,
                    id = node.id,
                    description = node.description,
                    isDirectory = node.hasChildren
                )
            } else {
                null
            }
        }
    }

    private fun findNodesAt(
        filesArray: JSONArray,
        currentPath: String,
        targetPath: String
    ): List<FileNode>? {
        if (currentPath == targetPath) {
            return parseNodes(filesArray)
        }
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

    private fun parseNodes(filesArray: JSONArray): List<FileNode> {
        val nodes = mutableListOf<FileNode>()
        for (i in 0 until filesArray.length()) {
            val item = filesArray.getJSONObject(i)
            nodes.add(
                FileNode(
                    name = item.getString("name"),
                    id = item.getString("id"),
                    description = item.getString("description"),
                    hasChildren = item.has("files")
                )
            )
        }
        return nodes
    }
}
