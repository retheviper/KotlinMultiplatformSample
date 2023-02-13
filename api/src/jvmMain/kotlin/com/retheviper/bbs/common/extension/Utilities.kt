package com.retheviper.bbs.common.extension

import com.amdelamar.jhash.Hash
import com.amdelamar.jhash.algorithms.Type
import com.retheviper.bbs.common.infrastructure.table.Audit
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import java.time.LocalDateTime


fun String.toHashedString(): String {
    return Hash.password(this.toCharArray()).algorithm(Type.BCRYPT).create()
}

infix fun String.verifyWith(hashed: String): Boolean {
    return Hash.password(this.toCharArray()).algorithm(Type.BCRYPT).verify(hashed)
}

fun <T : Any> Audit.insertAuditInfos(insertStatement: InsertStatement<T>, name: String) {
    insertStatement[createdBy] = name
    insertStatement[createdDate] = LocalDateTime.now()
    insertStatement[lastModifiedBy] = name
    insertStatement[lastModifiedDate] = LocalDateTime.now()
    insertStatement[deleted] = false
}

fun Audit.updateAuditInfos(it: UpdateStatement, name: String) {
    it[lastModifiedBy] = name
    it[lastModifiedDate] = LocalDateTime.now()
}