package com.retheviper.bbs.common.infrastructure.table

object Categories: Audit() {
    val name = text("name").uniqueIndex()
    val description = text("description").nullable()
}