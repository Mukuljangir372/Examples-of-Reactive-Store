package com.mukul.jan.arc.store

import android.util.Log
import kotlinx.coroutines.CoroutineScope

/**
 * //--------------------------------------------------------------------//
 * Predefined Middlewares and EndConnectors
 */

abstract class BaseLogger<S : State, E : Event> {
    abstract val prefix: String
    abstract val logEvent: Boolean
    abstract val logState: Boolean

    fun log(name: String, event: E, state: S) {
        if (logEvent) Log.d(name, "$prefix ($name) $event")
        if (logState) Log.d(name, "$prefix ($name) $state")
    }
}

class LoggerMiddleware<S : State, E : Event> constructor(
    override val prefix: String,
    override val logEvent: Boolean = true,
    override val logState: Boolean = true
) : Middleware<S, E>, BaseLogger<S, E>() {
    override fun invoke(scope: CoroutineScope, store: Store<S, E>, event: E): E {
        log(name = this::class.java.simpleName, event = event, state = store.state())
        return event
    }
}

class LoggerEndConnector<S : State, E : Event> constructor(
    override val prefix: String,
    override val logEvent: Boolean = true,
    override val logState: Boolean = true
) : EndConnector<S, E>, BaseLogger<S, E>() {
    override fun invoke(scope: CoroutineScope, store: Store<S, E>, event: E, state: S): S {
        log(name = this::class.java.simpleName, event = event, state = state)
        return state
    }
}



