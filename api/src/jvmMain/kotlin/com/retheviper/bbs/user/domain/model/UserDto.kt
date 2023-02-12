package com.retheviper.bbs.user.domain.model

import com.retheviper.bbs.user.web.model.request.CreateUserRequest

data class UserDto(
    val id: Int? = null,
    val username: String,
    val password: String,
    val name: String,
    val mail: String
) {
    companion object {
        fun from(request: CreateUserRequest) = UserDto(
            username = request.username,
            password = request.password,
            name = request.name,
            mail = request.mail
        )
    }
}
