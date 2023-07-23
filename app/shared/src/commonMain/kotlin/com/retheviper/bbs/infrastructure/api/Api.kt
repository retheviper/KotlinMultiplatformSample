package com.retheviper.bbs.infrastructure.api

interface Api {

    val basePath: String
        get() = "/api/v1"
}