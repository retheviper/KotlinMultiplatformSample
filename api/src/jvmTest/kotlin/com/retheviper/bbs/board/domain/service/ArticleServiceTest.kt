package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.infrastructure.repository.ArticleRepository
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.testing.DatabaseFreeSpec
import com.retheviper.bbs.testing.TestModelFactory
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class ArticleServiceTest : DatabaseFreeSpec({

                                                "findAll" {
                                                    val (page, pageSize, limit) = Triple(1, 1, 10)
                                                    val articleRecord = TestModelFactory.articleRecordModel(
                                                        id = ArticleId(1), authorId = UserId(1)
                                                    )

                                                    val repository = mockk<ArticleRepository> {
                                                        every {
                                                            findAll(
                                                                authorId = null,
                                                                page = page,
                                                                pageSize = pageSize,
                                                                limit = limit
                                                            )
                                                        } returns listOf(articleRecord)
                                                    }
                                                    val commentService = mockk<CommentService> {
                                                        every { findAll(listOf(articleRecord.id)) } returns emptyList()
                                                    }
                                                    val service = ArticleService(commentService, repository)

                                                    val articles = service.findAll(
                                                        authorId = null, page = page, pageSize = pageSize, limit = limit
                                                    )

                                                    articles shouldBe listOf(
                                                        Article(
                                                            id = articleRecord.id,
                                                            title = articleRecord.title,
                                                            content = articleRecord.content,
                                                            password = articleRecord.password,
                                                            authorId = articleRecord.authorId,
                                                            authorName = articleRecord.authorName,
                                                            comments = emptyList()
                                                        )
                                                    )

                                                    verify(exactly = 1) {
                                                        repository.findAll(
                                                            authorId = null,
                                                            page = page,
                                                            pageSize = pageSize,
                                                            limit = limit
                                                        )
                                                        commentService.findAll(listOf(articleRecord.id))
                                                    }
                                                }

                                                "find" {
                                                    val articleRecord = TestModelFactory.articleRecordModel(
                                                        id = ArticleId(1), authorId = UserId(1)
                                                    )
                                                    val repository = mockk<ArticleRepository> {
                                                        every { find(articleRecord.id) } returns articleRecord
                                                    }
                                                    val commentService = mockk<CommentService> {
                                                        every { findAll(articleRecord.id) } returns emptyList()
                                                    }
                                                    val service = ArticleService(commentService, repository)

                                                    val article = service.find(articleRecord.id)

                                                    article shouldBe Article(
                                                        id = articleRecord.id,
                                                        title = articleRecord.title,
                                                        content = articleRecord.content,
                                                        password = articleRecord.password,
                                                        authorId = articleRecord.authorId,
                                                        authorName = articleRecord.authorName,
                                                        comments = emptyList()
                                                    )
                                                }
                                            })