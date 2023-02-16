package com.retheviper.bbs.board.domain.model

import com.retheviper.bbs.board.infrastructure.model.ArticleRecord
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.model.request.CreateArticleRequest

data class Article(
    val id: ArticleId? = null,
    val title: String,
    val content: String,
    val password: String,
    val authorId: UserId,
    val authorName: String? = null,
    val comments: List<Comment> = emptyList()
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
                comments = comments ?: emptyList()
            )
        }

        fun from(request: CreateArticleRequest): Article {
            return Article(
                title = request.title,
                content = request.content,
                password = request.password,
                authorId = UserId(request.authorId)
            )
        }
    }
}
