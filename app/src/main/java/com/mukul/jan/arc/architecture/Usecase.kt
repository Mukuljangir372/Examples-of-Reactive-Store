package com.mukul.jan.arc.architecture

abstract class BaseUsecase

class HomeRepository()
class GetUsersUsecase: BaseUsecase() {
    operator fun invoke(): List<User> {
        return listOf(User(id = 1, name = "m"), User(id = 2,"k"))
    }
}

class DeleteUserUsecase: BaseUsecase() {
    operator fun invoke(id: Int): Int {
        //io or network call
        return id
    }
}