package com.mukul.jan.arc

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mukul.jan.arc.architecture.*
import com.mukul.jan.arc.delegation.PreferenceHolder
import com.mukul.jan.arc.delegation.PreferenceStore
import com.mukul.jan.arc.servicelocator.components
import com.mukul.jan.arc.ui.theme.ArcTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.UUID
import kotlin.random.Random

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
