package com.retheviper.bbs.common.infrastructure.table

object Tags : Audit() {
    val name = text("name").uniqueIndex()
    val description = text("description").nullable()
}