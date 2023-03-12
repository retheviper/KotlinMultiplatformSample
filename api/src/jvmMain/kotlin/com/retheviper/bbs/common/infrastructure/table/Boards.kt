package com.retheviper.bbs.common.infrastructure.table

object Boards : Audit() {
    val name = varchar("name", 50).uniqueIndex()
    val description = text("description").nullable()
}