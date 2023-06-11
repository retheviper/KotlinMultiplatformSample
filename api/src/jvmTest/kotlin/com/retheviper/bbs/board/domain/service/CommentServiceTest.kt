package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.infrastructure.repository.CommentRepository
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.testing.DatabaseFreeSpec
import com.retheviper.bbs.testing.TestModelFactory
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class CommentServiceTest : DatabaseFreeSpec({

    "findAll" - {

        "by article id" {
            val articleId = ArticleId(1)
            val authorId = UserId(1)
            val commentRecords = TestModelFactory.commentRecordModels(articleId, authorId)
            val repository = mockk<CommentRepository> {
                every { findBy(articleId) } returns commentRecords
            }
            val service = CommentService(repository)
            val result = service.findAll(articleId)

            result.size shouldBe commentRecords.size
            result.forEachIndexed { index, comment ->
                comment.articleId shouldBe commentRecords[index].articleId
                comment.id shouldBe commentRecords[index].id
                comment.content shouldBe commentRecords[index].content
                comment.password shouldBe commentRecords[index].password
                comment.authorId shouldBe commentRecords[index].authorId
                comment.authorName shouldBe commentRecords[index].authorName
            }
        }
    }

})