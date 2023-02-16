package com.retheviper.bbs.testing

import io.kotest.core.spec.style.FreeSpec
import org.jetbrains.exposed.sql.Database

abstract class FreeSpecWithDb(body: FreeSpec.() -> Unit = {}) : FreeSpec(body) {
    init {
        Database.connect(
            url = "jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver"
        )
    }
}