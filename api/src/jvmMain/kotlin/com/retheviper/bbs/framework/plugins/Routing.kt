package com.retheviper.bbs.framework.plugins

import com.retheviper.bbs.constant.API_BASE_PATH
import com.retheviper.bbs.constant.COUNT
import com.retheviper.bbs.model.Count
import com.retheviper.bbs.user.web.authentication
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting() {

    routing {
        get {
            call.respondRedirect("/index.html")
        }

        // Static plugin. Try to access `/static/index.html`
        static {
            resources("")
        }

        route(API_BASE_PATH) {
            post(COUNT) {
                val request = call.receive<Count>()
                call.application.environment.log.info("Current count from ${request.platform} is: ${request.number}")
                call.respond(HttpStatusCode.Accepted)
            }
        }

        authentication(this@configureRouting)
    }
}