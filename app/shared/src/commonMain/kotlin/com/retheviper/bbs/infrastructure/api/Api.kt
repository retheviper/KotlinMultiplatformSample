package com.retheviper.bbs.infrastructure.api

import com.retheviper.bbs.constant.PlatformName
import com.retheviper.bbs.getPlatform

interface Api {

    val apiBaseUrl: String
        get() = when (getPlatform().name) {
            PlatformName.ANDROID -> "http://10.0.2.2:8080/api/v1"
            PlatformName.IOS -> "http://0.0.0.0:8080/api/v1"
            PlatformName.DESKTOP -> "http://0.0.0.0:8080/api/v1"
            else -> ""
        }
}