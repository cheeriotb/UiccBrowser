/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.repository

data class ReadRecordParams(
    val fileId: FileId,
    val recordNo: Int = 1,
    val recordSize: Int = 0x100
) {
    class Builder(
        private var fileId: FileId,
        private var recordNo: Int = 1,
        private var recordSize: Int = 0x100
    ) {
        fun fileId(fileId: FileId) = apply { this.fileId = fileId }
        fun recordNo(recordNo: Int) = apply { this.recordNo = recordNo }
        fun recordSize(recordSize: Int) = apply { this.recordSize = recordSize }
        fun build() = ReadRecordParams(fileId, recordNo, recordSize)
    }
}
