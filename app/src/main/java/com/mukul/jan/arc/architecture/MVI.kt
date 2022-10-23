package com.mukul.jan.arc.architecture

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.Random

data class User(
    val id: Int,
    val name: String
)

/**
 * MVI - Model + View + Intent
 * View - screen/activity/fragment/composable screen
 * Intent - user event/interaction
 * Model - Description for the UI (Its a kind of state for the UI)
 * e.g - user event trigger from the view(screen) and pass to presenter/viewmodel(any data holder)
 * and then, a intent returns the updated model.
 */

/*

//Intent or Event
interface HomeEvent {
    object ButtonClicked : HomeEvent
}

//Model
data class HomeModel(
    val isLoading: Boolean,
    val users: List<User>
) {
    companion object {
        val EMPTY = HomeModel(
            isLoading = false,
            users = listOf()
        )
    }
}

*/

// MVP
// Model + Intent(Event)
//Presenter - Dealing with events(intents) and returning the updated model(ui state)
class HomePresenter(
    private val mainScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
){

}


/**
 * MVI + Store(In place of normal state)
 */

//@ExperimentalCoroutinesApi
//class MainViewModel(
//    private val repository: MainRepository
//) : ViewModel() {
//
//    val userIntent = Channel<MainIntent>(Channel.UNLIMITED)
//    private val _state = MutableStateFlow<MainState>(MainState.Idle)
//    val state: StateFlow<MainState>
//        get() = _state
//
//    init {
//        handleIntent()
//    }
//
//    private fun handleIntent() {
//        viewModelScope.launch {
//            userIntent.consumeAsFlow().collect {
//                when (it) {
//                    is MainIntent.FetchUser -> fetchUser()
//                }
//            }
//        }
//    }
//
//    private fun fetchUser() {
//        viewModelScope.launch {
//            _state.value = MainState.Loading
//            _state.value = try {
//                MainState.Users(repository.getUsers())
//            } catch (e: Exception) {
//                MainState.Error(e.localizedMessage)
//            }
//        }
//    }
//}






























