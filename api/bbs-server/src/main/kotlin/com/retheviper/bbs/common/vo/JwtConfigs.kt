package com.retheviper.bbs.common.vo

data class JwtConfigs(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String
)