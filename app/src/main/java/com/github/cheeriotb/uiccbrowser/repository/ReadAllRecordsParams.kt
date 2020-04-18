/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.repository

data class ReadAllRecordsParams(
    val fileId: FileId,
    val recordSize: Int = 0x100,
    val numberOfRecords: Int = 1
) {
    class Builder(
        private var fileId: FileId,
        private var recordSize: Int = 0x100,
        private var numberOfRecords: Int = 1
    ) {
        fun fileId(fileId: FileId) = apply { this.fileId = fileId }
        fun recordSize(recordSize: Int) = apply { this.recordSize = recordSize }
        fun numberOfRecords(numberOfRecords: Int) = apply { this.numberOfRecords = numberOfRecords }
        fun build() = ReadAllRecordsParams(fileId, recordSize, numberOfRecords)
    }
}
