package com.retheviper.bbs.testing

import io.kotest.core.spec.style.FreeSpec
import org.jetbrains.exposed.sql.Database

abstract class DatabaseFreeSpec(body: FreeSpec.() -> Unit = {}) : FreeSpec(body) {
    init {
        Database.connect(
            url = "jdbc:h2:./database",
            driver = "org.h2.Driver"
        )
    }
}