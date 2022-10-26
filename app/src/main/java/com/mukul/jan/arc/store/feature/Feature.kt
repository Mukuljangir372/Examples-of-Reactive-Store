package com.mukul.jan.arc.store.feature

import androidx.lifecycle.LifecycleOwner
import com.mukul.jan.arc.store.*
import kotlinx.coroutines.CoroutineScope

open class Feature<S : State, E : Event>(
    private val initialState: S,
    private val lifecycleOwner: LifecycleOwner?,
    private val coroutineScope: CoroutineScope,
    private val storeKey: String,
    private val reducer: Reducer<S, E>,
    private val middleware: List<Middleware<S, E>> = emptyList(),
    private val endConnector: List<EndConnector<S, E>> = emptyList(),
) {
    fun coroutineScope() = coroutineScope
    fun lifecycleOwner() = lifecycleOwner

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