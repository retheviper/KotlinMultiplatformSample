package com.retheviper.bbs.framework.plugin

import com.retheviper.bbs.common.extension.getAllTabes
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction


val hikariConfig = HikariConfig().apply {
    jdbcUrl = "jdbc:h2:./database"
    driverClassName = "org.h2.Driver"
    maximumPoolSize = 10
}

fun Application.configurePersistent() {


    Database.connect(HikariDataSource(hikariConfig))

    transaction {
        SchemaUtils.create(*getAllTabes())
    }
}