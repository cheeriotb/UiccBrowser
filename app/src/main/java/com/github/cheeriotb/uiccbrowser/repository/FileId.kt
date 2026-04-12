/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.repository

data class FileId(
    val aid: String = AID_NONE,
    val path: String = PATH_MF,
    val fileId: String = FID_ALMIGHTY
) {
    companion object {
        const val AID_NONE = ""
        const val PATH_MF = ""
        const val PATH_ADF = "7FFF"
        const val FID_ALMIGHTY = ""

        const val MF = "3F00"

        const val EF_DIR = "2F00"
        const val EF_ICCID = "2FE2"
    }

    class Builder(
        private var aid: String = AID_NONE,
        private var path: String = PATH_MF,
        private var fileId: String = FID_ALMIGHTY
    ) {
        fun aid(aid: String) = apply { this.aid = aid }
        fun path(path: String) = apply { this.path = path }
        fun fileId(fileId: String) = apply { this.fileId = fileId }
        fun build() = FileId(aid, path, fileId)
    }
}
