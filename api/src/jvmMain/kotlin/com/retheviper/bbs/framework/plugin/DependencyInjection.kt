package com.retheviper.bbs.framework.plugin

import com.retheviper.bbs.auth.domain.service.JwtService
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
            authModules(), userModules()
        )
    }
}

private fun authModules(): Module {
    val service = module { single { JwtService() } }
    return module { includes(service) }
}

private fun userModules(): Module {
    val service = module { single { UserService(get()) } }
    val repository = module { single { UserRepository() } }
    return module { includes(service, repository) }
}