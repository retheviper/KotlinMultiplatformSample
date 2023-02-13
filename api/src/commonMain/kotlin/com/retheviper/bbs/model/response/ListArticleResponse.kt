package com.retheviper.bbs.model.response

import kotlinx.serialization.Serializable

@Serializable
data class ListArticleResponse(
    val page: Int,
    val limit: Int,
    val pageSize: Int,
    val articleSummaries: List<ArticleSummary>
) {
    @Serializable
    data class ArticleSummary(
        val id: Int,
        val title: String,
        val authorName: String,
        val comments: Int
    )
}
