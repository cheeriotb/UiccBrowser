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
class FcpViewModelUnitTest {

    private val application =
        ApplicationProvider.getApplicationContext<android.app.Application>()

    // EF ICCID FCP (Transparent EF, file size 10)
    private val fcpBytes =
        hexStringToByteArray("621E8202412183022FE2A506C00100CA01808A01058B032F06048002000A8800")

    private val fileId = FileId(FileId.AID_NONE, FileId.PATH_MF, FileId.EF_ICCID)

    @Test
    fun decode_validFcp_setsElement() {
        val vm = FcpViewModel(application, fileId)
        vm.decode(fcpBytes)
        assertThat(vm.element.value).isNotNull()
    }

    @Test
    fun decode_invalidBytes_elementRemainsNull() {
        val vm = FcpViewModel(application, fileId)
        vm.decode(byteArrayOf(0x01, 0x02, 0x03))
        assertThat(vm.element.value).isNull()
    }

    @Test
    fun decode_calledTwice_elementRemainsSet() {
        val vm = FcpViewModel(application, fileId)
        vm.decode(fcpBytes)
        assertThat(vm.element.value).isNotNull()
        vm.decode(fcpBytes)
        assertThat(vm.element.value).isNotNull()
    }
}
