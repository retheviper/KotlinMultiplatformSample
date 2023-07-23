package com.retheviper.bbs.infrastructure.api

import com.retheviper.bbs.infrastructure.client.getHttpClient
import com.retheviper.bbs.infrastructure.model.request.CreateUserRequest
import com.retheviper.bbs.infrastructure.model.response.UserResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.utils.io.core.use


object UserApi : Api {

    private val endpoint = "$apiBaseUrl/user"

    suspend fun getUser(id: Int): UserResponse {
        return getHttpClient().use {
            it.get("$endpoint/$id").body()
        }
    }

    suspend fun createUser(request: CreateUserRequest): UserResponse {
        return getHttpClient().use {
            it.post(endpoint) {
                setBody(request)
            }
        }.body()
    }
}