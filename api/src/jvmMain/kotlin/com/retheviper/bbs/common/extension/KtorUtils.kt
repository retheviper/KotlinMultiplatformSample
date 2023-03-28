package com.retheviper.bbs.common.extension

import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.property.DatabaseConfigs
import com.retheviper.bbs.common.property.JwtConfigs
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.BoardId
import com.retheviper.bbs.common.value.CategoryId
import com.retheviper.bbs.common.value.CommentId
import com.retheviper.bbs.common.value.Id
import com.retheviper.bbs.common.value.MessageGroupId
import com.retheviper.bbs.common.value.TagId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.model.common.PaginationProperties
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.ApplicationRequest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
    val page = request.getQueryParameter("page") ?: 1
    val size = request.getQueryParameter("size") ?: 10
    val limit = request.getQueryParameter("limit") ?: 100

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
inline fun <reified T : Id> ApplicationCall.getIdFromPathParameter(): T {
    val badRequestException = BadRequestException("Invalid ID")

    val parameterName = when (T::class) {
        BoardId::class -> "boardId"
        ArticleId::class -> "articleId"
        CommentId::class -> "commentId"
        CategoryId::class -> "categoryId"
        TagId::class -> "tagId"
        UserId::class -> "userId"
        MessageGroupId::class -> "messageGroupId"
        else -> throw badRequestException
    }

    val parameter = parameters[parameterName] ?: throw badRequestException
    val id = parameter.toIntOrNull() ?: throw badRequestException

    return if (id < 1) {
        throw badRequestException
    } else {
        T::class.java.getDeclaredConstructor(Int::class.java).apply { isAccessible = true }.newInstance(id)
    }
}

inline fun <reified T: Any> ApplicationRequest.getQueryParameter(key: String): T? {
    val parameter = queryParameters[key]

    return when (T::class) {
        String::class -> parameter as T?
        Long::class -> parameter?.toLongOrNull() as T?
        Int::class -> parameter?.toIntOrNull() as T?
        LocalDateTime::class -> parameter?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) } as T?
        else -> null
    }
}

fun ApplicationCall.getUserInfoFromToken(): Pair<UserId, String> {
    val badRequestException = BadRequestException("Invalid token")
    val principal = principal<JWTPrincipal>() ?: throw badRequestException
    val userId = principal.payload.getClaim("userId").asInt() ?: throw badRequestException
    val username = principal.payload.getClaim("username").asString() ?: throw badRequestException
    return Pair(UserId(userId), username)
}