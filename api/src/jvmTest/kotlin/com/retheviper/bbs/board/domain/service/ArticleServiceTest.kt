package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.infrastructure.repository.ArticleRepository
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.CategoryId
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
            id = ArticleId(1),
            categoryId = CategoryId(1),
            authorId = UserId(1)
        )
        val category = TestModelFactory.categoryModel()
            .copy(
                id = articleRecord.categoryId,
                articleId = articleRecord.id
            )
        val tags = TestModelFactory.tagModels(articleRecord.id, 2)
        val comments = TestModelFactory.commentModels(articleRecord.id, articleRecord.authorId, 10)
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
            every { findAll(listOf(articleRecord.id)) } returns comments
        }
        val categoryService = mockk<CategoryService> {
            every { findAll(listOf(articleRecord.categoryId)) } returns listOf(category)
        }
        val tagService = mockk<TagService> {
            every { findAll(listOf(articleRecord.id)) } returns tags
        }
        val service = ArticleService(categoryService, tagService, commentService, repository)

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
                category = category,
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
                authorId = null,
                page = page,
                pageSize = pageSize,
                limit = limit
            )
            commentService.findAll(listOf(articleRecord.id))
            categoryService.findAll(listOf(articleRecord.categoryId))
            tagService.findAll(listOf(articleRecord.id))
        }
    }

    "find" {
        val articleRecord = TestModelFactory.articleRecordModel(
            id = ArticleId(1),
            categoryId = CategoryId(1),
            authorId = UserId(1)
        )
        val category = TestModelFactory.categoryModel()
            .copy(
                id = articleRecord.categoryId,
                articleId = articleRecord.id
            )
        val tags = TestModelFactory.tagModels(articleRecord.id, 2)
        val comments = TestModelFactory.commentModels(articleRecord.id, articleRecord.authorId, 10)
        val repository = mockk<ArticleRepository> {
            every { find(articleRecord.id) } returns articleRecord
            every { update(any()) } returns Unit
        }
        val commentService = mockk<CommentService> {
            every { findAll(articleRecord.id) } returns comments
        }
        val categoryService = mockk<CategoryService> {
            every { find(articleRecord.categoryId) } returns category
        }
        val tagService = mockk<TagService> {
            every { findAll(listOf(articleRecord.id)) } returns tags
        }
        val service = ArticleService(categoryService, tagService, commentService, repository)

        val article = service.find(articleRecord.id)

        article shouldBe Article(
            id = articleRecord.id,
            title = articleRecord.title,
            content = articleRecord.content,
            password = articleRecord.password,
            authorId = articleRecord.authorId,
            authorName = articleRecord.authorName,
            category = category,
            tags = tags,
            comments = comments,
            viewCount = articleRecord.viewCount,
            likeCount = articleRecord.likeCount,
            dislikeCount = articleRecord.dislikeCount,
            createdDate = articleRecord.createdDate,
            updatedDate = articleRecord.updatedDate
        )

        verify(exactly = 1) {
            repository.find(articleRecord.id)
            repository.update(any())
            commentService.findAll(articleRecord.id)
            categoryService.find(articleRecord.categoryId)
            tagService.findAll(listOf(articleRecord.id))
        }
    }
})