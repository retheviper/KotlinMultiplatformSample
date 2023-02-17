package com.retheviper.bbs.testing

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.retheviper.bbs.common.property.JwtConfigs
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import java.util.Date

fun String.toToken(jwtConfig: JwtConfigs? = null): String = JWT.create()
    .withAudience(jwtConfig?.audience ?: "jwt-audience")
    .withIssuer(jwtConfig?.issuer ?: "ktor sample app")
    .withClaim("username", this)
    .withExpiresAt(Date(System.currentTimeMillis() + 10 * 60 * 1000))
    .sign(Algorithm.HMAC256(jwtConfig?.secret ?: "secret"))

fun ApplicationTestBuilder.jsonClient(): HttpClient {
    return createClient {
        install(ContentNegotiation) {
            json()
        }
    }
}

suspend fun HttpClient.postJson(
    url: String,
    block: HttpRequestBuilder.() -> Unit
): HttpResponse {
    return post(url) {
        contentType(ContentType.Application.Json)
        block()
    }
}