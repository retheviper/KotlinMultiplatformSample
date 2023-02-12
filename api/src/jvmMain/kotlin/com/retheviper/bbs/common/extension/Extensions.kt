package com.retheviper.bbs.common.extension

import com.retheviper.bbs.board.domain.model.Board
import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.property.JwtConfigs
import com.retheviper.bbs.constant.ErrorCode
import com.retheviper.bbs.model.response.ExceptionResponse
import com.retheviper.bbs.model.response.GetBoardResponse
import com.retheviper.bbs.model.response.GetUserResponse
import com.retheviper.bbs.model.response.ListBoardResponse
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

fun ListBoardResponse.Companion.from(page: Int, pageSize: Int, limit: Int, dtos: List<Board>): ListBoardResponse {
    return ListBoardResponse(
        page = page,
        limit = limit,
        pageSize = pageSize,
        boardInfos = dtos.map {
            ListBoardResponse.BoardInfo(
                id = checkNotNull(it.id),
                title = it.title,
                authorName = checkNotNull(it.authorName)
            )
        }
    )
}

fun GetBoardResponse.Companion.from(dto: Board): GetBoardResponse {
    return GetBoardResponse(
        id = checkNotNull(dto.id),
        title = dto.title,
        content = dto.content,
        author = checkNotNull(dto.authorName),
        comments = dto.comments?.map {
            GetBoardResponse.Comment(
                id = checkNotNull(it.id),
                content = it.content,
                author = checkNotNull(it.authorName)
            )
        } ?: emptyList()
    )
}