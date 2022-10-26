package com.mukul.jan.arc.store

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.mukul.jan.arc.store.feature.Feature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException


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
 * EXTS
 */

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