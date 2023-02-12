package com.retheviper.bbs.common.infrastructure.table

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

abstract class Audit : IntIdTable() {
    val deleted: Column<Boolean> = bool("deleted")
    val createdBy: Column<String> = varchar("created_by", 16)
    val createdDate: Column<LocalDateTime> = datetime("created_date")
    val lastModifiedBy: Column<String> = varchar("last_modified_by", 16)
    val lastModifiedDate: Column<LocalDateTime> = datetime("last_modified_date")
}