package com.retheviper.bbs.framework.plugin

import com.retheviper.bbs.common.extension.getAllTables
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Schema
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction


val hikariConfig = HikariConfig().apply {
    jdbcUrl = "jdbc:mysql://localhost:3131/bbs"
    driverClassName = "com.mysql.cj.jdbc.Driver"
    username = "root"
    password = "password"
    maximumPoolSize = 10
}

fun Application.configurePersistent() {

    Database.connect(HikariDataSource(hikariConfig))
    val schema = Schema("bbs")

    transaction {
        SchemaUtils.setSchema(schema)
        SchemaUtils.create(*getAllTables())
    }
}