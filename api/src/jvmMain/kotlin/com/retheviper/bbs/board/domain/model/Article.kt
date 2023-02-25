package com.retheviper.bbs.board.domain.model

import com.retheviper.bbs.board.infrastructure.model.ArticleRecord
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.model.request.CreateArticleRequest
import java.time.LocalDateTime

data class Article(
    val id: ArticleId? = null,
    val title: String,
    val content: String,
    val password: String,
    val authorId: UserId,
    val authorName: String? = null,
    val category: Category? = null,
    val tags: List<Tag> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val viewCount: Int = 0,
    val likeCount: Int = 0,
    val dislikeCount: Int = 0,
    val createdDate: LocalDateTime? = null,
    val updatedDate: LocalDateTime? = null
) {
    companion object {
        fun from(articleRecord: ArticleRecord, category: Category, tags: List<Tag>, comments: List<Comment>?): Article {
            return Article(
                id = articleRecord.id,
                title = articleRecord.title,
                content = articleRecord.content,
                password = articleRecord.password,
                authorId = articleRecord.authorId,
                authorName = articleRecord.authorName,
                category = category,
                tags = tags,
                comments = comments ?: emptyList(),
                viewCount = articleRecord.viewCount,
                likeCount = articleRecord.likeCount,
                dislikeCount = articleRecord.dislikeCount,
                createdDate = articleRecord.createdDate,
                updatedDate = articleRecord.updatedDate
            )
        }

        fun from(request: CreateArticleRequest): Article {
            return Article(
                title = request.title,
                content = request.content,
                password = request.password,
                authorId = UserId(request.authorId),
                category = Category(name = request.categoryName),
                tags = request.tagNames?.map { Tag(name = it) } ?: emptyList()
            )
        }
    }

    fun updateViewCount(): Article {
        return this.copy(viewCount = this.viewCount + 1)
    }
}
