package com.retheviper.bbs.common.infrastructure.table

import org.jetbrains.exposed.sql.ReferenceOption

object MessageGroupMembers : Audit() {
    val messageGroupId = reference("message_group_id", MessageGroups, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
}