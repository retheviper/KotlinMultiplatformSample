package com.retheviper.bbs.common.infrastructure.table

object ArticleTags : Audit() {
    val articleId = reference("article_id", Articles)
    val tagId = reference("tag_id", Tags)
}