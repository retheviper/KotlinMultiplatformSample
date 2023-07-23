package com.retheviper.bbs.infrastructure.model.response

import kotlinx.serialization.Serializable

@Serializable
data class CommentResponse(
    val id: Int,
    val content: String,
    val author: String
)