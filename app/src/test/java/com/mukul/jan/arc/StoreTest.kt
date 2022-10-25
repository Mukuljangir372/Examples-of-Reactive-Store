package com.mukul.jan.arc

import com.mukul.jan.arc.architecture.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)

class StoreTest {

    @Mock
    private lateinit var getUserUsecase: GetUsersUsecase

    @Before
    fun setup() {
        getUserUsecase = mock()
    }

    @Test
    fun get_users_feature_should_change_users_state() = runTest {

        val feature = GetUsersFeature(
            scope = this,
            getUsersUsecase = getUserUsecase
        )

        val mocked = listOf(User(id = 1,name = "m"))
        whenever(getUserUsecase.invoke()).thenReturn(mocked)
        Assert.assertEquals(mocked,getUserUsecase.invoke())

        feature.invoke().join()
//        Assert.assertEquals(true, feature.store().receiveState().first().loading)

//        runCurrent() //execute all pending coroutines
//        advanceUntilIdle() //Advances the testScheduler to the point where there are no tasks remaining.

        val finalState = feature.store().receiveState().first()
        Assert.assertEquals(false, finalState.loading)

        Assert.assertEquals(
            mocked,
            feature.store().state().users
        )

    }
}