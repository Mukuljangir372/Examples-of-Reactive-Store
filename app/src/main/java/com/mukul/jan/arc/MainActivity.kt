package com.mukul.jan.arc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.mukul.jan.arc.store.example.HomeInteractor
import com.mukul.jan.arc.store.example.HomeViewModel
import com.mukul.jan.arc.ui.theme.ArcTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val interactor = HomeInteractor(viewModel)

        setContent {
            SimpleSurface {
                Button(onClick = {
                    interactor.fetchUsersClicked()
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
