package com.retheviper.bbs.common.infrastructure.table

object Tags : Audit() {
    val name = varchar("name", 50).uniqueIndex()
    val description = text("description").nullable()
}