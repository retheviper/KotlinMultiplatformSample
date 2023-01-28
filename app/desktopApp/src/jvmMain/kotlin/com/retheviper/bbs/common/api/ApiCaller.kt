package com.retheviper.bbs.common.api

import com.retheviper.bbs.common.model.Count
import com.retheviper.bbs.common.platform.DesktopPlatform
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json

object ApiCaller {

    private const val apiUrl = "http://0.0.0.0:8080/api/v1"

    private val client = HttpClient(Apache) {
        install(ContentNegotiation) {
            json()
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun postCount(number: Int) {
        client.post("$apiUrl/count") {
            setBody(
                Count(
                    platform = DesktopPlatform.name,
                    number = number
                )
            )
        }
    }
}