/*
 *  Copyright (C) 2026 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.ui.efdetail

import androidx.test.core.app.ApplicationProvider
import com.github.cheeriotb.uiccbrowser.repository.FileId
import com.github.cheeriotb.uiccbrowser.util.hexStringToByteArray
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InfoViewModelUnitTest {

    private val application =
        ApplicationProvider.getApplicationContext<android.app.Application>()

    // EF DIR record: 61 12 4F 10 <16-byte USIM AID>
    private val efDirRecord =
        hexStringToByteArray("61124F10A0000000871002FFFFFFFFFFFFFFFFFF")
    private val efArrRecord =
        hexStringToByteArray("8001019000")

    @Test
    fun decode_efDir_setsElement() {
        val vm = InfoViewModel(application, FileId(FileId.AID_NONE, FileId.PATH_MF, FileId.EF_DIR))
        vm.decode(efDirRecord)
        assertThat(vm.element.value).isNotNull()
    }

    @Test
    fun decode_unregisteredEf_elementRemainsNull() {
        val iccidFileId = FileId(FileId.AID_NONE, FileId.PATH_MF, "2FE2")
        val vm = InfoViewModel(application, iccidFileId)
        vm.decode(byteArrayOf(0x01, 0x02, 0x03))
        assertThat(vm.element.value).isNull()
    }

    @Test
    fun decode_efArr_setsElement() {
        val vm = InfoViewModel(application, FileId(FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ARR))
        vm.decode(efArrRecord)
        assertThat(vm.element.value).isNotNull()
    }

    @Test
    fun decode_calledTwice_elementRemainsSet() {
        val vm = InfoViewModel(application, FileId(FileId.AID_NONE, FileId.PATH_MF, FileId.EF_DIR))
        vm.decode(efDirRecord)
        assertThat(vm.element.value).isNotNull()
        vm.decode(efDirRecord)
        assertThat(vm.element.value).isNotNull()
    }
}
