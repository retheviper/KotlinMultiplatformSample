package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.domain.model.Category
import com.retheviper.bbs.board.infrastructure.repository.CategoryRepository
import com.retheviper.bbs.common.exception.BadRequestException
import com.retheviper.bbs.common.exception.CategoryAlreadyExistsException
import com.retheviper.bbs.common.exception.CategoryNotFountException
import com.retheviper.bbs.common.value.BoardId
import com.retheviper.bbs.common.value.CategoryId

class CategoryService(private val categoryRepository: CategoryRepository) {

    fun findBy(): List<Category> {
        val records = categoryRepository.findBy()

        if (records.isEmpty()) {
            throw CategoryNotFountException("Category not found.")
        }

        return records.map { Category.from(it) }
    }

    fun findBy(id: BoardId): List<Category> {
        val records = categoryRepository.findBy(id)

        if (records.isEmpty()) {
            throw CategoryNotFountException("Category for board id: $id not found.")
        }

        return records.map { Category.from(it) }
    }

    fun create(category: Category): Category {
        categoryRepository.find(category.name)?.let {
            throw CategoryAlreadyExistsException("Category name: ${category.name} already exists.")
        }

        val id = categoryRepository.create(category)
        return category.copy(id = id)
    }

    fun update(category: Category): Category {
        category.id ?: throw BadRequestException("Category id must not be null.")
        categoryRepository.update(category)
        return category
    }

    fun delete(id: CategoryId) {
        categoryRepository.delete(id)
    }
}