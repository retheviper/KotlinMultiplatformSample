package com.retheviper.bbs.framework.plugin

import com.retheviper.bbs.auth.domain.service.JwtService
import com.retheviper.bbs.auth.infrastructure.repository.AuthRepository
import com.retheviper.bbs.board.domain.service.BoardService
import com.retheviper.bbs.board.infrastructure.BoardRepository
import com.retheviper.bbs.board.infrastructure.CommentRepository
import com.retheviper.bbs.common.extension.getJwtConfigs
import com.retheviper.bbs.user.domain.service.UserService
import com.retheviper.bbs.user.infrastructure.repository.UserRepository
import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureDependencyInjection() {
    install(Koin) {
        slf4jLogger()
        modules(
            authModules(),
            userModules(),
            boardModules()
        )
    }
}

private fun Application.authModules(): Module {
    val config = module { single { getJwtConfigs() } }
    val service = module { single { JwtService(get(), get()) } }
    val repository = module { single { AuthRepository() } }
    return module { includes(config, service, repository) }
}

private fun userModules(): Module {
    val service = module { single { UserService(get()) } }
    val repository = module { single { UserRepository() } }
    return module { includes(service, repository) }
}

private fun boardModules(): Module {
    val service = module { single { BoardService(get()) } }
    val boardRepository = module { single { BoardRepository() } }
    val commentRepository = module { single { CommentRepository() } }
    return module { includes(service, boardRepository, commentRepository) }
}