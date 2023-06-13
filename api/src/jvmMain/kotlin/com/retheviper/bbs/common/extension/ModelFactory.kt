package com.retheviper.bbs.common.extension

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.domain.model.Comment
import com.retheviper.bbs.message.domain.model.Message
import com.retheviper.bbs.model.common.PaginationProperties
import com.retheviper.bbs.model.response.ArticleResponse
import com.retheviper.bbs.model.response.CommentResponse
import com.retheviper.bbs.model.response.UserResponse
import com.retheviper.bbs.model.response.ListArticleResponse
import com.retheviper.bbs.model.response.ListMessagesResponse
import com.retheviper.bbs.user.domain.model.User

fun UserResponse.Companion.from(dto: User): UserResponse {
    return UserResponse(
        id = checkNotNull(dto.id?.value),
        username = dto.username,
        name = dto.name,
        mail = dto.mail
    )
}

fun ListArticleResponse.Companion.from(
    paginationProperties: PaginationProperties,
    dtos: List<Article>
): ListArticleResponse {
    return ListArticleResponse(
        paginationProperties = paginationProperties,
        articleSummaries = dtos.map {
            ListArticleResponse.ArticleSummary(
                id = checkNotNull(it.id?.value),
                title = it.title ?: "",
                authorName = checkNotNull(it.authorName),
                categoryName = checkNotNull(it.category).name,
                comments = it.comments.size,
                viewCount = it.viewCount,
                createdDate = it.createdDate.toString()
            )
        }
    )
}

fun ArticleResponse.Companion.from(dto: Article): ArticleResponse {
    return ArticleResponse(
        id = checkNotNull(dto.id?.value),
        title = dto.title ?: "",
        content = dto.content ?: "",
        author = checkNotNull(dto.authorName),
        viewCount = dto.viewCount,
        likeCount = dto.likeCount,
        dislikeCount = dto.dislikeCount,
        categoryName = checkNotNull(dto.category).name,
        tags = dto.tags.map { it.name },
        comments = dto.comments.map {
            CommentResponse.from(it)
        }
    )
}

fun CommentResponse.Companion.from(dto: Comment): CommentResponse {
    return CommentResponse(
        id = checkNotNull(dto.id?.value),
        content = dto.content,
        author = checkNotNull(dto.authorName)
    )
}

fun ListMessagesResponse.Companion.from(dto: Message): ListMessagesResponse {
    return ListMessagesResponse(
        id = checkNotNull(dto.id?.value),
        messageGroupId = checkNotNull(dto.messageGroupId.value),
        userId = checkNotNull(dto.userId.value),
        username = dto.username,
        content = dto.content,
        createdDate = dto.createdDate.toString(),
        updatedDate = dto.updatedDate.toString()
    )
}