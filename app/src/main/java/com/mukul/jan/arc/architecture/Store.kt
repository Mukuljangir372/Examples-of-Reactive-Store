package com.mukul.jan.arc.architecture

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.reflect.KClass

interface Event
interface State

// Reducer species how state changes after event has triggered
interface Reducer<S : State, E : Event> {
    fun invoke(store: Store<S, E>, event: E): S
}

// Middleware sits between dispatching the event and the moment it goes to reducer
// It can a logger or analytics middleware
interface Middleware<S : State, E : Event> {
    fun invoke(scope: CoroutineScope, store: Store<S, E>, event: E): E
}

// EndConnector called after reducer
interface EndConnector<S : State, E : Event> {
    fun invoke(scope: CoroutineScope, store: Store<S, E>, event: E, state: S): S
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
                    scope = scope,
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
                    scope = scope,
                    store = store,
                    event = finalEvent,
                    state = currentState
                )
            }
            currentState
        } else {
            state
        }
        store.updateState(finalState, finalEvent)
    }
}

abstract class Store<S : State, E : Event>(
    private val initialState: S,
    private val reducer: Reducer<S, E>,
    private val middleware: List<Middleware<S, E>> = emptyList(),
    private val endConnector: List<EndConnector<S, E>> = emptyList()
) {
    private val coroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val coroutineScope = CoroutineScope(coroutineDispatcher)
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        coroutineScope.cancel()
        throw throwable
    }

    private val dispatcher = Dispatcher(
        scope = coroutineScope,
        exceptionHandler = exceptionHandler,
        reducer = reducer,
        middleware = middleware,
        endConnector = endConnector
    )

    private val dispatchedEventsChannel = Channel<E>(Channel.CONFLATED)
    fun receiveDispatchedEvents() = dispatchedEventsChannel.receiveAsFlow()

    private fun sendDispatchedEvent(event: E) {
        runBlocking {
            dispatchedEventsChannel.send(event)
        }
    }

    fun dispatch(event: E) = coroutineScope.launch(exceptionHandler) {
        sendDispatchedEvent(event)
        dispatcher.dispatch(
            store = this@Store,
            event = event
        )
    }

    @Volatile
    private var mutableState: S = initialState
    fun state() = mutableState

    @Synchronized
    fun updateState(newState: S, event: E) {
        mutableState = newState
        sendState(newState)
        sendFinishedEvent(event)
    }

    private val stateChannel = Channel<S>(Channel.CONFLATED)
    fun receiveState() = stateChannel.receiveAsFlow()

    private fun sendState(state: S) {
        runBlocking {
            stateChannel.send(state)
        }
    }

    private val finishedEventsChannel = Channel<E>(Channel.CONFLATED)
    fun receiveFinishedEvents() = finishedEventsChannel.receiveAsFlow()

    private fun sendFinishedEvent(event: E) {
        runBlocking {
            finishedEventsChannel.send(event)
        }
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
    private val storeKey: String,
    private val reducer: Reducer<S, E>,
    private val middleware: List<Middleware<S, E>> = emptyList(),
    private val endConnector: List<EndConnector<S, E>> = emptyList(),
) {
    inner class FeatureStore : Store<S, E>(
        initialState = initialState,
        reducer = reducer,
        middleware = middleware,
        endConnector = endConnector
    )

    private val store = getStore(storeKey, FeatureStore())
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

fun storeKey(target: Class<*>): String {
    return target.name + ".store"
}

fun className(target: Class<*>): String {
    return target.simpleName
}

/**
 * EXTS FOR STORE
 */

fun <S, E, T : Store<S, E>> T.observeState(
    scope: CoroutineScope, block: (S) -> Unit
): T {
    scope.launch {
        this@observeState.receiveState().collectLatest {
            block(it)
        }
    }
    return this
}

fun <S, E, T : Store<S, E>> T.observeDispatchedEvents(
    scope: CoroutineScope,
    block: (E) -> Unit
): T {
    scope.launch {
        this@observeDispatchedEvents.receiveDispatchedEvents().collectLatest {
            block(it)
        }
    }
    return this
}

fun <S, E, T : Store<S, E>> T.observeFinishedEvents(
    scope: CoroutineScope, block: (E) -> Unit
): T {
    scope.launch {
        this@observeFinishedEvents.receiveFinishedEvents().collectLatest {
            block(it)
        }
    }
    return this
}

/**
 * EXTS FOR FEATURES
 */

fun <S, E, T : Feature<S, E>> T.observeState(
    scope: CoroutineScope,
    block: (S) -> Unit
): T {
    store().observeState(scope, block)
    return this
}

fun <S, E, T : Feature<S, E>> T.observeDispatchedEvents(
    scope: CoroutineScope,
    block: (E) -> Unit
): T {
    store().observeDispatchedEvents(scope, block)
    return this
}

fun <S, E, T : Feature<S, E>> T.observeFinishedEvents(
    scope: CoroutineScope,
    block: (E) -> Unit
): T {
    store().observeFinishedEvents(scope, block)
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



















