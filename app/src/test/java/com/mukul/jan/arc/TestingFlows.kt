package com.mukul.jan.arc

import com.mukul.jan.arc.testing.TestingRepository
import com.mukul.jan.arc.testing.TestingViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class TestingFlows {

    lateinit var repo: TestingRepository
    lateinit var viewModel: TestingViewModel

    @Before
    fun setup() {
        repo = mock()
        viewModel = TestingViewModel(UnconfinedTestDispatcher(),repo)
    }

    @Test
    fun get_users_should_fetch_users() = runTest {
        val users = viewModel.getUsers()
        Assert.assertEquals(listOf(1),users,)
    }
}















