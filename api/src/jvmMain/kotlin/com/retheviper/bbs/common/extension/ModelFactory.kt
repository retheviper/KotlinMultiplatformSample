package com.retheviper.bbs.common.extension

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.domain.model.Comment
import com.retheviper.bbs.message.domain.model.Message
import com.retheviper.bbs.model.common.PaginationProperties
import com.retheviper.bbs.model.response.GetArticleResponse
import com.retheviper.bbs.model.response.GetCommentResponse
import com.retheviper.bbs.model.response.GetUserResponse
import com.retheviper.bbs.model.response.ListArticleResponse
import com.retheviper.bbs.model.response.ListCommentResponse
import com.retheviper.bbs.model.response.ListLatestMessagesResponse
import com.retheviper.bbs.user.domain.model.User

fun GetUserResponse.Companion.from(dto: User): GetUserResponse {
    return GetUserResponse(
        id = checkNotNull(dto.id?.value),
        username = dto.username,
        name = dto.name,
        mail = dto.mail
    )
}

fun ListArticleResponse.Companion.from(paginationProperties: PaginationProperties, dtos: List<Article>): ListArticleResponse {
    return ListArticleResponse(
        paginationProperties = paginationProperties,
        articleSummaries = dtos.map {
            ListArticleResponse.ArticleSummary(
                id = checkNotNull(it.id?.value),
                title = it.title,
                authorName = checkNotNull(it.authorName),
                categoryName = checkNotNull(it.category).name,
                comments = it.comments.size,
                viewCount = it.viewCount,
                createdDate = it.createdDate.toString()
            )
        }
    )
}

fun GetArticleResponse.Companion.from(dto: Article): GetArticleResponse {
    return GetArticleResponse(
        id = checkNotNull(dto.id?.value),
        title = dto.title,
        content = dto.content,
        author = checkNotNull(dto.authorName),
        viewCount = dto.viewCount,
        likeCount = dto.likeCount,
        dislikeCount = dto.dislikeCount,
        categoryName = checkNotNull(dto.category).name,
        tags = dto.tags.map { it.name },
        comments = dto.comments.map {
            GetCommentResponse.from(it)
        }
    )
}

fun ListCommentResponse.Companion.from(paginationProperties: PaginationProperties, dtos: List<Comment>): ListCommentResponse {
    return ListCommentResponse(
        paginationProperties = paginationProperties,
        comments = dtos.map {
            GetCommentResponse.from(it)
        }
    )
}

fun GetCommentResponse.Companion.from(dto: Comment): GetCommentResponse {
    return GetCommentResponse(
        id = checkNotNull(dto.id?.value),
        content = dto.content,
        author = checkNotNull(dto.authorName)
    )
}

fun ListLatestMessagesResponse.Companion.from(dto: Message): ListLatestMessagesResponse {
    return ListLatestMessagesResponse(
        id = checkNotNull(dto.id?.value),
        messageGroupId = checkNotNull(dto.messageGroupId.value),
        userId = checkNotNull(dto.userId.value),
        username = dto.username,
        content = dto.content,
        createdDate = dto.createdDate.toString(),
        updatedDate = dto.updatedDate.toString()
    )
}