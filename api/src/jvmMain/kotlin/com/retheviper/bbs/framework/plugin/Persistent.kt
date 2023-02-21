package com.retheviper.bbs.framework.plugin

import com.retheviper.bbs.common.infrastructure.table.Articles
import com.retheviper.bbs.common.infrastructure.table.Comments
import com.retheviper.bbs.common.infrastructure.table.Users
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configurePersistent() {

    val config = HikariConfig().apply {
        jdbcUrl = "jdbc:h2:./database"
        driverClassName = "org.h2.Driver"
        maximumPoolSize = 10
    }

    Database.connect(HikariDataSource(config))

    transaction {
        SchemaUtils.create(Users, Articles, Comments)
    }
}