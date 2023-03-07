package com.retheviper.bbs.common.property

class DatabaseConfigs(
    val url: String,
    val driver: String,
    val username: String,
    val password: String,
    val maximumPoolSize: Int,
    val maxLifetime: Long,
    val connectionTimeout: Long,
    val idleTimeout: Long
)