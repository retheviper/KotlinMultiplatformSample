package com.retheviper.chat.infrastructure.database

import com.retheviper.chat.config.DatabaseSettings
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

object Database {
    lateinit var database: R2dbcDatabase
        private set

    fun initialize(config: DatabaseSettings) {
        migrate(config)
        database = R2dbcDatabase.connect(
            url = config.r2dbcUrl,
            driver = "postgresql",
            user = config.username,
            password = config.password
        )
    }

    fun close() {
    }

    private fun migrate(config: DatabaseSettings) {
        Flyway.configure()
            .dataSource(config.jdbcUrl, config.username, config.password)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }
}
