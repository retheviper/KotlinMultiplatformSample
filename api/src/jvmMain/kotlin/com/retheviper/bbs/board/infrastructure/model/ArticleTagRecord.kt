package com.retheviper.bbs.board.infrastructure.model

import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.TagId

data class ArticleTagRecord(
    val articleId: ArticleId,
    val tagId: TagId
)