package com.retheviper.bbs.board.infrastructure.repository

import com.retheviper.bbs.board.domain.model.Category
import com.retheviper.bbs.board.infrastructure.model.CategoryRecord
import com.retheviper.bbs.common.extension.insertAuditInfos
import com.retheviper.bbs.common.infrastructure.table.Categories
import com.retheviper.bbs.common.value.BoardId
import com.retheviper.bbs.common.value.CategoryId
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select

class CategoryRepository {

    fun findAll(ids: List<CategoryId>): List<CategoryRecord> {
        return Categories.select { (Categories.id inList ids.map { it.value }) and (Categories.deleted eq false) }
            .map { it.toRecord() }
    }

    fun find(id: CategoryId): CategoryRecord {
        return Categories.select { (Categories.id eq id.value) and (Categories.deleted eq false) }
            .map { it.toRecord() }
            .first()
    }

    fun find(name: String): CategoryRecord {
        return Categories.select { (Categories.name eq name) and (Categories.deleted eq false) }
            .map { it.toRecord() }
            .first()
    }

    fun create(category: Category): CategoryId {
        val id = Categories.insertAndGetId {
            it[name] = category.name
            it[description] = category.description
            it[boardId] = requireNotNull(category.boardId).value
            insertAuditInfos(it, "system")
        }.value
        return CategoryId(id)
    }

    private fun ResultRow.toRecord() = CategoryRecord(
        boardId = BoardId(this[Categories.id].value),
        id = CategoryId(this[Categories.id].value),
        name = this[Categories.name],
        description = this[Categories.description]
    )
}