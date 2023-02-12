package com.retheviper.bbs.auth.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Credential(
    val username: String,
    val password: String
)
