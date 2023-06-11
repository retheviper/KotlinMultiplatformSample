package com.retheviper.bbs.board.domain.model

import com.retheviper.bbs.board.infrastructure.model.ArticleRecord
import com.retheviper.bbs.board.infrastructure.model.CommentRecord
import com.retheviper.bbs.board.infrastructure.model.TagRecord
import com.retheviper.bbs.common.extension.toHashedString
import com.retheviper.bbs.common.extension.trimAll
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.BoardId
import com.retheviper.bbs.common.value.CategoryId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.model.request.CreateArticleRequest
import com.retheviper.bbs.model.request.UpdateArticleRequest
import java.time.LocalDateTime

data class Article(
    val boardId: BoardId? = null,
    val id: ArticleId? = null,
    val title: String? = null,
    val content: String? = null,
    val password: String,
    val authorId: UserId,
    val authorName: String? = null,
    val category: Category? = null,
    val tags: List<Tag> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val viewCount: UInt = 0u,
    val likeCount: UInt = 0u,
    val dislikeCount: UInt = 0u,
    val createdDate: LocalDateTime? = null,
    val updatedDate: LocalDateTime? = null
) {
    companion object {
        fun from(article: ArticleRecord, tags: List<TagRecord>, comments: List<CommentRecord>): Article {
            return Article(
                boardId = article.boardId,
                id = article.id,
                title = article.title,
                content = article.content,
                password = article.password,
                authorId = article.authorId,
                authorName = article.authorName,
                category = Category(
                    id = article.categoryId,
                    name = article.categoryName
                ),
                tags = tags.map { Tag.from(it) },
                comments = comments.map { Comment.from(it) },
                viewCount = article.viewCount,
                likeCount = article.likeCount,
                dislikeCount = article.dislikeCount,
                createdDate = article.createdDate,
                updatedDate = article.updatedDate
            )
        }

        fun from(boardId: BoardId?, authorId: UserId, request: CreateArticleRequest): Article {
            return Article(
                boardId = boardId,
                title = request.title.trimAll(),
                content = request.content.trimAll(),
                authorId = authorId,
                password = request.password.trimAll(),
                category = Category(id = CategoryId(request.categoryId)),
                tags = request.tagNames?.map { Tag(name = it.trimAll()) }?.distinct() ?: emptyList()
            )
        }

        fun from(id: ArticleId, authorId: UserId, request: UpdateArticleRequest): Article {
            return Article(
                id = id,
                title = request.title?.trimAll(),
                content = request.content?.trimAll(),
                authorId = authorId,
                password = request.password.trimAll(),
                category = request.categoryId?.let { Category(id = CategoryId(request.categoryId)) },
                tags = request.tagNames?.map { Tag(name = it.trimAll()) }?.distinct() ?: emptyList()
            )
        }
    }
}
