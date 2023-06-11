package com.retheviper.bbs.board.infrastructure.model.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateArticleRequest(
    val title: String,
    val content: String,
    val password: String,
    val categoryName: String,
    val tagNames: List<String>?
)