package com.retheviper.bbs.framework.plugin

import com.retheviper.bbs.common.infrastructure.table.User
import com.retheviper.bbs.user.domain.model.UserDto
import com.retheviper.bbs.user.infrastructure.repository.UserRepository
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject

fun Application.configurePersistent() {

    Database.connect(
        url = "jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver"
    )

    val userRepository by inject<UserRepository>()

    transaction {
        SchemaUtils.create(
            User
        )

        userRepository.create(
            UserDto(
                username = "test", password = "1234", name = "test user", mail = "test_user@test.com"
            )
        )
    }
}