package com.retheviper.bbs.infrastructure.model.request

import kotlinx.serialization.Serializable

@Serializable
data class UpdateCommentRequest(
    val content: String,
    val password: String
)
