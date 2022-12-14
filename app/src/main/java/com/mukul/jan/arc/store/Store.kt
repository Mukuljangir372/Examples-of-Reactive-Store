package com.mukul.jan.arc.store

import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
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
        internal var binding: Binding? = null

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
            binding?.unbind()
        }

        interface Binding {
            fun unbind()
        }
    }

    interface Observer<S : State, E : Event> {
        fun onInvoke(state: S, event: E)
    }
}

class SubscriptionLifecycleBinding<S : State, E : Event>(
    private val owner: LifecycleOwner,
    private val subscription: Store.Subscription<S, E>,
) : DefaultLifecycleObserver, Store.Subscription.Binding {
    override fun onStart(owner: LifecycleOwner) {
        subscription.resume()
    }

    override fun onStop(owner: LifecycleOwner) {
        subscription.pause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        subscription.unsubscribe()
    }

    override fun unbind() {
        owner.lifecycle.removeObserver(this)
    }
}

class SubscriptionViewBinding<S : State, E : Event>(
    private val view: View,
    private val subscription: Store.Subscription<S, E>,
) : View.OnAttachStateChangeListener, Store.Subscription.Binding {
    override fun unbind() {
        view.removeOnAttachStateChangeListener(this)
    }

    override fun onViewAttachedToWindow(p0: View) {
        subscription.resume()
    }

    override fun onViewDetachedFromWindow(p0: View) {
        subscription.unsubscribe()
    }
}




