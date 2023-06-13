package com.retheviper.bbs.model.request

import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserRequest(
    val username: String,
    val password: String,
    val name: String,
    val mail: String
)