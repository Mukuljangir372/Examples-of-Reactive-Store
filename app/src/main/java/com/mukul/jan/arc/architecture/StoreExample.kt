package com.mukul.jan.arc.architecture

import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlin.random.Random


/**
 * Unidirectional Reactive MVI Architecture
 */
data class AppState(
    val name1: String,
    val name2: String,
    val name3: String,
): State {
    companion object {
        val EMPTY  = AppState(
            name1 = "",
            name2 = "",
            name3 = ""
        )
    }
}

sealed class AppEvent : Event {
    data class NameChange1(val name: String): AppEvent()
    data class NameChange2(val name: String): AppEvent()
    data class NameChange3(val name: String): AppEvent()
}

class AppReducer: Reducer<AppState, AppEvent> {
    override fun invoke(store: Store<AppState, AppEvent>, event: AppEvent): AppState {
        return when(event) {
            is AppEvent.NameChange1 -> {
                store.state().copy(name1 = event.name)
            }
            is AppEvent.NameChange2 -> {
                store.state().copy(name2 = event.name)
            }
            is AppEvent.NameChange3 -> {
                store.state().copy(name3 = event.name)
            }
        }
    }
}

open class ExampleStore: Store<AppState, AppEvent>(
    initialState = AppState.EMPTY,
    reducer = AppReducer(),
    endConnector = listOf(LoggerEndConnector(prefix = ""))
)

class ExampleViewModel(): ViewModel() {
    companion object {
        private const val STORE = "ExampleViewModel.store"
    }
    private val store = createStore(STORE,ExampleStore())
    fun store() = store
    fun state() = store.state()

    fun changeUserName() {
        viewModelScope.launch {

            val random1 = Random.nextInt()
            val newName1 = "mukul1 $random1"
            store.dispatch(AppEvent.NameChange1(name = newName1))

//            val random2 = Random.nextInt()
//            val newName2 = "mukul2 $random2"
//            store.dispatch(AppEvent.NameChange2(name = newName2))

//            val random3 = Random.nextInt()
//            val newName3 = "mukul3 $random3"
//            store.dispatch(AppEvent.NameChange3(name = newName3))
        }
    }
}















