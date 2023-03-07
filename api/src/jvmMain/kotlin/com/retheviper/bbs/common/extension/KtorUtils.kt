package com.retheviper.bbs.common.extension

import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.property.DatabaseConfigs
import com.retheviper.bbs.common.property.JwtConfigs
import com.retheviper.bbs.model.common.PaginationProperties
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest

fun Application.getEnvironmentVariable(key: String): String = environment.config.property(key).getString()

fun Application.getJwtConfigs(): JwtConfigs {
    val secret = getEnvironmentVariable("jwt.secret")
    val issuer = getEnvironmentVariable("jwt.issuer")
    val audience = getEnvironmentVariable("jwt.audience")
    val realm = getEnvironmentVariable("jwt.realm")
    return JwtConfigs(secret = secret, issuer = issuer, audience = audience, realm = realm)
}

fun Application.getDatabaseConfigs(): DatabaseConfigs {
    val url = getEnvironmentVariable("database.url")
    val driver = getEnvironmentVariable("database.driver")
    val username = getEnvironmentVariable("database.username")
    val password = getEnvironmentVariable("database.password")
    val maximumPoolSize = getEnvironmentVariable("database.maximumPoolSize").toInt()
    val maxLifetime = getEnvironmentVariable("database.maxLifetime").toLong()
    val connectionTimeout = getEnvironmentVariable("database.connectionTimeout").toLong()
    val idleTimeout = getEnvironmentVariable("database.idleTimeout").toLong()
    return DatabaseConfigs(
        url = url,
        driver = driver,
        username = username,
        password = password,
        maximumPoolSize = maximumPoolSize,
        maxLifetime = maxLifetime,
        connectionTimeout = connectionTimeout,
        idleTimeout = idleTimeout
    )
}

@Throws(BadRequestException::class)
fun ApplicationCall.getPaginationProperties(): PaginationProperties {
    val page = request.queryParametersAsInt("page") ?: 1
    val size = request.queryParametersAsInt("size") ?: 10
    val limit = request.queryParametersAsInt("limit") ?: 100

    if (page < 1 || size < 1 || limit < 1) {
        throw BadRequestException("Invalid page, size or limit.")
    }

    if (size > limit) {
        throw BadRequestException("size must be less than or equal to limit.")
    }

    if (page > limit / size) {
        throw BadRequestException("page must be less than or equal to limit / pageSize.")
    }

    return PaginationProperties(page = page, size = size, limit = limit)
}

@Throws(BadRequestException::class)
fun ApplicationCall.getIdFromParameter(): Int {
    val parameter = parameters["id"] ?: throw BadRequestException("Invalid ID")
    return try {
        val id = parameter.toInt()
        if (id < 1) {
            throw BadRequestException("Invalid ID")
        } else {
            id
        }
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