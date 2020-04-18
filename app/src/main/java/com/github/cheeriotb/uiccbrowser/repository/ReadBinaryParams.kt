/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.repository

data class ReadBinaryParams(
    val fileId: FileId,
    val offset: Int = 0,
    val size: Int = 0x100
) {
    class Builder(
        private var fileId: FileId,
        private var offset: Int = 0,
        private var size: Int = 0x100
    ) {
        fun fileId(fileId: FileId) = apply { this.fileId = fileId }
        fun offset(offset: Int) = apply { this.offset = offset }
        fun size(recordSize: Int) = apply { this.size = recordSize }
        fun build() = ReadBinaryParams(fileId, offset, size)
    }
}
