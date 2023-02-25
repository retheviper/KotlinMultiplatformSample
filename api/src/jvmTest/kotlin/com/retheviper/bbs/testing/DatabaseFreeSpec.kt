package com.retheviper.bbs.testing

import com.retheviper.bbs.framework.plugin.hikariConfig
import io.kotest.core.spec.style.FreeSpec
import org.jetbrains.exposed.sql.Database

abstract class DatabaseFreeSpec(body: FreeSpec.() -> Unit = {}) : FreeSpec(body) {
    init {
        Database.connect(
            url = hikariConfig.jdbcUrl,
            driver = hikariConfig.driverClassName
        )
    }
}