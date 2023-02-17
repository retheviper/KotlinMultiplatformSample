package com.retheviper.bbs.board.route

import com.retheviper.bbs.constant.ErrorCode
import com.retheviper.bbs.model.request.CreateArticleRequest
import com.retheviper.bbs.model.response.ExceptionResponse
import com.retheviper.bbs.testing.jsonClient
import com.retheviper.bbs.testing.postJson
import com.retheviper.bbs.testing.toToken
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication

class ArticleRouterTest : FreeSpec({

    "Create article" - {

        val request = CreateArticleRequest(
            title = "title",
            content = "test content",
            password = "password",
            authorId = 1
        )

        "OK" {
            testApplication {
                val client = jsonClient()

                val response = client.postJson("/api/v1/board/article") {
                    headers["Authorization"] = "Bearer ${"username".toToken()}"
                    setBody(request)
                }

                response.status shouldBe HttpStatusCode.Created
            }
        }

        "NG - Authentication failed (No token)" {
            testApplication {
                val client = jsonClient()

                val response = client.postJson("/api/v1/board/article") {
                    setBody(request)
                }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        "NG - Title is empty" {
            testApplication {
                val client = jsonClient()

                val response = client.postJson("/api/v1/board/article") {
                    headers["Authorization"] = "Bearer ${"username".toToken()}"
                    setBody(request.copy(title = ""))
                }

                response.status shouldBe HttpStatusCode.BadRequest
                response.body<ExceptionResponse>().let {
                    it.code shouldBe ErrorCode.INVALID_PARAMETER.value
                    it.message shouldBe "Article title is empty."
                }
            }
        }

        "NG - Content is empty" {
            testApplication {
                val client = jsonClient()

                val response = client.postJson("/api/v1/board/article") {
                    headers["Authorization"] = "Bearer ${"username".toToken()}"
                    setBody(request.copy(content = ""))
                }

                response.status shouldBe HttpStatusCode.BadRequest
                response.body<ExceptionResponse>().let {
                    it.code shouldBe ErrorCode.INVALID_PARAMETER.value
                    it.message shouldBe "Article content is empty."
                }
            }
        }

        "NG - Password is empty" {
            testApplication {
                val client = jsonClient()

                val response = client.postJson("/api/v1/board/article") {
                    headers["Authorization"] = "Bearer ${"username".toToken()}"
                    setBody(request.copy(password = ""))
                }

                response.status shouldBe HttpStatusCode.BadRequest
                response.body<ExceptionResponse>().let {
                    it.code shouldBe ErrorCode.INVALID_PARAMETER.value
                    it.message shouldBe "Article password is empty."
                }
            }
        }
    }
})