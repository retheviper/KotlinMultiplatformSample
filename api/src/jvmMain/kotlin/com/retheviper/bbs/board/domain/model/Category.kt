package com.retheviper.bbs.board.domain.model

import com.retheviper.bbs.board.infrastructure.model.CategoryRecord
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.CategoryId

data class Category(
    val articleId: ArticleId? = null,
    val id: CategoryId? = null,
    val name: String,
    val description: String? = null
) {
    companion object {
        fun from(record: CategoryRecord): Category {
            return Category(
                id = record.id,
                name = record.name,
                description = record.description
            )
        }
    }
}