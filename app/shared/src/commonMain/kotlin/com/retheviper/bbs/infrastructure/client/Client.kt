package com.retheviper.bbs.infrastructure.client

import com.retheviper.bbs.infrastructure.model.Count
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*

object Client {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun postCount(number: Int) {
        client.post("http://10.0.2.2:8080/api/v1/count") {
            setBody(Count(number))
        }
    }
}