package com.retheviper.bbs.client

import com.retheviper.bbs.constant.API_BASE_PATH
import com.retheviper.bbs.constant.COUNT
import com.retheviper.bbs.model.Count
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.browser.window
import kotlinx.serialization.json.Json

val API_URL = "${window.location.origin}$API_BASE_PATH"

val jsonClient = HttpClient {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
            }
        )
    }
    defaultRequest {
        contentType(ContentType.Application.Json)
    }
}

suspend fun postCount(number: Int) {
    jsonClient.post("$API_URL$COUNT") {
        setBody(Count(number))
    }
}