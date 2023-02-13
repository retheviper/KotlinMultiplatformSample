package com.retheviper.bbs.model.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateArticleRequest(
    val title: String,
    val content: String,
    val password: String,
    val authorId: Int,
    val authorName: String,
)