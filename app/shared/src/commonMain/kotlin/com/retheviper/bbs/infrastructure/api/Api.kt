package com.retheviper.bbs.infrastructure.api

import com.retheviper.bbs.constant.API_BASE_PATH

interface Api {

    val basePath: String
        get() = API_BASE_PATH
}
