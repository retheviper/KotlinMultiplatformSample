package com.retheviper.bbs.board.domain.model

import com.retheviper.bbs.board.infrastructure.model.CategoryRecord
import com.retheviper.bbs.common.value.BoardId
import com.retheviper.bbs.common.value.CategoryId

data class Category(
    val boardId: BoardId? = null,
    val id: CategoryId? = null,
    val name: String,
    val description: String? = null
) {
    companion object {
        fun from(record: CategoryRecord): Category {
            return Category(
                boardId = record.boardId,
                id = record.id,
                name = record.name,
                description = record.description
            )
        }
    }
}