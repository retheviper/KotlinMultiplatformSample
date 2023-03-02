package com.retheviper.bbs.common.extension

import com.amdelamar.jhash.Hash
import com.amdelamar.jhash.algorithms.Type
import com.retheviper.bbs.common.infrastructure.table.ArticleTags
import com.retheviper.bbs.common.infrastructure.table.Articles
import com.retheviper.bbs.common.infrastructure.table.Audit
import com.retheviper.bbs.common.infrastructure.table.Categories
import com.retheviper.bbs.common.infrastructure.table.Comments
import com.retheviper.bbs.common.infrastructure.table.SensitiveWords
import com.retheviper.bbs.common.infrastructure.table.Tags
import com.retheviper.bbs.common.infrastructure.table.Users
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import java.time.LocalDateTime


fun String.toHashedString(): String {
    return Hash.password(this.toCharArray()).algorithm(Type.BCRYPT).create()
}

infix fun String.matchesWith(hashed: String): Boolean {
    return Hash.password(this.toCharArray()).algorithm(Type.BCRYPT).verify(hashed)
}

infix fun String.notMatchesWith(hashed: String): Boolean {
    return !matchesWith(hashed)
}

fun <T : Any> Audit.insertAuditInfos(insertStatement: InsertStatement<T>, name: String) {
    insertStatement[createdBy] = name
    insertStatement[createdDate] = LocalDateTime.now()
    insertStatement[lastModifiedBy] = name
    insertStatement[lastModifiedDate] = LocalDateTime.now()
    insertStatement[deleted] = false
}

fun Audit.updateAuditInfos(updateStatement: UpdateStatement, name: String) {
    updateStatement[lastModifiedBy] = name
    updateStatement[lastModifiedDate] = LocalDateTime.now()
}

fun getAllTables(): Array<Table> {
    return arrayOf(
        Articles,
        ArticleTags,
        Categories,
        Comments,
        Tags,
        Users,
        SensitiveWords
    )
}