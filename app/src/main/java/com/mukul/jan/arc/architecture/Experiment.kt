package com.mukul.jan.arc.architecture

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

interface BaseHomeInteractor {
    fun deleteUserClicked()
    fun fetchUsersClicked()
}

class HomeInteractor(
    private val viewModel: HomeViewModel
) : BaseHomeInteractor {
    override fun deleteUserClicked() {
        viewModel.deleteUser(id = 1)
    }

    override fun fetchUsersClicked() {
        viewModel.insertUser()
    }
}

class HomeStore : Store<HomeState, HomeEvent>(
    initialState = HomeState.Idle,
    reducer = HomeReducer(),
    middleware = listOf(
        LoggerMiddleware(
            prefix = "HomeStore"
        )
    ),
    endConnector = listOf(
        LoggerEndConnector(
            prefix = "HomeStore"
        )
    )
)

interface HomeEvent : Event {
    data class InsertUsers(val users: List<User>) : HomeEvent
    data class RemoveUser(val id: Int) : HomeEvent
}

data class HomeState(
    val isLoading: Boolean,
    val users: List<User>
) : State {
    companion object {
        val Idle = HomeState(
            isLoading = false,
            users = listOf()
        )
    }
}

class HomeReducer : Reducer<HomeState, HomeEvent> {
    override fun invoke(store: Store<HomeState, HomeEvent>, event: HomeEvent): HomeState {
        return when (event) {
            is HomeEvent.InsertUsers -> {
                val users = store.state().users
                store.state().copy(users = users + event.users)
            }
            is HomeEvent.RemoveUser -> {
                val users = store.state().users.toMutableList()
                users.removeIf { it.id == event.id }
                store.state().copy(users = users)
            }
            else -> store.state()
        }
    }
}

class HomeViewModel() : ViewModel() {

    companion object {
        private const val storeKey = "HomeViewModel.store"
    }

    private val store = getStore(storeKey, HomeStore())

    fun insertUser() {
        GetUsersFeature(
            scope = viewModelScope,
            getUsersUsecase = GetUsersUsecase()
        ).observeState {
            if (!it.loading && it.users.isNotEmpty()) {
                store.dispatch(HomeEvent.InsertUsers(users = it.users))
            }
        }.invoke()
    }

    fun deleteUser(id: Int) {
        DeleteUserFeature(
            scope = viewModelScope,
            deleteUsersUsecase = DeleteUserUsecase()
        ).observeState {
            if (it.userDeleted) {
                store.dispatch(HomeEvent.RemoveUser(id = it.deletedUser))
            }
        }.invoke(id = 1)
    }


}














