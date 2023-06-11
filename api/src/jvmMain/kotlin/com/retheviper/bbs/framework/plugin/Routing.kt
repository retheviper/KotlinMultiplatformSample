package com.retheviper.bbs.framework.plugin

import com.retheviper.bbs.auth.presentation.route.routeAuth
import com.retheviper.bbs.board.presentation.route.routeBoard
import com.retheviper.bbs.common.presentation.route.routeHealth
import com.retheviper.bbs.constant.API_BASE_PATH
import com.retheviper.bbs.constant.COUNT
import com.retheviper.bbs.message.presentation.route.routeMessage
import com.retheviper.bbs.model.request.CountRequest
import com.retheviper.bbs.user.presentation.route.routeUser
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.log
import io.ktor.server.http.content.CompressedFileType
import io.ktor.server.http.content.staticFiles
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.io.File

fun Application.configureRouting() {

    routing {
        staticFiles("/", File("build/distributions")) {
            default("index.html")
            preCompressed(CompressedFileType.GZIP)
        }

        route(API_BASE_PATH) {
            post(COUNT) {
                val request = call.receive<CountRequest>()
                call.application.log.info("Current count from ${request.platform} is: ${request.number}")
                call.respond(HttpStatusCode.Accepted)
            }
            routeHealth()
            routeAuth()
            routeUser()
            routeBoard()
            routeMessage()
        }
    }
}
