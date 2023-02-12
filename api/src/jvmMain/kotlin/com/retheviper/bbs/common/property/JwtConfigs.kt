package com.retheviper.bbs.common.property

data class JwtConfigs(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String
)