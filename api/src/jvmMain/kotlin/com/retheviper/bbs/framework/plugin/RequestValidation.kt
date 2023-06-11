package com.retheviper.bbs.framework.plugin

import com.retheviper.bbs.model.request.CreateArticleRequest
import com.retheviper.bbs.model.request.CreateUserRequest
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult

fun Application.configureRequestValidation() {
    install(RequestValidation) {
        validate<CreateArticleRequest> { article ->
            when {
                article.title.isBlank() -> ValidationResult.Invalid("Article title is empty.")
                article.content.isBlank() -> ValidationResult.Invalid("Article content is empty.")
                article.password.isBlank() -> ValidationResult.Invalid("Article password is empty.")
                else -> ValidationResult.Valid
            }
        }

        validate<CreateUserRequest> { user ->
            when {
                user.username.isBlank() -> ValidationResult.Invalid("Username is empty.")
                user.password.isBlank() -> ValidationResult.Invalid("Password is empty.")
                user.name.isBlank() -> ValidationResult.Invalid("Name is empty.")
                user.mail.isBlank() -> ValidationResult.Invalid("Email is empty.")
                !Regex("^[a-zA-Z0-9]{4,16}$").matches(user.username) -> ValidationResult.Invalid("Username must be 4 to 16 characters long and only contain alphanumeric characters.")
                !Regex("^[a-zA-Z0-9]{8,16}$").matches(user.password) -> ValidationResult.Invalid("Password must be 8 to 16 characters long and only contain alphanumeric characters.")
                user.name.length !in 2..16 -> ValidationResult.Invalid("Name must be 2 to 16 characters long.")
                !Regex("^[a-zA-Z0-9]+@[a-zA-Z0-9]+\\.[a-zA-Z0-9]+$").matches(user.mail) -> ValidationResult.Invalid("Email must be in the required format.")
                else -> ValidationResult.Valid
            }
        }
    }
}