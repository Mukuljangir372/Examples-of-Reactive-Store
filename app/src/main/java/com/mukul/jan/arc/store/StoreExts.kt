package com.mukul.jan.arc.store

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import com.mukul.jan.arc.store.feature.Feature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException


/**
 * //--------------------------------------------------------------------//
 * EXTS
 */

fun storeKey(target: Class<*>): String {
    return target.name + ".store"
}

fun className(target: Class<*>): String {
    return target.simpleName
}

fun <S : State, E : Event> Store<S, E>.observe(
    owner: LifecycleOwner? = null,
    type: Store.SubscriptionType,
    block: (state: S, event: E) -> Unit,
): Store.Subscription<S, E> {
    val store = this
    val subscription = store.observe(
        type = type,
        observer = object : Store.Observer<S, E> {
            override fun onInvoke(state: S, event: E) {
                block(state, event)
            }
        })

    owner?.let {
        subscription.binding = SubscriptionLifecycleBinding(
            owner = owner, subscription = subscription
        ).apply {
            it.lifecycle.addObserver(this)
        }
    }
    return subscription
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <S : State, E : Event> Store<S, E>.stateChannel(
    owner: LifecycleOwner? = null,
    type: Store.SubscriptionType
): Channel<S> {
    val store = this
    val channel = Channel<S>(Channel.CONFLATED)
    val subscription = store.observe(
        owner = owner, type = type
    ) { state, _ ->
        runBlocking {
            try {
                channel.send(state)
            } catch (_: CancellationException) {

            }
        }
    }
    channel.invokeOnClose {
        subscription.unsubscribe()
    }
    return channel
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <S : State, E : Event> Store<S, E>.eventChannel(
    owner: LifecycleOwner? = null,
    type: Store.SubscriptionType
): Channel<E> {
    val store = this
    val channel = Channel<E>(Channel.CONFLATED)

    val subscription = store.observe(
        owner = owner, type = type
    ) { _, event ->
        runBlocking {
            try {
                channel.send(event)
            } catch (_: CancellationException) {

            }
        }
    }
    channel.invokeOnClose {
        subscription.unsubscribe()
    }
    return channel
}

fun <S : State, E : Event> Store<S, E>.consumeState(
    owner: LifecycleOwner? = null,
    scope: CoroutineScope,
    block: (S) -> Unit
): Store<S, E> {
    val store = this
    val channel = store.stateChannel(
        owner = owner,
        type = Store.SubscriptionType.State
    )
    scope.launch {
        channel.consumeEach {
            block(it)
        }
    }
    return this
}

fun <S : State, E : Event> Store<S, E>.consumeDispatchedEvents(
    owner: LifecycleOwner? = null,
    scope: CoroutineScope,
    block: (E) -> Unit
): Store<S, E> {
    val store = this
    val channel = store.eventChannel(
        owner = owner,
        type = Store.SubscriptionType.DispatchedEvents
    )
    scope.launch {
        channel.consumeEach {
            block(it)
        }
    }
    return this
}

fun <S : State, E : Event> Store<S, E>.consumeFinishedEvents(
    owner: LifecycleOwner? = null,
    scope: CoroutineScope,
    block: (E) -> Unit
): Store<S, E> {
    val store = this
    val channel = store.eventChannel(
        owner = owner,
        type = Store.SubscriptionType.FinishedEvents
    )
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
    owner: LifecycleOwner? = lifecycleOwner(),
    scope: CoroutineScope = coroutineScope(),
    block: (S) -> Unit
): T {
    store().consumeState(
        owner = owner,
        scope = scope,
        block = block
    )
    return this
}

fun <S : State, E : Event, T : Feature<S, E>> T.consumeDispatchedEvents(
    owner: LifecycleOwner? = lifecycleOwner(),
    scope: CoroutineScope = coroutineScope(),
    block: (E) -> Unit
): T {
    store().consumeDispatchedEvents(
        owner = owner,
        scope = scope,
        block = block
    )
    return this
}

fun <S : State, E : Event, T : Feature<S, E>> T.consumeFinishedEvents(
    owner: LifecycleOwner? = lifecycleOwner(),
    scope: CoroutineScope = coroutineScope(),
    block: (E) -> Unit
): T {
    store().consumeFinishedEvents(
        owner = owner,
        scope = scope,
        block = block
    )
    return this
}

/**
 * //--------------------------------------------------------------------//
 * EXTS FOR ANDROID SPECIFIC
 */

fun <S : State, E : Event> Fragment.consumeState(
    store: Store<S, E>,
    block: (S) -> Unit
) {
    val fragment = this
    val scope = lifecycle.coroutineScope
    if (fragment.view != null && fragment.activity != null) {
        store.consumeState(
            owner = viewLifecycleOwner,
            scope = scope,
            block = {
                if (fragment.view != null && fragment.activity != null) {
                    block(it)
                }
            }
        )
    }
}

fun <S : State, E : Event> ComponentActivity.consumeState(
    store: Store<S, E>,
    block: (S) -> Unit
) {
    val activity = this
    val scope = lifecycle.coroutineScope
    if (activity.lifecycle.currentState != Lifecycle.State.DESTROYED) {
        store.consumeState(
            owner = activity,
            scope = scope,
            block = {
                if (activity.lifecycle.currentState != Lifecycle.State.DESTROYED) {
                    block(it)
                }
            }
        )
    }
}