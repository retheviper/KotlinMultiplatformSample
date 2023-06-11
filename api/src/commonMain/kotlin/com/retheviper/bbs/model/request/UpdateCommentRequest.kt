package com.retheviper.bbs.model.request

import kotlinx.serialization.Serializable

@Serializable
data class UpdateCommentRequest(
    val content: String,
    val password: String
)
