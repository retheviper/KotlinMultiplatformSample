@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.retheviper.chat.plugins

import com.retheviper.chat.contract.HealthResponse
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.http.content.staticFiles
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respondFile
import io.ktor.server.response.respond
import io.ktor.server.response.respondResource
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.plugins.swagger.swaggerUI
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

fun Application.configureRouting() {
    val frontendDirectory = resolveFrontendDirectory()

    routing {
        get("/health") {
            call.respond(HealthResponse(status = "ok", service = "messaging-api", version = "v1"))
        }

        get("/openapi.yaml") {
            val resource = this::class.java.classLoader
                .getResource("openapi/messaging-api.yaml")
                ?.readText()
                ?: error("openapi resource missing")
            call.respondText(resource, io.ktor.http.ContentType.Application.Yaml)
        }

        get("/") {
            val indexFile = frontendDirectory?.resolve("index.html")
            if (indexFile != null && indexFile.exists()) {
                call.respondFile(indexFile.toFile())
            } else {
                call.respondResource("web/index.html")
            }
        }

        if (frontendDirectory != null) {
            log.info("Serving frontend assets from {}", frontendDirectory)
            staticFiles("/", frontendDirectory.toFile())
        } else {
            staticResources("/", "web")
        }

        swaggerUI(path = "docs", swaggerFile = "openapi/messaging-api.yaml")
    }
}

private fun Application.resolveFrontendDirectory(): Path? {
    val configuredDirectory = environment.config.propertyOrNull("messaging.frontend.devDir")?.getString()?.trim().orEmpty()
    val candidates = buildList {
        if (configuredDirectory.isNotEmpty()) {
            add(Path.of(configuredDirectory))
        }
        add(Path.of(System.getProperty("user.dir"), "shared", "build", "dist", "wasmJs", "developmentExecutable"))
    }

    return candidates.firstOrNull { it.exists() && it.isDirectory() }
}
