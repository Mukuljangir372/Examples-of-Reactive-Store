package com.mukul.jan.arc.architecture

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap

interface Event
interface State

// Reducer species how state changes after event has triggered
interface Reducer<S : State, E : Event> {
    fun invoke(store: Store<S, E>, event: E): S
}

// Middleware sits between dispatching the event and the moment it goes to reducer
// It can a logger or analytics middleware
interface Middleware<S : State, E : Event> {
    fun invoke(store: Store<S, E>, event: E): E
}

// EndConnector called after reducer
interface EndConnector<S : State, E : Event> {
    fun invoke(store: Store<S, E>, event: E): S
}

class Dispatcher<S : State, E : Event>(
    private val scope: CoroutineScope,
    private val exceptionHandler: CoroutineExceptionHandler,
    private val reducer: Reducer<S, E>,
    private val middleware: List<Middleware<S, E>>,
    private val endConnector: List<EndConnector<S, E>>,
) {
    fun dispatch(store: Store<S, E>, event: E) {
        val finalEvent = if (middleware.isNotEmpty()) {
            var currentEvent: E = event
            for (single in middleware) {
                currentEvent = single.invoke(
                    store = store,
                    event = currentEvent
                )
            }
            currentEvent
        } else {
            event
        }

        val state = reducer.invoke(
            store = store,
            event = finalEvent
        )

        val finalState = if (endConnector.isNotEmpty()) {
            var currentState: S = state
            for (single in endConnector) {
                currentState = single.invoke(
                    store = store,
                    event = finalEvent
                )
            }
            currentState
        } else {
            state
        }
        store.updateState(finalState)
    }
}

abstract class Store<S : State, E : Event>(
    private val initialState: S,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val reducer: Reducer<S, E>,
    private val middleware: List<Middleware<S, E>> = emptyList(),
    private val endConnector: List<EndConnector<S, E>> = emptyList()
) {
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        scope.cancel()
        throw throwable
    }

    private val dispatcher = Dispatcher(
        scope = scope,
        exceptionHandler = exceptionHandler,
        reducer = reducer,
        middleware = middleware,
        endConnector = endConnector
    )

    private val reactiveState = Channel<S>(Channel.CONFLATED)
    fun receive() = reactiveState.receiveAsFlow()

    fun observe(block: (S) -> Unit): Store<S, E> {
        scope.launch {
            receive().collectLatest {
                block(it)
            }
        }
        return this
    }

    @Volatile
    private var mutableState: S = initialState

    fun state() = mutableState

    @Synchronized
    fun updateState(newState: S) {
        mutableState = newState
        runBlocking {
            reactiveState.send(newState)
        }
    }

    fun dispatch(event: E) {
        dispatcher.dispatch(
            store = this,
            event = event
        )
    }
}

/**
 * Store Provider
 */
class StoreProvider private constructor() {

    companion object {
        @Volatile
        private var instance: StoreProvider? = null
        fun getInstance(): StoreProvider {
            if (instance != null) return instance!!

            synchronized(this) {
                if (instance == null) {
                    instance = StoreProvider()
                }
            }
            return instance!!
        }
    }

    private val hashMap = ConcurrentHashMap<String, Store<*, *>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Store<*, *>> get(key: String, default: T): T {
        checkForKey(key)
        val store = hashMap[key]
        if (store != null) {
            return store as T
        } else {
            hashMap[key] = default
        }
        return default
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Store<*, *>> create(key: String, default: T): T {
        checkForKey(key)
        hashMap[key] = default
        return default
    }

    private fun checkForKey(key: String) {
        check(
            value = key.isNotEmpty(),
            lazyMessage = {
                "Key can't be empty or null"
            }
        )
    }
}

/**
 * FEATURE
 */

open class Feature<S : State, E : Event>(
    private val initialState: S,
    private val reducer: Reducer<S, E>,
    private val scope: CoroutineScope,
) {
    inner class FeatureStore : Store<S, E>(
        initialState = initialState,
        reducer = reducer,
        scope = scope
    )

    private val store = FeatureStore()
    fun store() = store

    protected fun dispatch(event: E) {
        store.dispatch(event)
    }
}

/**
 * EXTS
 */
fun <T : Store<*, *>> getStore(key: String, default: T): T {
    val provider = StoreProvider.getInstance()
    return provider.get(key, default)
}

fun <T : Store<*, *>> createStore(key: String, default: T): T {
    val provider = StoreProvider.getInstance()
    return provider.create(key, default)
}

fun <S, E, T : Feature<S, E>> T.consume(block: (S) -> Unit): T {
    store().observe {
        block(it)
    }
    return this
}

/**
 * Predefined Middlewares and EndConnectors
 */

abstract class BaseLogger<S : State, E : Event> {
    abstract val prefix: String
    abstract val logEvent: Boolean
    abstract val logState: Boolean

    fun log(name: String, event: E, state: S) {
        if (logEvent) Log.d(name, "$prefix $event")
        if (logState) Log.d(name,"$prefix $state")
    }
}

class LoggerMiddleware<S : State, E : Event> constructor(
    override val prefix: String,
    override val logEvent: Boolean = true,
    override val logState: Boolean = true
) : Middleware<S, E>, BaseLogger<S, E>() {
    override fun invoke(store: Store<S, E>, event: E): E {
        log(name = this::class.java.simpleName, event = event, state = store.state())
        return event
    }
}

class LoggerEndConnector<S : State, E : Event> constructor(
    override val prefix: String,
    override val logEvent: Boolean = true,
    override val logState: Boolean = true
) : EndConnector<S, E>, BaseLogger<S, E>() {
    override fun invoke(store: Store<S, E>, event: E): S {
        log(name = this::class.java.simpleName, event = event, state = store.state())
        return store.state()
    }
}



















