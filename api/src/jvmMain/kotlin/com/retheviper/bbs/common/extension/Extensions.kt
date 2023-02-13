package com.retheviper.bbs.common.extension

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.domain.model.Comment
import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.property.JwtConfigs
import com.retheviper.bbs.constant.ErrorCode
import com.retheviper.bbs.model.response.ExceptionResponse
import com.retheviper.bbs.model.response.GetArticleResponse
import com.retheviper.bbs.model.response.GetCommentResponse
import com.retheviper.bbs.model.response.GetUserResponse
import com.retheviper.bbs.model.response.ListArticleResponse
import com.retheviper.bbs.model.response.ListCommentResponse
import com.retheviper.bbs.user.domain.model.User
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

fun Application.getEnvironmentVariable(key: String): String =
    environment.config.property(key).getString()

fun Application.getJwtConfigs(): JwtConfigs {
    val secret = getEnvironmentVariable("jwt.secret")
    val issuer = getEnvironmentVariable("jwt.issuer")
    val audience = getEnvironmentVariable("jwt.audience")
    val realm = getEnvironmentVariable("jwt.realm")
    return JwtConfigs(secret, issuer, audience, realm)
}

fun ApplicationCall.getPaginationProperties(): Triple<Int, Int, Int> {
    val page = request.queryParameters["page"]?.toInt() ?: 1
    val pageSize = request.queryParameters["pageSize"]?.toInt() ?: 10
    val limit = request.queryParameters["limit"]?.toInt() ?: 100

    if (page < 1 || pageSize < 1 || limit < 1) {
        throw BadRequestException("Invalid page, pageSize or limit.")
    }

    if (pageSize > limit) {
        throw BadRequestException("pageSize must be less than or equal to limit.")
    }

    if (page > limit / pageSize) {
        throw BadRequestException("page must be less than or equal to limit / pageSize.")
    }

    return Triple(page, pageSize, limit)
}

suspend fun ApplicationCall.respondBadRequest(message: String) {
    respond(
        status = HttpStatusCode.BadRequest,
        message = ExceptionResponse(
            code = ErrorCode.INVALID_PARAMETER.value,
            message = message
        )
    )
}

suspend fun ApplicationCall.respondBadRequest(exception: BadRequestException) {
    respond(
        status = HttpStatusCode.BadRequest,
        message = ExceptionResponse(
            code = exception.code.value,
            message = exception.message ?: "Bad Request"
        )
    )
}

suspend fun ApplicationCall.respondInternalServerError() {
    respond(
        status = HttpStatusCode.InternalServerError,
        message = ExceptionResponse(
            code = ErrorCode.UNKNOWN_ERROR.value,
            message = "Server Error"
        )
    )
}

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