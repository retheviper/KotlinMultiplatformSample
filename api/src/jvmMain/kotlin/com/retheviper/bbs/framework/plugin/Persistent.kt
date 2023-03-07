package com.retheviper.bbs.framework.plugin

import com.retheviper.bbs.common.extension.getAllTables
import com.retheviper.bbs.common.extension.getDatabaseConfigs
import com.retheviper.bbs.common.property.DatabaseConfigs
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Schema
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configurePersistent() {

    val configs = getDatabaseConfigs()

    Database.connect(getDataSource(configs))
    val schema = Schema("bbs")

    transaction {
        SchemaUtils.setSchema(schema)
        SchemaUtils.create(*getAllTables())
    }
}

fun getDataSource(databaseConfigs: DatabaseConfigs): HikariDataSource {
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = databaseConfigs.url
        driverClassName = databaseConfigs.driver
        username = databaseConfigs.username
        password = databaseConfigs.password
        maximumPoolSize = databaseConfigs.maximumPoolSize
        maxLifetime = databaseConfigs.maxLifetime
        connectionTimeout = databaseConfigs.connectionTimeout
        idleTimeout = databaseConfigs.idleTimeout
    }

    return HikariDataSource(hikariConfig)
}