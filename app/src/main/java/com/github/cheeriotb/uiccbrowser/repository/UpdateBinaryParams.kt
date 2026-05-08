/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.repository

data class UpdateBinaryParams(
    val fileId: FileId,
    val offset: Int = 0,
    val data: ByteArray
) {
    class Builder(
        private var fileId: FileId,
        private var offset: Int = 0,
        private var data: ByteArray = ByteArray(0)
    ) {
        fun fileId(fileId: FileId) = apply { this.fileId = fileId }
        fun offset(offset: Int) = apply { this.offset = offset }
        fun data(data: ByteArray) = apply { this.data = data }
        fun build() = UpdateBinaryParams(fileId, offset, data)
    }
}
