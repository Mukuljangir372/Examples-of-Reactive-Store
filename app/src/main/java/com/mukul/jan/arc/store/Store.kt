package com.mukul.jan.arc.store

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

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
    private val scopeWithException = scope + exceptionHandler

    fun dispatch(store: Store<S, E>, event: E) {
        val finalEvent = if (middleware.isNotEmpty()) {
            var currentEvent: E = event
            for (single in middleware) {
                currentEvent = single.invoke(
                    scope = scopeWithException,
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
                    scope = scopeWithException,
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

    private val stateSubs =
        Collections.newSetFromMap(ConcurrentHashMap<Subscription<S, E>, Boolean>())
    private val dispatchedEventSubs =
        Collections.newSetFromMap(ConcurrentHashMap<Subscription<S, E>, Boolean>())
    private val finishedEventSubs =
        Collections.newSetFromMap(ConcurrentHashMap<Subscription<S, E>, Boolean>())

    private fun getSubscriptions(type: SubscriptionType): MutableSet<Subscription<S, E>> {
        return when (type) {
            is SubscriptionType.State -> stateSubs
            is SubscriptionType.DispatchedEvents -> dispatchedEventSubs
            is SubscriptionType.FinishedEvents -> finishedEventSubs
        }
    }

    private fun removeSubscription(type: SubscriptionType, sub: Subscription<S, E>) {
        val subs = getSubscriptions(type)
        subs.remove(sub)
    }

    private val dispatcher = Dispatcher(
        scope = coroutineScope,
        exceptionHandler = exceptionHandler,
        reducer = reducer,
        middleware = middleware,
        endConnector = endConnector
    )

    fun dispatch(event: E) = coroutineScope.launch(exceptionHandler) {
        synchronized(this@Store) {
            sendDispatchedEvent(event)
            dispatcher.dispatch(
                store = this@Store,
                event = event
            )
        }
    }

    private fun sendDispatchedEvent(event: E) {
        dispatchedEventSubs.forEach { sub ->
            sub.dispatch(state(), event)
        }
    }

    @Volatile
    private var mutableState: S = initialState
    fun state() = mutableState

    @Synchronized
    fun updateState(newState: S, event: E) {
        mutableState = newState
        sendState(newState, event)
        sendFinishedEvent(event)
    }

    private fun sendState(state: S, event: E) {
        stateSubs.forEach { sub ->
            sub.dispatch(state, event)
        }
    }

    private fun sendFinishedEvent(event: E) {
        finishedEventSubs.forEach { sub ->
            sub.dispatch(state(), event)
        }
    }

    fun observe(
        type: SubscriptionType,
        observer: Observer<S, E>
    ): Subscription<S, E> {
        val subs = getSubscriptions(type)
        val sub = Subscription(
            type = type,
            observer = observer,
            store = this
        )
        subs.add(sub)
        return sub
    }

    sealed class SubscriptionType {
        object State : SubscriptionType()
        object DispatchedEvents : SubscriptionType()
        object FinishedEvents : SubscriptionType()
    }

    class Subscription<S : State, E : Event> internal constructor(
        private val type: SubscriptionType,
        private val observer: Observer<S, E>,
        store: Store<S, E>,
    ) {
        private val storeRef = WeakReference(store)
        private var active = true

        @Synchronized
        fun pause() {
            active = false
        }

        @Synchronized
        fun resume() {
            active = true
        }

        @Synchronized
        internal fun dispatch(state: S, event: E) {
            if (active) {
                observer.onInvoke(state, event)
            }
        }

        @Synchronized
        fun unsubscribe() {
            active = false
            storeRef.get()?.removeSubscription(type, this)
            storeRef.clear()
        }
    }

    interface Observer<S : State, E : Event> {
        fun onInvoke(state: S, event: E)
    }
}

/**
 * //--------------------------------------------------------------------//
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
        checkForEmptyKey(key)
        val store = hashMap[key]
        if (store != null) {
            return store as T
        } else {
            hashMap[key] = default
        }
        return default
    }

    private fun checkForEmptyKey(key: String) {
        check(
            value = key.isNotEmpty(),
            lazyMessage = {
                "Key can't be empty or null"
            }
        )
    }
}

/**
 * //--------------------------------------------------------------------//
 * FEATURE
 */

open class Feature<S : State, E : Event>(
    private val initialState: S,
    private val coroutineScope: CoroutineScope,
    private val storeKey: String,
    private val reducer: Reducer<S, E>,
    private val middleware: List<Middleware<S, E>> = emptyList(),
    private val endConnector: List<EndConnector<S, E>> = emptyList(),
) {
    fun coroutineScope() = coroutineScope

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
 * //--------------------------------------------------------------------//
 * EXTS
 */
fun <T : Store<*, *>> getStore(key: String, default: T): T {
    val provider = StoreProvider.getInstance()
    return provider.get(key, default)
}

fun storeKey(target: Class<*>): String {
    return target.name + ".store"
}

fun className(target: Class<*>): String {
    return target.simpleName
}

/**
 * //--------------------------------------------------------------------//
 * EXTS FOR STORE
 */

fun <S : State, E : Event> Store<S, E>.observe(
    type: Store.SubscriptionType,
    block: (state: S, event: E) -> Unit,
): Store<S, E> {
    val store = this
    store.observe(
        type = type,
        observer = object : Store.Observer<S, E> {
            override fun onInvoke(state: S, event: E) {
                block(state, event)
            }
        }
    )
    return store
}

fun <S : State, E : Event> Store<S, E>.stateChannel(
    type: Store.SubscriptionType
): Channel<S> {
    val store = this
    val channel = Channel<S>(Channel.CONFLATED)
    store.observe(type = type) { state, _ ->
        runBlocking {
            try {
                channel.send(state)
            } catch (_: CancellationException) {

            }
        }
    }
    return channel
}

fun <S : State, E : Event> Store<S, E>.eventChannel(
    type: Store.SubscriptionType
): Channel<E> {
    val store = this
    val channel = Channel<E>(Channel.CONFLATED)
    store.observe(type = type) { _, event ->
        runBlocking {
            try {
                channel.send(event)
            } catch (_: CancellationException) {

            }
        }
    }
    return channel
}

fun <S : State, E : Event> Store<S, E>.consumeState(
    scope: CoroutineScope,
    block: (S) -> Unit
): Store<S, E> {
    val store = this
    val channel = store.stateChannel(type = Store.SubscriptionType.State)
    scope.launch {
        channel.consumeEach {
            block(it)
        }
    }
    return this
}

fun <S : State, E : Event> Store<S, E>.consumeDispatchedEvents(
    scope: CoroutineScope,
    block: (E) -> Unit
): Store<S, E> {
    val store = this
    val channel = store.eventChannel(type = Store.SubscriptionType.DispatchedEvents)
    scope.launch {
        channel.consumeEach {
            block(it)
        }
    }
    return this
}

fun <S : State, E : Event> Store<S, E>.consumeFinishedEvents(
    scope: CoroutineScope,
    block: (E) -> Unit
): Store<S, E> {
    val store = this
    val channel = store.eventChannel(type = Store.SubscriptionType.FinishedEvents)
    scope.launch {
        channel.consumeEach {
            block(it)
        }
    }
    return this
}

fun <S : State, E : Event> Fragment.consumeState(store: Store<S, E>, block: (S) -> Unit) {
    val fragment = this
    val scope = lifecycle.coroutineScope
    if (fragment.view != null && fragment.activity != null) {
        store.consumeState(scope = scope) { latestState ->
            if (fragment.view != null && fragment.activity != null) {
                block(latestState)
            }
        }
    }
}

fun <S : State, E : Event> ComponentActivity.consumeState(store: Store<S, E>, block: (S) -> Unit) {
    val activity = this
    val scope = lifecycle.coroutineScope
    if (activity.lifecycle.currentState != Lifecycle.State.DESTROYED) {
        store.consumeState(scope = scope) { latestState ->
            if (activity.lifecycle.currentState != Lifecycle.State.DESTROYED) {
                block(latestState)
            }
        }
    }
}

/**
 * //--------------------------------------------------------------------//
 * EXTS FOR FEATURES
 */

fun <S : State, E : Event, T : Feature<S, E>> T.consumeState(
    scope: CoroutineScope = coroutineScope(),
    block: (S) -> Unit
): T {
    store().consumeState(scope, block)
    return this
}

fun <S : State, E : Event, T : Feature<S, E>> T.consumeDispatchedEvents(
    scope: CoroutineScope = coroutineScope(),
    block: (E) -> Unit
): T {
    store().consumeDispatchedEvents(scope, block)
    return this
}

fun <S : State, E : Event, T : Feature<S, E>> T.consumeFinishedEvents(
    scope: CoroutineScope = coroutineScope(),
    block: (E) -> Unit
): T {
    store().consumeFinishedEvents(scope, block)
    return this
}

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
















