/*
 *  Copyright (C) 2020 Cheerio <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.uiccbrowser.repository

import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import com.github.cheeriotb.uiccbrowser.cacheio.CachedSubscription
import com.github.cheeriotb.uiccbrowser.cacheio.CachedSubscriptionDataSource
import com.github.cheeriotb.uiccbrowser.cacheio.SelectResponseDataSource
import com.github.cheeriotb.uiccbrowser.cardio.Command
import com.github.cheeriotb.uiccbrowser.cardio.Interface
import com.github.cheeriotb.uiccbrowser.cardio.Iso7816
import com.github.cheeriotb.uiccbrowser.cardio.Response
import com.github.cheeriotb.uiccbrowser.util.hexStringToByteArray
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class CardRepositoryUnitTest {
    @Rule @JvmField
    val grantPermissionRule: GrantPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.READ_PHONE_STATE)
    @Mock
    private lateinit var cardIoMock: Interface
    @Mock
    private lateinit var cacheIoMock: SelectResponseDataSource
    @Mock
    private lateinit var subscriptionIoMock: CachedSubscriptionDataSource

    private lateinit var repository: CardRepository

    companion object {
        private const val FCP = "621E8202412183022FE2A506C00100CA01808A01058B032F06048002000A8800"
        private const val SW = "9000"
        private const val RESPONSE_FCP = FCP + SW
        private const val ICCID = "8988211000000282106F"
        private const val RESPONSE_ICCID = "988812010000202801F6$SW"
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        val target = CardRepository.from(ApplicationProvider.getApplicationContext(), 0)
        assertThat(target).isNotNull()
        repository = target!!
        ReflectionHelpers.setField(repository, "cardIo", cardIoMock)
        ReflectionHelpers.setField(repository, "cacheIo", cacheIoMock)
        ReflectionHelpers.setField(repository, "subscriptionIo", subscriptionIoMock)

        `when`(cardIoMock.openChannel(Interface.BASIC_CHANNEL_AID))
                .thenReturn(Interface.OpenChannelResult.SUCCESS)
        `when`(cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
                0x08 /* Select by path from MF */, 0x04 /* Return FCP template */,
                hexStringToByteArray(CardRepository.EF_ICCID))))
                .thenReturn(Response(hexStringToByteArray(RESPONSE_FCP)))
        `when`(cardIoMock.transmit(Command(Iso7816.INS_READ_BINARY, 0x00, 0x00, 0x100)))
                .thenReturn(Response(hexStringToByteArray(RESPONSE_ICCID)))

        runBlocking {
            `when`(subscriptionIoMock.get(ICCID)).thenReturn(CachedSubscription(ICCID, "USIM"))
        }
    }

    @After
    fun tearDown() {
        if (::repository.isInitialized) {
            repository.dispose()
        }
    }

    @Test
    fun from() {
        assertThat(CardRepository.from(ApplicationProvider.getApplicationContext(), 0))
                .isEqualTo(repository)
    }

    @Test
    fun initialize_failure_openChannel() = runBlocking {
        `when`(cardIoMock.openChannel(Interface.BASIC_CHANNEL_AID))
                .thenReturn(Interface.OpenChannelResult.GENERIC_FAILURE)
        assertThat(repository.initialize()).isEqualTo(false)
        assertThat(repository.isAccessible).isEqualTo(false)
        assertThat(repository.isCached).isEqualTo(false)
    }

    @Test
    fun initialize_failure_selectIccId() = runBlocking {
        `when`(cardIoMock.transmit(Command(Iso7816.INS_SELECT_FILE,
                0x08 /* Select by path from MF */, 0x04 /* Return FCP template */,
                hexStringToByteArray(CardRepository.EF_ICCID))))
                .thenReturn(Response(hexStringToByteArray("6F00")))
        assertThat(repository.initialize()).isEqualTo(false)
        assertThat(repository.isAccessible).isEqualTo(false)
        assertThat(repository.isCached).isEqualTo(false)
    }

    @Test
    fun initialize_failure_readIccId() = runBlocking {
        `when`(cardIoMock.transmit(Command(Iso7816.INS_READ_BINARY, 0x00, 0x00, 0x100)))
                .thenReturn(Response(hexStringToByteArray("6F00")))
        assertThat(repository.initialize()).isEqualTo(false)
        assertThat(repository.isAccessible).isEqualTo(false)
        assertThat(repository.isCached).isEqualTo(false)
    }

    @Test
    fun initialize_notCached() = runBlocking {
        `when`(subscriptionIoMock.get(ICCID)).thenReturn(null)
        assertThat(repository.initialize()).isEqualTo(true)
        assertThat(repository.isAccessible).isEqualTo(true)
        assertThat(repository.isCached).isEqualTo(false)
    }
}
