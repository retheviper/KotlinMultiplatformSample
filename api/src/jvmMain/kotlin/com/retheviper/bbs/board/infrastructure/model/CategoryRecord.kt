package com.retheviper.bbs.board.infrastructure.model

import com.retheviper.bbs.common.value.BoardId
import com.retheviper.bbs.common.value.CategoryId

data class CategoryRecord(
    val boardId: BoardId?,
    val id: CategoryId,
    val name: String,
    val description: String?
)