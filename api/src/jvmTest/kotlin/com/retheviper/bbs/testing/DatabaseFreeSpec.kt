package com.retheviper.bbs.testing

import io.kotest.core.spec.style.FreeSpec
import org.jetbrains.exposed.sql.Database

abstract class DatabaseFreeSpec(body: FreeSpec.() -> Unit = {}) : FreeSpec(body) {
    init {
        Database.connect(
            url = "jdbc:mysql://localhost:3131/bbs?useUnicode=true&characterEncoding=utf8&allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC",
            driver = "com.mysql.cj.jdbc.Driver"
        )
    }
}