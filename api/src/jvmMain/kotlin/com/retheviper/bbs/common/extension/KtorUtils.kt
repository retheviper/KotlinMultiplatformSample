package com.retheviper.bbs.common.extension

import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.property.JwtConfigs
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest

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
    val page = request.queryParametersAsInt("page") ?: 1
    val pageSize = request.queryParametersAsInt("pageSize") ?: 10
    val limit = request.queryParametersAsInt("limit") ?: 100

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
    val parameter = parameters["id"] ?: throw BadRequestException("Invalid ID")
    return try {
        parameter.toInt()
    } catch (e: NumberFormatException) {
        throw BadRequestException("Invalid ID")
    }
}

fun ApplicationRequest.queryParametersAsInt(key: String): Int? {
    val parameter = queryParameters[key]
    return try {
        parameter?.toInt()
    } catch (e: NumberFormatException) {
        throw BadRequestException("Invalid $key")
    }
}