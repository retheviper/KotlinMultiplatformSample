package com.retheviper.bbs.infrastructure.client

import com.retheviper.bbs.constant.PlatformName
import com.retheviper.bbs.getHttpClient
import com.retheviper.bbs.getPlatform
import com.retheviper.bbs.infrastructure.model.Count
import io.ktor.client.request.*

class ApiCaller {

    private val client = getHttpClient()

    private val platform = getPlatform()

    private val apiUrl: String
        get() = when (platform.name) {
            PlatformName.ANDROID -> "http://10.0.2.2:8080/api/v1"
            PlatformName.IOS -> "http://0.0.0.0:8080/api/v1"
            else -> ""
        }

    suspend fun postCount(number: Int) {
        client.post("$apiUrl/count") {
            setBody(
                Count(
                    platform = platform.name.value,
                    number = number
                )
            )
        }
    }
}