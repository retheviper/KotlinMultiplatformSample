package com.retheviper.bbs.common.presentation.route

import com.retheviper.bbs.model.response.HealthResponse
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.routeHealth() {
    get("/health") {
        val databaseConnection = transaction {
            val connection = TransactionManager.current().connection
            val statement = connection.prepareStatement(
                sql = "SELECT * FROM DUAL;",
                returnKeys = true
            )
            if (statement.executeQuery().next()) {
                "connected"
            } else {
                "disconnect"
            }
        }

        call.application.log.info("Database connection: $databaseConnection")
        call.respond(
            HealthResponse(
                health = "OK",
                databaseConnection = databaseConnection
            )
        )
    }
}