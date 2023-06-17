package com.retheviper.bbs.board.infrastructure.model

import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.TagId

data class TagRecord(
    val articleId: ArticleId?,
    val id: TagId,
    val name: String,
    val description: String?,
    val createdBy: String
)