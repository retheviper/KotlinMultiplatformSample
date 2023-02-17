package com.retheviper.bbs.framework.plugin

import com.retheviper.bbs.auth.domain.service.JwtService
import com.retheviper.bbs.auth.infrastructure.repository.AuthRepository
import com.retheviper.bbs.board.domain.service.ArticleService
import com.retheviper.bbs.board.domain.service.CommentService
import com.retheviper.bbs.board.infrastructure.repository.ArticleRepository
import com.retheviper.bbs.board.infrastructure.repository.CommentRepository
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
            koinAuthModules(),
            koinUserModules(),
            koinBoardModules()
        )
    }
}

fun Application.koinAuthModules(): Module {
    return module {
        single { getJwtConfigs() }
        single { JwtService(get(), get()) }
        single { AuthRepository() }
    }
}

fun koinUserModules(): Module {
    return module {
        single { UserService(get()) }
        single { UserRepository() }
    }
}

fun koinBoardModules(): Module {
    return module {
        single { ArticleService(get(), get()) }
        single { ArticleRepository() }
        single { CommentService(get()) }
        single { CommentRepository() }
    }
}