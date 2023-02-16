package com.retheviper.bbs.board.domain.model

import com.retheviper.bbs.board.infrastructure.model.ArticleRecord

data class Article(
    val id: Int? = null,
    val title: String,
    val content: String,
    val password: String,
    val authorId: Int,
    val authorName: String? = null,
    val comments: List<Comment>? = null
) {
    companion object {
        fun from(articleRecord: ArticleRecord, comments: List<Comment>?): Article {
            return Article(
                id = articleRecord.id,
                title = articleRecord.title,
                content = articleRecord.content,
                password = articleRecord.password,
                authorId = articleRecord.authorId,
                authorName = articleRecord.authorName,
                comments = comments
            )
        }
    }
}
