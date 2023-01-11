package com.retheviper.bbs

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform