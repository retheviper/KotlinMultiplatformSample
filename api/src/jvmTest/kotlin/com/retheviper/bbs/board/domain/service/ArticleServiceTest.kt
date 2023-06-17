package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.domain.model.Category
import com.retheviper.bbs.board.domain.model.Comment
import com.retheviper.bbs.board.domain.model.Tag
import com.retheviper.bbs.board.infrastructure.repository.ArticleRepository
import com.retheviper.bbs.board.infrastructure.repository.ArticleTagRepository
import com.retheviper.bbs.board.infrastructure.repository.CategoryRepository
import com.retheviper.bbs.board.infrastructure.repository.CommentRepository
import com.retheviper.bbs.board.infrastructure.repository.TagRepository
import com.retheviper.bbs.common.exception.ArticleNotFoundException
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.testing.DatabaseFreeSpec
import com.retheviper.bbs.testing.TestModelFactory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class ArticleServiceTest : DatabaseFreeSpec({

    val articleRepository = mockk<ArticleRepository>()
    val categoryRepository = mockk<CategoryRepository>()
    val tagRepository = mockk<TagRepository>()
    val articleTagRepository = mockk<ArticleTagRepository>()
    val commentRepository = mockk<CommentRepository>()

    val service =
        ArticleService(articleRepository, categoryRepository, tagRepository, articleTagRepository, commentRepository)

    beforeAny { clearAllMocks() }

    "findBy" - {
        "OK" {
            val articleRecord = TestModelFactory.articleRecordModel()
            val tagRecords = TestModelFactory.tagRecordModels(articleRecord.id, 2)
            val commentRecords = TestModelFactory.commentRecordModels(articleRecord.id, articleRecord.authorId, 10)
            val paginationProperties = TestModelFactory.paginationPropertiesModel()

            every { articleRepository.findBy(any(), any(), any()) } returns listOf(articleRecord)
            every { tagRepository.findBy(any<List<ArticleId>>()) } returns tagRecords
            every { commentRepository.findBy(any<List<ArticleId>>()) } returns commentRecords

            val articles = service.findBy(
                boardId = articleRecord.boardId,
                authorId = null,
                paginationProperties = paginationProperties
            )

            articles shouldBe listOf(
                Article(
                    boardId = articleRecord.boardId,
                    id = articleRecord.id,
                    title = articleRecord.title,
                    content = articleRecord.content,
                    password = articleRecord.password,
                    authorId = articleRecord.authorId,
                    authorName = articleRecord.authorName,
                    category = Category(
                        id = articleRecord.categoryId,
                        name = articleRecord.categoryName
                    ),
                    tags = tagRecords.map { Tag.from(it) },
                    comments = commentRecords.map { Comment.from(it) },
                    viewCount = articleRecord.viewCount,
                    likeCount = articleRecord.likeCount,
                    dislikeCount = articleRecord.dislikeCount,
                    createdDate = articleRecord.createdDate,
                    updatedDate = articleRecord.updatedDate
                )
            )

            verify(exactly = 1) {
                articleRepository.findBy(
                    boardId = articleRecord.boardId,
                    authorId = null,
                    paginationProperties = paginationProperties
                )
                tagRepository.findBy(listOf(articleRecord.id))
                commentRepository.findBy(listOf(articleRecord.id))
            }
        }
    }

    "find" - {
        "OK" {
            val articleRecord = TestModelFactory.articleRecordModel()
            val tagRecords = TestModelFactory.tagRecordModels(articleRecord.id, 2)
            val commentRecords = TestModelFactory.commentRecordModels(articleRecord.id, articleRecord.authorId, 10)

            every { articleRepository.find(any(), any()) } returns articleRecord
            every { tagRepository.findBy(any<ArticleId>()) } returns tagRecords
            every { commentRepository.findBy(any<ArticleId>()) } returns commentRecords

            val article = service.find(articleRecord.id)

            article shouldBe Article(
                boardId = articleRecord.boardId,
                id = articleRecord.id,
                title = articleRecord.title,
                content = articleRecord.content,
                password = articleRecord.password,
                authorId = articleRecord.authorId,
                authorName = articleRecord.authorName,
                category = Category(
                    id = articleRecord.categoryId,
                    name = articleRecord.categoryName
                ),
                tags = tagRecords.map { Tag.from(it) },
                comments = commentRecords.map { Comment.from(it) },
                viewCount = articleRecord.viewCount,
                likeCount = articleRecord.likeCount,
                dislikeCount = articleRecord.dislikeCount,
                createdDate = articleRecord.createdDate,
                updatedDate = articleRecord.updatedDate
            )

            verify(exactly = 1) {
                articleRepository.find(articleRecord.id, false)
                tagRepository.findBy(articleRecord.id)
                commentRepository.findBy(articleRecord.id)
            }
        }

        "NG - Not found" {
            every { articleRepository.find(any(), any()) } returns null

            shouldThrow<ArticleNotFoundException> {
                service.find(ArticleId(1))
            }

            verify(exactly = 1) {
                articleRepository.find(ArticleId(1), false)
            }
        }
    }
})