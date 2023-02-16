package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.infrastructure.model.ArticleRecord
import com.retheviper.bbs.board.infrastructure.repository.ArticleRepository
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.testing.FreeSpecWithDb
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class ArticleServiceTest : FreeSpecWithDb({

    val repository = mockk<ArticleRepository>()
    val commentService = mockk<CommentService>()
    val service = ArticleService(commentService, repository)

    "findAll" {
        val (page, pageSize, limit) = Triple(1, 1, 10)
        val articleId = ArticleId(1)
        every { repository.findAll(authorId = null, page = page, pageSize = pageSize, limit = limit) } returns listOf(
            ArticleRecord(
                id = articleId,
                title = "title",
                content = "content",
                password = "password",
                authorId = UserId(1),
                authorName = "authorName"
            )
        )
        every { commentService.findAll(listOf(articleId)) } returns emptyList()

        val articles = service.findAll(authorId = null, page = page, pageSize = pageSize, limit = limit)

        articles shouldBe listOf(
            Article(
                id = articleId,
                title = "title",
                content = "content",
                password = "password",
                authorId = UserId(1),
                authorName = "authorName",
                comments = null
            )
        )

        verify {
            repository.findAll(authorId = null, page = page, pageSize = pageSize, limit = limit)
            commentService.findAll(listOf(articleId))
        }
    }

    "find" {
        val articleId = ArticleId(1)

        every { repository.find(articleId) } returns ArticleRecord(
            id = articleId,
            title = "title",
            content = "content",
            password = "password",
            authorId = UserId(1),
            authorName = "authorName"
        )
        every { commentService.findAll(articleId) } returns emptyList()

        val article = service.find(articleId)

        article shouldBe Article(
            id = articleId,
            title = "title",
            content = "content",
            password = "password",
            authorId = UserId(1),
            authorName = "authorName",
            comments = emptyList()
        )
    }
})