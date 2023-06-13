package com.retheviper.bbs.framework.plugin

import com.retheviper.bbs.auth.domain.service.AuthService
import com.retheviper.bbs.auth.domain.usecase.AuthUseCase
import com.retheviper.bbs.auth.infrastructure.repository.AuthRepository
import com.retheviper.bbs.board.domain.service.ArticleService
import com.retheviper.bbs.board.domain.service.CategoryService
import com.retheviper.bbs.board.domain.service.CommentService
import com.retheviper.bbs.board.infrastructure.repository.ArticleRepository
import com.retheviper.bbs.board.infrastructure.repository.ArticleTagRepository
import com.retheviper.bbs.board.infrastructure.repository.BoardRepository
import com.retheviper.bbs.board.infrastructure.repository.CategoryRepository
import com.retheviper.bbs.board.infrastructure.repository.TagRepository
import com.retheviper.bbs.common.domain.service.SensitiveWordService
import com.retheviper.bbs.common.extension.getJwtConfigs
import com.retheviper.bbs.common.infrastructure.repository.SensitiveWordRepository
import com.retheviper.bbs.message.domain.service.MessageService
import com.retheviper.bbs.message.infrastructure.repository.MessageRepository
import com.retheviper.bbs.user.domain.service.UserService
import com.retheviper.bbs.user.domain.usecase.UserUseCase
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
            koinCommonModules(),
            koinUserModules(),
            koinBoardModules(),
            koinMessageModules()
        )
    }
}

fun Application.koinAuthModules(): Module {
    return module {
        single { getJwtConfigs() }
        single { AuthUseCase(get()) }
        single { AuthService(get(), get()) }
        single { AuthRepository() }
    }
}

fun koinCommonModules(): Module {
    return module {
        single { SensitiveWordService(get()) }
        single { SensitiveWordRepository() }
    }
}

fun koinUserModules(): Module {
    return module {
        single { UserUseCase(get(), get()) }
        single { UserService(get()) }
        single { UserRepository() }
    }
}

fun koinBoardModules(): Module {
    return module {
        single { BoardRepository() }
        single { ArticleService(get(), get(), get(), get(), get()) }
        single { ArticleRepository() }
        single { CategoryService(get()) }
        single { CategoryRepository() }
        single { TagRepository() }
        single { ArticleTagRepository() }
        single { CommentService(get()) }
    }
}

fun koinMessageModules(): Module {
    return module {
        single { MessageService(get(), get()) }
        single { MessageRepository() }
    }
}