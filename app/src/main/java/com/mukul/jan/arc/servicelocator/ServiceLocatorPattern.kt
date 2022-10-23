package com.mukul.jan.arc.servicelocator

import android.content.Context

// Dependency Injection -
// Consider Service Locator pattern for DI for those app components that don't requires
// testing phase like firebase analytics, clipboard handler etc. rather than using Hilt/Dagger.

//Some components that don't require testing
class FirebaseAnalytics()
class PerformanceMonitor()
class TimeStore()

class Components(
    private val context: Context
) {
    val firebase by lazy {
        FirebaseStore(
            analytics = FirebaseAnalytics()
        )
    }
    val monitor by lazy {
        PerformanceMonitor()
    }
}

class FirebaseStore(
    val analytics: FirebaseAnalytics
)

fun Context.components() = Components(
    context = this
)

//USAGES [Service Locator]

class ExampleActivity {
    fun onCreated() {
//        val anl = components().firebase.analytics
    }
}