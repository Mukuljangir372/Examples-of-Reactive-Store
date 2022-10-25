package com.mukul.jan.arc.architecture

import kotlinx.coroutines.delay

abstract class BaseUsecase

class GetUsersUsecase: BaseUsecase() {
    suspend operator fun invoke(): List<User> {
        delay(1000)
        return listOf(User(id = 1, name = "m"), User(id = 2,"k"))
    }
}

class DeleteUserUsecase: BaseUsecase() {
    operator fun invoke(id: Int): Int {
        //io or network call
        return id
    }
}