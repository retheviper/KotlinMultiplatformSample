package com.retheviper.bbs.board.infrastructure.repository

import com.retheviper.bbs.board.infrastructure.model.ArticleRecord
import com.retheviper.bbs.common.extension.getAllTables
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.CategoryId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.testing.KtorFreeSpec
import com.retheviper.bbs.testing.TestModelFactory
import com.retheviper.bbs.testing.dropAndCreate
import com.retheviper.bbs.user.infrastructure.repository.UserRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject

class ArticleRepositoryTest : KtorFreeSpec({

    val userId = UserId(1)
    val articleId = ArticleId(1)
    val category = TestModelFactory.categoryModel().copy(id = CategoryId(1))

    beforeSpec {
        testApplication {
            application {
                val userRepository by inject<UserRepository>()
                val categoryRepository by inject<CategoryRepository>()
                val articleRepository by inject<ArticleRepository>()
                transaction {
                    SchemaUtils.dropAndCreate(*getAllTables())
                    userRepository.create(TestModelFactory.userModel())
                    categoryRepository.create(category.name, category.description)
                    articleRepository.create(TestModelFactory.articleModel(userId, category))
                }
            }
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
                            paginationProperties = TestModelFactory.paginationPropertiesModel()
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
                            paginationProperties = TestModelFactory.paginationPropertiesModel()
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
                    val article = TestModelFactory.articleModel(userId, category)
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
                    val article = TestModelFactory.articleModel(userId, category).copy(id = articleId)
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
})