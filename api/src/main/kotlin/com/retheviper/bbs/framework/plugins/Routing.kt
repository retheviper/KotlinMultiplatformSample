package com.retheviper.bbs.framework.plugins

import com.retheviper.bbs.user.web.authentication
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting() {

    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        // Static plugin. Try to access `/static/index.html`
        static("/static") {
            resources("static")
        }

        authentication(this@configureRouting)
    }
}
