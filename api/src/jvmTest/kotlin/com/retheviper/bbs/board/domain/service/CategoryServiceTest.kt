package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.infrastructure.repository.CategoryRepository
import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.exception.CategoryNotFountException
import com.retheviper.bbs.common.value.BoardId
import com.retheviper.bbs.common.value.CategoryId
import com.retheviper.bbs.testing.DatabaseFreeSpec
import com.retheviper.bbs.testing.TestModelFactory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class CategoryServiceTest : DatabaseFreeSpec({

    val repository = mockk<CategoryRepository>()
    val service = CategoryService(repository)

    beforeAny { clearAllMocks() }

    "findBy" - {
        "OK" {
            val record = TestModelFactory.categoryRecordModel(CategoryId(1))

            every { repository.findBy(any()) } returns listOf(record)

            val result = service.findBy(record.boardId!!)

            result.size shouldBe 1
            result.first().id shouldBe record.id
            result.first().name shouldBe record.name
            result.first().description shouldBe record.description

            verify(exactly = 1) { repository.findBy(record.boardId!!) }
        }

        "NG - No category found" {
            every { repository.findBy(any()) } returns emptyList()

            shouldThrow<CategoryNotFountException> { service.findBy(BoardId(1)) }

            verify(exactly = 1) { repository.findBy(BoardId(1)) }
        }
    }

    "create" - {
        "OK" {
            val id = CategoryId(1)
            val category = TestModelFactory.categoryModel()

            every { repository.find(any<String>()) } returns null
            every { repository.create(any()) } returns id

            val result = service.create(category)
            result shouldBe category.copy(id = id)
        }
    }

    "update" - {
        "OK" {
            val category = TestModelFactory.categoryModel()
                .copy(id = CategoryId(1))

            justRun { repository.update(any()) }

            val result = service.update(category)

            result shouldBe category
        }

        "NG - No category id" {
            val category = TestModelFactory.categoryModel()

            shouldThrow<BadRequestException> { service.update(category) }
        }
    }

    "delete" - {
        "OK" {
            val id = CategoryId(1)

            every { repository.delete(any()) } returns Unit

            service.delete(id)
        }
    }
})