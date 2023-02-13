package com.retheviper.bbs.common.extension

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.domain.model.Comment
import com.retheviper.bbs.model.response.GetArticleResponse
import com.retheviper.bbs.model.response.GetCommentResponse
import com.retheviper.bbs.model.response.GetUserResponse
import com.retheviper.bbs.model.response.ListArticleResponse
import com.retheviper.bbs.model.response.ListCommentResponse
import com.retheviper.bbs.user.domain.model.User

fun GetUserResponse.Companion.from(dto: User): GetUserResponse {
    return GetUserResponse(
        id = checkNotNull(dto.id),
        username = dto.username,
        name = dto.name,
        mail = dto.mail
    )
}

fun ListArticleResponse.Companion.from(page: Int, pageSize: Int, limit: Int, dtos: List<Article>): ListArticleResponse {
    return ListArticleResponse(
        page = page,
        limit = limit,
        pageSize = pageSize,
        articleSummaries = dtos.map {
            ListArticleResponse.ArticleSummary(
                id = checkNotNull(it.id),
                title = it.title,
                authorName = checkNotNull(it.authorName),
                comments = it.comments?.size ?: 0
            )
        }
    )
}

fun GetArticleResponse.Companion.from(dto: Article): GetArticleResponse {
    return GetArticleResponse(
        id = checkNotNull(dto.id),
        title = dto.title,
        content = dto.content,
        author = checkNotNull(dto.authorName),
        comments = dto.comments?.map {
            GetCommentResponse.from(it)
        } ?: emptyList()
    )
}

fun ListCommentResponse.Companion.from(page: Int, pageSize: Int, limit: Int, dtos: List<Comment>): ListCommentResponse {
    return ListCommentResponse(
        page = page,
        limit = limit,
        pageSize = pageSize,
        comments = dtos.map {
            GetCommentResponse.from(it)
        }
    )
}

fun GetCommentResponse.Companion.from(dto: Comment): GetCommentResponse {
    return GetCommentResponse(
        id = checkNotNull(dto.id),
        content = dto.content,
        author = checkNotNull(dto.authorName)
    )
}