package com.mukul.jan.arc

import com.mukul.jan.arc.architecture.AppEvent
import com.mukul.jan.arc.architecture.AppState
import com.mukul.jan.arc.architecture.ExampleStore
import org.junit.Assert
import org.junit.Test

class StoreTest {
    @Test
    fun store_event() {
        val store = ExampleStore()
        store.dispatch(AppEvent.NameChange1(name = "event 1"))
        store.dispatch(AppEvent.NameChange2(name = "event 2"))
        store.dispatch(AppEvent.NameChange3(name = "event 3"))


    }
}