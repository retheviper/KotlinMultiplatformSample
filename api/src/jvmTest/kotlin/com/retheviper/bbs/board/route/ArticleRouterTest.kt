package com.retheviper.bbs.board.route

import com.retheviper.bbs.board.infrastructure.repository.ArticleRepository
import com.retheviper.bbs.board.infrastructure.repository.CommentRepository
import com.retheviper.bbs.common.infrastructure.table.Articles
import com.retheviper.bbs.common.infrastructure.table.Comments
import com.retheviper.bbs.common.infrastructure.table.Users
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.constant.ErrorCode
import com.retheviper.bbs.model.request.CreateArticleRequest
import com.retheviper.bbs.model.response.ExceptionResponse
import com.retheviper.bbs.model.response.GetArticleResponse
import com.retheviper.bbs.testing.KtorFreeSpec
import com.retheviper.bbs.testing.TestModelFactory
import com.retheviper.bbs.testing.jsonClient
import com.retheviper.bbs.testing.postJson
import com.retheviper.bbs.testing.toToken
import com.retheviper.bbs.user.infrastructure.repository.UserRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject

class ArticleRouterTest : KtorFreeSpec() {

    private val urlString = "/api/v1/board/article"
    private val user = TestModelFactory.userModel()
    private val article = TestModelFactory.articleModel(UserId(1))
    private val comment = TestModelFactory.commentModel(ArticleId(1), UserId(1))

    init {
        beforeSpec {
            testApplication {
                application {
                    val userRepository by inject<UserRepository>()
                    val articleRepository by inject<ArticleRepository>()
                    val commentRepository by inject<CommentRepository>()

                    transaction {
                        userRepository.create(user)
                        articleRepository.create(article)
                        commentRepository.create(comment)
                    }
                }
            }
        }

        afterSpec {
            transaction {
                SchemaUtils.drop(Users, Articles, Comments)
            }
        }

        "Find Article" - {
            "OK" {
                testApplication {
                    val client = jsonClient()

                    val response = client.get("$urlString/1")

                    response.status shouldBe HttpStatusCode.OK
                    response.body<GetArticleResponse>().let {
                        it.title shouldBe article.title
                        it.content shouldBe article.content
                        it.author shouldBe user.name
                        it.comments.first().content shouldBe comment.content
                        it.comments.first().author shouldBe user.name
                    }
                }
            }

            "NG - Article not found (Not exist ID)" {
                testApplication {
                    val client = jsonClient()

                    val response = client.get("$urlString/${Int.MAX_VALUE}")

                    response.status shouldBe HttpStatusCode.NotFound
                    response.body<ExceptionResponse>().let {
                        it.code shouldBe ErrorCode.ARTICLE_NOT_FOUND.value
                        it.message shouldContain "Article not found with id: ArticleId"
                    }
                }
            }

            "NG - Article not found (Invalid ID)" {
                testApplication {
                    val client = jsonClient()

                    val response = client.get("$urlString/a")

                    response.status shouldBe HttpStatusCode.BadRequest
                    response.body<ExceptionResponse>().let {
                        it.code shouldBe ErrorCode.INVALID_PARAMETER.value
                        it.message shouldBe "Invalid ID"
                    }
                }
            }
        }

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

                    val response = client.postJson(urlString) {
                        headers["Authorization"] = "Bearer ${"username".toToken()}"
                        setBody(request)
                    }

                    response.status shouldBe HttpStatusCode.Created
                }
            }

            "NG - Authentication failed (No token)" {
                testApplication {
                    val client = jsonClient()

                    val response = client.postJson(urlString) {
                        setBody(request)
                    }

                    response.status shouldBe HttpStatusCode.Unauthorized
                }
            }

            "NG - Title is empty" {
                testApplication {
                    val client = jsonClient()

                    val response = client.postJson(urlString) {
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

                    val response = client.postJson(urlString) {
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

                    val response = client.postJson(urlString) {
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
    }
}