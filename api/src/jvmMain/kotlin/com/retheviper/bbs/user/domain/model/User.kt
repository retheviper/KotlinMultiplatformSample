package com.retheviper.bbs.user.domain.model

import com.retheviper.bbs.common.extension.toHashedString
import com.retheviper.bbs.common.extension.trimAll
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.model.request.CreateUserRequest
import com.retheviper.bbs.model.request.UpdateUserRequest

data class User(
    val id: UserId? = null,
    val username: String,
    val password: String,
    val name: String,
    val mail: String
) {
    companion object {
        fun from(request: CreateUserRequest) = User(
            username = request.username.trimAll(),
            password = request.password.trimAll(),
            name = request.name,
            mail = request.mail
        )

        fun from(id: UserId, request: UpdateUserRequest) = User(
            id = id,
            username = request.username.trimAll(),
            password = request.password.trimAll(),
            name = request.name,
            mail = request.mail
        )
    }
}
