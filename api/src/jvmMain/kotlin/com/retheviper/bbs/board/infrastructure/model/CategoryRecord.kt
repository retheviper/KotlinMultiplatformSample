package com.retheviper.bbs.board.infrastructure.model

import com.retheviper.bbs.common.value.CategoryId

data class CategoryRecord(
    val id: CategoryId,
    val name: String,
    val description: String?
)