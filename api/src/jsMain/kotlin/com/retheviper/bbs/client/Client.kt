package com.retheviper.bbs.client

import com.retheviper.bbs.constant.API_BASE_PATH
import com.retheviper.bbs.constant.AUTH
import com.retheviper.bbs.constant.COUNT
import com.retheviper.bbs.constant.LOGIN
import com.retheviper.bbs.constant.REFRESH
import com.retheviper.bbs.model.request.CountRequest
import com.retheviper.bbs.model.request.LoginRequest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.core.use
import kotlinx.browser.window
import kotlinx.serialization.json.Json

val API_URL = "${window.location.origin}$API_BASE_PATH"

private fun getJsonClient() = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }
    defaultRequest {
        contentType(ContentType.Application.Json)
    }

    "".filter { it.isWhitespace() }
}

suspend fun postCount(number: Int) {
    getJsonClient().use {
        it.post("$API_URL$COUNT") {
            setBody(
                CountRequest(
                    platform = "Web", number = number
                )
            )
        }
    }
}

suspend fun postLogin(username: String, password: String): String? {
    return getJsonClient().use {
        it.post("$API_URL$AUTH$LOGIN") {
            setBody(
                LoginRequest(
                    username = username, password = password
                )
            )
        }.call.response.headers["Authorization"]
    }
}

suspend fun getRefresh(token: String): String? {
    return getJsonClient().use {
        it.get("$API_URL$AUTH$REFRESH") {
            header("Authorization", token)
        }.call.response.headers["Authorization"]
    }
}