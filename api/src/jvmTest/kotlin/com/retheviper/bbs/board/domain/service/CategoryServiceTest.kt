package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.infrastructure.repository.CategoryRepository
import com.retheviper.bbs.common.value.CategoryId
import com.retheviper.bbs.testing.DatabaseFreeSpec
import com.retheviper.bbs.testing.TestModelFactory
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class CategoryServiceTest : DatabaseFreeSpec({

    "find" - {

        "findAll" {
            val record = TestModelFactory.categoryRecordModel(CategoryId(1))
            val repository = mockk<CategoryRepository> {
                every { findAll(listOf(CategoryId(1))) } returns listOf(record)
            }
            val service = CategoryService(repository)
            val result = service.findAll(listOf(record.id))
            result.size shouldBe 1
            result.first().id shouldBe record.id
            result.first().name shouldBe record.name
            result.first().description shouldBe record.description

            verify(exactly = 1) { repository.findAll(listOf(record.id)) }
        }

        "find - by id" {
            val record = TestModelFactory.categoryRecordModel(CategoryId(1))
            val repository = mockk<CategoryRepository> {
                every { find(record.id) } returns record
            }
            val service = CategoryService(repository)
            val result = service.find(record.id)
            result.id shouldBe record.id
            result.name shouldBe record.name
            result.description shouldBe record.description

            verify(exactly = 1) { repository.find(record.id) }
        }

        "find - by name" {
            val record = TestModelFactory.categoryRecordModel(CategoryId(1))
            val repository = mockk<CategoryRepository> {
                every { find(record.name) } returns record
            }
            val service = CategoryService(repository)
            val result = service.find(record.name)
            result.id shouldBe record.id
            result.name shouldBe record.name
            result.description shouldBe record.description

            verify(exactly = 1) { repository.find(record.name) }
        }
    }

    "create" {
        val id = CategoryId(1)
        val (name, description, createdBy) = Triple("name", "description", "createdBy")
        val repository = mockk<CategoryRepository> {
            every { create(name, description, createdBy) } returns id
        }
        val service = CategoryService(repository)
        val result = service.create(name, description, createdBy)
        result shouldBe id
    }
})