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
import com.mukul.jan.arc.store.example.HomeEvent
import com.mukul.jan.arc.store.example.HomeInteractor
import com.mukul.jan.arc.store.example.HomeStore
import com.mukul.jan.arc.store.example.HomeViewModel
import com.mukul.jan.arc.ui.theme.ArcTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val interactor = HomeInteractor(viewModel)

        val store = HomeStore()

        consumeState(store) {
            Log.e("", "0 $it")
        }
        consumeState(store) {
            Log.e("", "01 $it")
        }
        setContent {
            SimpleSurface {
                Button(onClick = {
                    lifecycleScope.launch {
                        for (i in 1..20) {
                            delay(1000)
                            store.dispatch(HomeEvent.ChangeName("$i"))
                        }
                    }
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
