package com.retheviper.bbs.infrastructure.client

import com.retheviper.bbs.constant.PlatformName
import com.retheviper.bbs.getPlatform
import com.retheviper.bbs.infrastructure.model.request.CountRequest
import io.ktor.client.request.*
import io.ktor.utils.io.core.use

object ApiCaller {

    private val platform = getPlatform()

    private val apiUrl: String
        get() = when (platform.name) {
            PlatformName.ANDROID -> "http://10.0.2.2:8080/api/v1"
            PlatformName.IOS -> "http://0.0.0.0:8080/api/v1"
            PlatformName.DESKTOP -> "http://0.0.0.0:8080/api/v1"
            else -> ""
        }

    suspend fun postCount(number: Int) {
        getHttpClient().use {
            it.post("$apiUrl/count") {
                setBody(
                    CountRequest(
                        platform = platform.name.value,
                        number = number
                    )
                )
            }
        }
    }
}