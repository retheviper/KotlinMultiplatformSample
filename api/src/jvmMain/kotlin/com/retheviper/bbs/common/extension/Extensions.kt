package com.retheviper.bbs.common.extension

import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.property.JwtConfigs
import com.retheviper.bbs.constant.ErrorCode
import com.retheviper.bbs.model.response.ExceptionResponse
import com.retheviper.bbs.model.response.GetUserResponse
import com.retheviper.bbs.user.domain.model.UserDto
import io.ktor.server.application.Application

fun Application.getEnvironmentVariable(key: String): String =
    environment.config.property(key).getString()

fun Application.getJwtConfigs(): JwtConfigs {
    val secret = getEnvironmentVariable("jwt.secret")
    val issuer = getEnvironmentVariable("jwt.issuer")
    val audience = getEnvironmentVariable("jwt.audience")
    val realm = getEnvironmentVariable("jwt.realm")
    return JwtConfigs(secret, issuer, audience, realm)
}

fun ExceptionResponse.Companion.from(e: BadRequestException): ExceptionResponse {
    return ExceptionResponse(
        code = e.code.value,
        message = e.message.toString()
    )
}

fun ExceptionResponse.Companion.default(): ExceptionResponse {
    return ExceptionResponse(
        code = ErrorCode.UNKNOWN_ERROR.value,
        message = "Server Error"
    )
}

fun GetUserResponse.Companion.from(dto: UserDto): GetUserResponse {
    return GetUserResponse(
        id = checkNotNull(dto.id),
        username = dto.username,
        name = dto.name,
        mail = dto.mail
    )
}