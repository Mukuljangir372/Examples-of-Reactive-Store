package com.mukul.jan.arc.testing

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

class TestingViewModel(
    private val dispatcher: CoroutineDispatcher,
    private val repo: TestingRepository,
): ViewModel() {
    private val scope = CoroutineScope(dispatcher)

    suspend fun getUsers() : List<Int>{
        delay(1000L)
        return listOf(1)
    }
}