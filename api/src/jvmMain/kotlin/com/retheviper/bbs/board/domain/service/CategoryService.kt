package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.domain.model.Category
import com.retheviper.bbs.board.infrastructure.repository.CategoryRepository
import com.retheviper.bbs.common.value.CategoryId
import org.jetbrains.exposed.sql.transactions.transaction

class CategoryService(private val categoryRepository: CategoryRepository) {

    fun findAll(ids: List<CategoryId>): List<Category> {
        return transaction {
            categoryRepository.findAll(ids)
                .map { Category.from(it) }
        }
    }

    fun find(id: CategoryId): Category {
        return transaction {
            Category.from(categoryRepository.find(id))
        }
    }

    fun find(name: String): Category {
        return transaction {
            Category.from(categoryRepository.find(name))
        }
    }

    fun create(name: String, description: String, createdBy: String): CategoryId {
        return transaction {
            categoryRepository.create(name, description, createdBy)
        }
    }
}