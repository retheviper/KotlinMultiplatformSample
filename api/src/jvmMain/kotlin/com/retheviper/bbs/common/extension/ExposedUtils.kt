package com.retheviper.bbs.common.extension

import com.retheviper.bbs.common.infrastructure.table.ArticleTags
import com.retheviper.bbs.common.infrastructure.table.Articles
import com.retheviper.bbs.common.infrastructure.table.Audit
import com.retheviper.bbs.common.infrastructure.table.Boards
import com.retheviper.bbs.common.infrastructure.table.Categories
import com.retheviper.bbs.common.infrastructure.table.Comments
import com.retheviper.bbs.common.infrastructure.table.MessageGroupMembers
import com.retheviper.bbs.common.infrastructure.table.MessageGroups
import com.retheviper.bbs.common.infrastructure.table.Messages
import com.retheviper.bbs.common.infrastructure.table.SensitiveWords
import com.retheviper.bbs.common.infrastructure.table.Tags
import com.retheviper.bbs.common.infrastructure.table.Users
import com.retheviper.bbs.model.common.PaginationProperties
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import java.time.LocalDateTime


fun <T : Any> Audit.insertAuditInfos(statement: InsertStatement<T>, name: String) {
    statement[createdBy] = name
    statement[createdDate] = LocalDateTime.now()
    statement[lastModifiedBy] = name
    statement[lastModifiedDate] = LocalDateTime.now()
    statement[deleted] = false
}

fun Audit.insertAuditInfos(statement: BatchInsertStatement, name: String) {
    statement[createdBy] = name
    statement[createdDate] = LocalDateTime.now()
    statement[lastModifiedBy] = name
    statement[lastModifiedDate] = LocalDateTime.now()
    statement[deleted] = false
}

fun Audit.updateAuditInfos(updateStatement: UpdateStatement, name: String) {
    updateStatement[lastModifiedBy] = name
    updateStatement[lastModifiedDate] = LocalDateTime.now()
}

fun Query.withPagination(paginationProperties: PaginationProperties): Query {
    return this.limit(paginationProperties.limit)
        .limit(paginationProperties.size, ((paginationProperties.page - 1) * paginationProperties.size).toLong())
}

fun getAllTables(): Array<Table> {
    return arrayOf(
        Boards,
        Articles,
        ArticleTags,
        Categories,
        Comments,
        Tags,
        Users,
        SensitiveWords,
        MessageGroups,
        MessageGroupMembers,
        Messages
    )
}