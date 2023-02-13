package com.retheviper.bbs.common.extension

import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.property.JwtConfigs
import com.retheviper.bbs.constant.ErrorCode
import com.retheviper.bbs.model.response.ExceptionResponse
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

@Throws(BadRequestException::class)
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

@Throws(BadRequestException::class)
fun ApplicationCall.getIdFromParameter(): Int {
    return parameters["id"]?.toInt() ?: throw BadRequestException("Invalid ID")
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