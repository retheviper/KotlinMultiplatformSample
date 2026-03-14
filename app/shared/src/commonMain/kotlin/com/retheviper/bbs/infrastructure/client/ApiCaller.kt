package com.retheviper.bbs.infrastructure.client

import com.retheviper.bbs.constant.API_BASE_PATH
import com.retheviper.bbs.constant.COUNT
import com.retheviper.bbs.constant.PlatformName
import com.retheviper.bbs.getPlatform
import com.retheviper.bbs.model.request.CountRequest
import io.ktor.client.request.*

object ApiCaller {

    private val platform = getPlatform()

    private val apiUrl: String
        get() = when (platform.name) {
            PlatformName.ANDROID -> "http://10.0.2.2:8080$API_BASE_PATH"
            PlatformName.IOS -> "http://0.0.0.0:8080$API_BASE_PATH"
            PlatformName.DESKTOP -> "http://0.0.0.0:8080$API_BASE_PATH"
            else -> ""
        }

    suspend fun postCount(number: Int) {
        getHttpClient().use {
            it.post("$apiUrl$COUNT") {
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
