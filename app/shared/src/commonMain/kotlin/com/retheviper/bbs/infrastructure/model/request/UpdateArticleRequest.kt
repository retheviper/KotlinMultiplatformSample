package com.retheviper.bbs.infrastructure.model.request

import kotlinx.serialization.Serializable

@Serializable
data class UpdateArticleRequest(
    val title: String?,
    val content: String?,
    val password: String,
    val categoryId: Int?,
    val tagNames: List<String>?
)