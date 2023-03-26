package com.retheviper.bbs.common.infrastructure.table

import org.jetbrains.exposed.sql.ReferenceOption

object MessageGroups : Audit() {
    val ownerId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
}