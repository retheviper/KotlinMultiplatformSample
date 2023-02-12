package com.retheviper.bbs.common.infrastructure.table

import org.jetbrains.exposed.sql.Column

object Users : Audit() {
    val username: Column<String> = varchar(name = "username", length = 16)
    val password: Column<String> = varchar(name = "password", length = 255)
    val name: Column<String> = varchar(name = "name", length = 16)
    val mail: Column<String> = varchar(name = "mail", length = 255)
}