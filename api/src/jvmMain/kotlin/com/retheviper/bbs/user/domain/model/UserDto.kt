package com.retheviper.bbs.user.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Int? = null,
    val username: String,
    val password: String? = null,
    val name: String,
    val mail: String
)
