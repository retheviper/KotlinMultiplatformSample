package com.retheviper.bbs.board.infrastructure.repository

import com.retheviper.bbs.common.infrastructure.table.Articles
import com.retheviper.bbs.common.infrastructure.table.Comments
import com.retheviper.bbs.common.infrastructure.table.Users
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.testing.KtorFreeSpec
import com.retheviper.bbs.testing.TestModelFactory
import com.retheviper.bbs.user.infrastructure.repository.UserRepository
import io.kotest.matchers.shouldNotBe
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject

class ArticleRepositoryTest : KtorFreeSpec() {

    init {
        beforeSpec {
            testApplication {
                application {
                    val userRepository by inject<UserRepository>()
                    val articleRepository by inject<ArticleRepository>()
                    transaction {
                        SchemaUtils.create(Articles, Users, Comments)
                        userRepository.create(TestModelFactory.userModel())
                        articleRepository.create(TestModelFactory.articleModel(UserId(1)))
                    }
                }
            }
        }

        afterSpec {
            transaction {
                SchemaUtils.drop(Articles, Users, Comments)
            }
        }

        "find" - {
            "OK" {
                testApplication {
                    application {
                        val repository by inject<ArticleRepository>()
                        val result = transaction { repository.find(ArticleId(1)) }
                        result shouldNotBe null
                    }
                }
            }
        }

        "create" - {
            "OK" {
                testApplication {
                    application {
                        val repository by inject<ArticleRepository>()
                        val article = TestModelFactory.articleModel(UserId(1))
                        val result = transaction { repository.create(article) }
                        result shouldNotBe null
                    }
                }
            }
        }
    }
}