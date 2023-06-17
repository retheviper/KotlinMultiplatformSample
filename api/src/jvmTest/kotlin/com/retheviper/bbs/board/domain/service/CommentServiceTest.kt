package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.domain.model.Comment
import com.retheviper.bbs.board.infrastructure.repository.CommentRepository
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.testing.DatabaseFreeSpec
import com.retheviper.bbs.testing.TestModelFactory
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class CommentServiceTest : DatabaseFreeSpec({

    val repository = mockk<CommentRepository>()
    val service = CommentService(repository)

    "find" - {
        "OK" {
            val commentRecord = TestModelFactory.commentRecordModel()

            every { repository.find(any()) } returns commentRecord

            val result = service.find(commentRecord.id)

            result shouldBe Comment.from(commentRecord)
        }
    }
})