package com.retheviper.bbs.common.infrastructure.table

import org.jetbrains.exposed.sql.ReferenceOption

object Categories: Audit() {
    val name = varchar("name", 50).uniqueIndex()
    val description = text("description").nullable()
    val boardId = reference("board_id", Boards, onDelete = ReferenceOption.CASCADE)
}