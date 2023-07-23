package com.retheviper.bbs.common.api

import com.retheviper.bbs.common.model.Count
import com.retheviper.bbs.common.platform.DesktopPlatform
import com.retheviper.bbs.getHttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody

object ApiCaller {

    private const val apiUrl = "http://0.0.0.0:8080/api/v1"

    suspend fun postCount(number: Int) {
        getHttpClient().use {
            it.post("$apiUrl/count") {
                setBody(
                    Count(
                        platform = DesktopPlatform.name,
                        number = number
                    )
                )
            }
        }
    }
}