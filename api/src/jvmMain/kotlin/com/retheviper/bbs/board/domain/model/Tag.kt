package com.retheviper.bbs.board.domain.model

import com.retheviper.bbs.board.infrastructure.model.TagRecord
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.TagId

data class Tag(
    val articleId: ArticleId? = null,
    val id: TagId? = null,
    val name: String,
    val description: String? = null,
    val createdBy: String? = null,
) {
    companion object {
        fun from(tagRecord: TagRecord): Tag {
            return Tag(
                id = tagRecord.id,
                name = tagRecord.name,
                description = tagRecord.description,
                createdBy = tagRecord.createdBy
            )
        }
    }
}