package com.retheviper.bbs.user.domain.model

import com.retheviper.bbs.model.request.CreateUserRequest

data class User(
    val id: Int? = null,
    val username: String,
    val password: String,
    val name: String,
    val mail: String
) {
    companion object {
        fun from(request: CreateUserRequest) = User(
            username = request.username,
            password = request.password,
            name = request.name,
            mail = request.mail
        )
    }
}
