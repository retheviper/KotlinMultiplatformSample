package com.retheviper.bbs

import com.retheviper.bbs.constant.PlatformName
import io.ktor.client.*

interface Platform {
    val name: PlatformName
    val nameWithVersion: String
}

expect fun getPlatform(): Platform