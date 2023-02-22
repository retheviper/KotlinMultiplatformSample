package com.retheviper.bbs.board.infrastructure.repository

import com.retheviper.bbs.board.infrastructure.model.ArticleRecord
import com.retheviper.bbs.common.infrastructure.table.Articles
import com.retheviper.bbs.common.infrastructure.table.Comments
import com.retheviper.bbs.common.infrastructure.table.Users
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.testing.KtorFreeSpec
import com.retheviper.bbs.testing.TestModelFactory
import com.retheviper.bbs.user.infrastructure.repository.UserRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject

class ArticleRepositoryTest : KtorFreeSpec() {

    private val userId = UserId(1)
    private val articleId = ArticleId(1)

    init {
        beforeSpec {
            testApplication {
                application {
                    val userRepository by inject<UserRepository>()
                    val articleRepository by inject<ArticleRepository>()
                    transaction {
                        SchemaUtils.create(Articles, Users, Comments)
                        userRepository.create(TestModelFactory.userModel())
                        articleRepository.create(TestModelFactory.articleModel(userId))
                    }
                }
            }
        }

        afterSpec {
            transaction {
                SchemaUtils.drop(Articles, Users, Comments)
            }
        }

        "count" - {
            "OK" {
                testApplication {
                    application {
                        val repository by inject<ArticleRepository>()
                        val result = transaction { repository.count(userId) }
                        result shouldBe 1
                    }
                }
            }
        }

        "find" - {
            "OK" {
                testApplication {
                    application {
                        val repository by inject<ArticleRepository>()
                        val result = transaction { repository.find(articleId) }
                        result shouldNotBe null
                    }
                }
            }
        }

        "findAll" - {
            "OK - with authorId" {
                testApplication {
                    application {
                        val repository by inject<ArticleRepository>()
                        val result = transaction {
                            repository.findAll(
                                authorId = userId,
                                page = 1,
                                pageSize = 10,
                                limit = 100
                            )
                        }
                        result shouldNotBe emptyList<ArticleRecord>()
                    }
                }
            }

            "OK - without authorId" {
                testApplication {
                    application {
                        val repository by inject<ArticleRepository>()
                        val result = transaction {
                            repository.findAll(
                                authorId = null,
                                page = 1,
                                pageSize = 10,
                                limit = 100
                            )
                        }
                        result shouldNotBe emptyList<ArticleRecord>()
                    }
                }
            }
        }

        "create" - {
            "OK" {
                testApplication {
                    application {
                        val repository by inject<ArticleRepository>()
                        val article = TestModelFactory.articleModel(userId)
                        val result = transaction { repository.create(article) }
                        result shouldNotBe null
                    }
                }
            }
        }

        "update" - {
            "OK" {
                testApplication {
                    application {
                        val repository by inject<ArticleRepository>()
                        val article = TestModelFactory.articleModel(userId).copy(id = articleId)
                        transaction { repository.update(article) }

                        val result = transaction { repository.find(articleId) }
                        result shouldNotBe null
                        result?.let {
                            it.title shouldBe article.title
                            it.content shouldBe article.content
                        }
                    }
                }
            }
        }

        "delete" - {
            "OK" {
                testApplication {
                    application {
                        val repository by inject<ArticleRepository>()
                        transaction { repository.delete(articleId) }

                        val result = transaction { repository.find(articleId) }
                        result shouldBe null
                    }
                }
            }
        }
    }
}