package com.retheviper.bbs.board.infrastructure.model.response

import com.retheviper.bbs.model.common.PaginationProperties
import kotlinx.serialization.Serializable

@Serializable
data class ListArticleResponse(
    val paginationProperties: PaginationProperties,
    val articleSummaries: List<ArticleSummary>
) {
    @Serializable
    data class ArticleSummary(
        val id: Int,
        val title: String,
        val authorName: String,
        val categoryName: String,
        val comments: Int,
        val viewCount: Int,
        val createdDate: String
    )
}
