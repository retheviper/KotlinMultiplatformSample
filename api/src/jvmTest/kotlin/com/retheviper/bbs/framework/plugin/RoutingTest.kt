package com.retheviper.bbs.framework.plugin

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication

class RoutingTest : FreeSpec({
    "GET / should return 200 and text/html" {
        testApplication {
            val response = client.get("/")

            response.status shouldBe HttpStatusCode.OK
            response.headers["Content-Type"] shouldBe "text/html; charset=UTF-8"
        }
    }
})