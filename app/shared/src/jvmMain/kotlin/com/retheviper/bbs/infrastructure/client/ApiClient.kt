package com.retheviper.bbs.infrastructure.client

import com.retheviper.bbs.getPlatform
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json


actual fun getHttpClient(): HttpClient {
    return HttpClient(Apache) {
        install(ContentNegotiation) {
            json()
        }
        defaultRequest {
            header("X-Platform", getPlatform().nameWithVersion)
            contentType(ContentType.Application.Json)
            url {
                host = "localhost"
                port = 8080
            }
        }
        expectSuccess = true
        HttpResponseValidator {
            handleResponseExceptionWithRequest { exception, _ ->
                val clientException = exception as? ClientRequestException ?: return@handleResponseExceptionWithRequest
                val exceptionResponse = clientException.response.bodyAsText()
                throw RuntimeException(exceptionResponse)
            }
        }

    }
}