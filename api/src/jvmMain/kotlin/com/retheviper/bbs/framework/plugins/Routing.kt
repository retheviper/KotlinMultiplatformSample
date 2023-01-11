package com.retheviper.bbs.framework.plugins

import com.retheviper.bbs.user.web.authentication
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
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

        authentication(this@configureRouting)
    }
}
