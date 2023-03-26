package com.retheviper.bbs.auth.domain.model

import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.model.request.LoginRequest

data class Credential(
    val userId: UserId? = null,
    val username: String,
    val password: String
) {
    companion object {
        fun from(request: LoginRequest): Credential {
            return Credential(
                username = request.username,
                password = request.password
            )
        }
    }
}
