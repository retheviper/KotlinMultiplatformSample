package com.retheviper.bbs.board.infrastructure.repository

import com.retheviper.bbs.board.infrastructure.model.ArticleRecord
import com.retheviper.bbs.common.extension.getAllTables
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.BoardId
import com.retheviper.bbs.common.value.CategoryId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.testing.KtorFreeSpec
import com.retheviper.bbs.testing.TestModelFactory
import com.retheviper.bbs.testing.dropAndCreate
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject

class ArticleRepositoryTest : KtorFreeSpec({

    val boardId = BoardId(1)
    val userId = UserId(1)
    val articleId = ArticleId(1)
    val category = TestModelFactory.categoryModel().copy(id = CategoryId(1))

    beforeSpec {
        testApplication {
            application {
                transaction {
                    SchemaUtils.dropAndCreate(*getAllTables())
                    val createBoardQuery = "INSERT INTO Boards (created_by, created_date, deleted, last_modified_by, last_modified_date, `name`) VALUES ('system', '2023-03-07T16:08:32.494946', FALSE, 'system', '2023-03-07T16:08:32.494946', 'system')"
                    val createUserQuery = "INSERT INTO Users (created_by, created_date, deleted, last_modified_by, last_modified_date, mail, `name`, password, username) VALUES ('ZCRQD', '2023-03-07T16:08:32.494946', FALSE, 'ZCRQD', '2023-03-07T16:08:32.495029', 'KJMVSHXHV', 'ABIIAUPWSE', 'XIAP', 'ZCRQD')"
                    val createCategoryQuery = "INSERT INTO Categories (created_by, created_date, deleted, description, last_modified_by, last_modified_date, `name`, board_id) VALUES ('system', '2023-03-07T16:08:32.517222', FALSE, 'WYOGAGN', 'system', '2023-03-07T16:08:32.517229', 'BONNC', 1)"
                    val createArticleQuery = "INSERT INTO Articles (author_id, category_id, content, created_by, created_date, deleted, last_modified_by, last_modified_date, password, title, board_id) VALUES (1, 1, 'FOXIE', 'AMKHSBHUX', '2023-03-07T16:15:08.386278', FALSE, 'AMKHSBHUX', '2023-03-07T16:15:08.386287', 'CZYRWU', 'ZIXW', 1)"
                    exec(createBoardQuery)
                    exec(createUserQuery)
                    exec(createCategoryQuery)
                    exec(createArticleQuery)
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
                            boardId = boardId,
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
                            boardId = boardId,
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
                    val article = TestModelFactory.articleModel(category = category)
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
                    val article = TestModelFactory.articleModel(category = category).copy(id = articleId)
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