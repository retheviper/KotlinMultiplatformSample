package com.retheviper.chat.config

import io.ktor.server.config.ApplicationConfig

data class DatabaseSettings(
    val jdbcUrl: String,
    val r2dbcUrl: String,
    val username: String,
    val password: String
)

data class BootstrapSettings(
    val sampleDataEnabled: Boolean
)

data class ApiConfig(
    val database: DatabaseSettings,
    val bootstrap: BootstrapSettings
) {
    companion object {
        fun from(config: ApplicationConfig): ApiConfig {
            return ApiConfig(
                database = DatabaseSettings(
                    jdbcUrl = config.property("messaging.database.jdbcUrl").getString(),
                    r2dbcUrl = config.property("messaging.database.r2dbcUrl").getString(),
                    username = config.property("messaging.database.username").getString(),
                    password = config.property("messaging.database.password").getString()
                ),
                bootstrap = BootstrapSettings(
                    sampleDataEnabled = config.propertyOrNull("messaging.bootstrap.sampleDataEnabled")
                        ?.getString()
                        ?.toBooleanStrictOrNull()
                        ?: true
                )
            )
        }
    }
}
