package com.mukul.jan.arc

import com.mukul.jan.arc.store.Store
import com.mukul.jan.arc.store.example.HomeEvent
import com.mukul.jan.arc.store.example.HomeState
import com.mukul.jan.arc.store.example.HomeStore
import com.mukul.jan.arc.store.getStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class StoreTest {

    private lateinit var store: Store<HomeState, HomeEvent>

    @Before
    fun setup() {
        store = getStore("test.store", HomeStore())
    }

    @Test
    fun `Observers must called after being observed`() = runTest {
        val event: HomeEvent.InsertUsers = mock()

        val observer1: Store.Observer<HomeState, HomeEvent> = mock()
        store.observe(type = Store.SubscriptionType.State, observer1)

        val observer2: Store.Observer<HomeState, HomeEvent> = mock()
        store.observe(type = Store.SubscriptionType.State, observer2)

        store.dispatch(event)
        advanceUntilIdle()

        verify(observer1, times(1)).onInvoke(any(), any())
        verify(observer2, times(1)).onInvoke(any(), any())
    }

    @Test
    fun `Observers must stop observing after unsubscribe it`() = runTest {
        val event: HomeEvent.InsertUsers = mock()

        val observer1: Store.Observer<HomeState, HomeEvent> = mock()
        val sub1 = store.observe(type = Store.SubscriptionType.State, observer1)

        val observer2: Store.Observer<HomeState, HomeEvent> = mock()
        val sub2 = store.observe(type = Store.SubscriptionType.State, observer2)
        sub2.unsubscribe()


        store.dispatch(event)
        advanceUntilIdle()

        verify(observer1, times(1)).onInvoke(any(), any())
        verify(observer2, times(1)).onInvoke(any(), any())
    }


}