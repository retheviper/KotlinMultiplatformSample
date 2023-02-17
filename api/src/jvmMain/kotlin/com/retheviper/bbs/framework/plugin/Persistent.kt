package com.retheviper.bbs.framework.plugin

import com.retheviper.bbs.common.infrastructure.table.Articles
import com.retheviper.bbs.common.infrastructure.table.Comments
import com.retheviper.bbs.common.infrastructure.table.Users
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configurePersistent() {

    Database.connect(
        url = "jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver"
    )

    transaction {
        SchemaUtils.create(Users, Articles, Comments)
    }
}