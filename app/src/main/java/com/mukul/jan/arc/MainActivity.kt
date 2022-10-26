package com.mukul.jan.arc

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mukul.jan.arc.store.consumeState
import com.mukul.jan.arc.store.store_example.HomeEvent
import com.mukul.jan.arc.store.store_example.HomeInteractor
import com.mukul.jan.arc.store.store_example.HomeStore
import com.mukul.jan.arc.store.store_example.HomeViewModel
import com.mukul.jan.arc.ui.theme.ArcTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val interactor = HomeInteractor(viewModel)

        val store = HomeStore()
        store.consumeState(lifecycleScope) {
            Log.e("", "1 consumeState")
        }
        store.consumeState(lifecycleScope) {
            Log.e("", "2 consumeState")
        }

        store.consumeState(lifecycleScope) {
            Log.e("", "3 consumeState")
        }
        store.consumeState(lifecycleScope) {
            Log.e("", "4 consumeState")
        }

        store.consumeState(lifecycleScope) {
            Log.e("", "5 consumeState")
        }
        store.consumeState(lifecycleScope) {
            Log.e("", "6 consumeState")
        }

        store.consumeState(lifecycleScope) {
            Log.e("", "7 consumeState")
        }
        store.consumeState(lifecycleScope) {
            Log.e("", "8 consumeState")
        }

        setContent {
            SimpleSurface {
                Button(onClick = {
                    store.dispatch(HomeEvent.InsertUsers(users = listOf()))
                }) {
                    Text(text = "Fetch Users")
                }
            }
        }
    }
}

@Composable
private fun SimpleSurface(block: @Composable () -> Unit) {
    ArcTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            Column {
                block()
            }
        }
    }
}
