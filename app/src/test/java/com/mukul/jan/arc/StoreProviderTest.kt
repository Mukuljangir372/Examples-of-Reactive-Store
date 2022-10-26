package com.mukul.jan.arc

import com.mukul.jan.arc.store.StoreProvider
import com.mukul.jan.arc.store.example.HomeStore
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock


class StoreProviderTest {

    private lateinit var store: HomeStore
    private lateinit var storeProvider: StoreProvider

    @Before
    fun setup() {
        store = mock()
        storeProvider = mock()
    }

    @Test
    fun `StoreProvider must throw exception if store already exits with same key`() {

    }

}