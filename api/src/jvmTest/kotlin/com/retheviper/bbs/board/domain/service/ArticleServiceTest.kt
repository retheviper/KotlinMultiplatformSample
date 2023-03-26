package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.domain.model.Category
import com.retheviper.bbs.board.infrastructure.repository.ArticleRepository
import com.retheviper.bbs.common.domain.service.SensitiveWordService
import com.retheviper.bbs.testing.DatabaseFreeSpec
import com.retheviper.bbs.testing.TestModelFactory
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class ArticleServiceTest : DatabaseFreeSpec({

    "findAll" {
        val articleRecord = TestModelFactory.articleRecordModel()
        val tags = TestModelFactory.tagModels(articleRecord.id, 2)
        val comments = TestModelFactory.commentModels(articleRecord.id, articleRecord.authorId, 10)
        val repository = mockk<ArticleRepository> {
            every {
                findAll(
                    boardId = articleRecord.boardId,
                    authorId = null,
                    paginationProperties = TestModelFactory.paginationPropertiesModel()
                )
            } returns listOf(articleRecord)
        }
        val sensitiveWordService = mockk<SensitiveWordService> {
            every { find(any()) } returns emptySet()
        }
        val commentService = mockk<CommentService> {
            every { findAll(listOf(articleRecord.id)) } returns comments
        }
        val tagService = mockk<TagService> {
            every { findAll(listOf(articleRecord.id)) } returns tags
        }
        val service = ArticleService(sensitiveWordService, mockk(), tagService, commentService, repository)

        val articles = service.findAll(
            boardId = articleRecord.boardId,
            authorId = null,
            paginationProperties = TestModelFactory.paginationPropertiesModel()
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
                tags = tags,
                comments = comments,
                viewCount = articleRecord.viewCount,
                likeCount = articleRecord.likeCount,
                dislikeCount = articleRecord.dislikeCount,
                createdDate = articleRecord.createdDate,
                updatedDate = articleRecord.updatedDate
            )
        )

        verify(exactly = 1) {
            repository.findAll(
                boardId = articleRecord.boardId,
                authorId = null,
                paginationProperties = TestModelFactory.paginationPropertiesModel()
            )
            commentService.findAll(listOf(articleRecord.id))
            tagService.findAll(listOf(articleRecord.id))
        }
    }

    "find" {
        val articleRecord = TestModelFactory.articleRecordModel()
        val tags = TestModelFactory.tagModels(articleRecord.id, 2)
        val comments = TestModelFactory.commentModels(articleRecord.id, articleRecord.authorId, 10)
        val repository = mockk<ArticleRepository> {
            every { find(articleRecord.id, true) } returns articleRecord
            every { update(any()) } returns Unit
        }
        val sensitiveWordService = mockk<SensitiveWordService> {
            every { find(any()) } returns emptySet()
        }
        val commentService = mockk<CommentService> {
            every { findAll(articleRecord.id) } returns comments
        }
        val tagService = mockk<TagService> {
            every { findAll(listOf(articleRecord.id)) } returns tags
        }
        val service = ArticleService(sensitiveWordService, mockk(), tagService, commentService, repository)

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
            tags = tags,
            comments = comments,
            viewCount = articleRecord.viewCount + 1,
            likeCount = articleRecord.likeCount,
            dislikeCount = articleRecord.dislikeCount,
            createdDate = articleRecord.createdDate,
            updatedDate = articleRecord.updatedDate
        )

        verify(exactly = 1) {
            repository.find(articleRecord.id, true)
            repository.update(any())
            commentService.findAll(articleRecord.id)
            tagService.findAll(listOf(articleRecord.id))
        }
    }
})