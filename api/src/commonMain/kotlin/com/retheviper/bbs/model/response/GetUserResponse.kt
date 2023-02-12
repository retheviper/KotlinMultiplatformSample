package com.retheviper.bbs.model.response

import kotlinx.serialization.Serializable

@Serializable
data class GetUserResponse(
    val id: Int,
    val username: String,
    val name: String,
    val mail: String
)