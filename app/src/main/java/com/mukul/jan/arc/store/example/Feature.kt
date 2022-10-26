package com.mukul.jan.arc.store.example

import com.mukul.jan.arc.store.*
import com.mukul.jan.arc.store.feature.Feature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DeleteUserFeature(
    private val scope: CoroutineScope,
    private val deleteUsersUsecase: DeleteUserUsecase,
) : Feature<DeleteUserFeature.FeatureState, DeleteUserFeature.FeatureEvent>(
    initialState = FeatureState.idle,
    coroutineScope = scope,
    storeKey = storeKey(DeleteUserFeature::class.java),
    reducer = FeatureReducer(),
) {
    sealed class FeatureEvent : Event {
        data class DeleteUser(
            val id: Int,
            val deleted: Boolean,
        ) : FeatureEvent()
    }

    data class FeatureState(
        val loading: Boolean,
        val userDeleted: Boolean,
        val deletedUser: Int
    ) : State {
        companion object {
            val idle = FeatureState(
                loading = false,
                userDeleted = false,
                deletedUser = 0
            )
        }
    }

    class FeatureReducer : Reducer<FeatureState, FeatureEvent> {
        override fun invoke(
            store: Store<FeatureState, FeatureEvent>,
            event: FeatureEvent
        ): FeatureState {
            return when (event) {
                is FeatureEvent.DeleteUser -> {
                    store.state().copy(
                        loading = !event.deleted,
                        deletedUser = event.id,
                        userDeleted = event.deleted
                    )
                }
            }
        }
    }

    operator fun invoke(id: Int) {
        dispatch(
            FeatureEvent.DeleteUser(
                id = id,
                deleted = false
            )
        )
        val deletedUserId = deleteUsersUsecase.invoke(id)
        dispatch(
            FeatureEvent.DeleteUser(
                id = deletedUserId,
                deleted = true
            )
        )
    }
}


class GetUsersFeature(
    private val scope: CoroutineScope,
    private val getUsersUsecase: GetUsersUsecase,
) : Feature<GetUsersFeature.FeatureState, GetUsersFeature.FeatureEvent>(
    initialState = FeatureState.idle,
    coroutineScope = scope,
    storeKey = storeKey(GetUsersFeature::class.java),
    reducer = FeatureReducer(),
    middleware = listOf(
        LoggerMiddleware(
            prefix = className(GetUsersFeature::class.java)
        )
    ),
    endConnector = listOf(
        LoggerEndConnector(
            prefix = className(GetUsersFeature::class.java)
        )
    )
) {
    sealed class FeatureEvent : Event {
        data class InsertUsers(
            val users: List<User>
        ) : FeatureEvent()

        data class Loading(
            val loading: Boolean
        ) : FeatureEvent()

        object OpenAlertDialog : FeatureEvent()
    }

    data class FeatureState(
        val loading: Boolean,
        val users: List<User>
    ) : State {
        companion object {
            val idle = FeatureState(
                loading = false,
                users = emptyList()
            )
        }
    }

    class FeatureReducer : Reducer<FeatureState, FeatureEvent> {
        override fun invoke(
            store: Store<FeatureState, FeatureEvent>,
            event: FeatureEvent
        ): FeatureState {
            return when (event) {
                is FeatureEvent.InsertUsers -> {
                    store.state().copy(
                        loading = false,
                        users = event.users
                    )
                }
                is FeatureEvent.Loading -> {
                    store.state().copy(
                        loading = event.loading
                    )
                }
                else -> store.state()
            }
        }
    }

    operator fun invoke() = scope.launch {
        dispatch(FeatureEvent.Loading(loading = true))
        val users = getUsersUsecase.invoke()
        dispatch(FeatureEvent.InsertUsers(users = users))
        dispatch(FeatureEvent.OpenAlertDialog)
    }
}
