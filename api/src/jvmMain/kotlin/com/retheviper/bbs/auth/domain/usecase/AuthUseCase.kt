package com.retheviper.bbs.auth.domain.usecase

import com.retheviper.bbs.auth.domain.model.Credential
import com.retheviper.bbs.auth.domain.service.AuthService
import com.retheviper.bbs.common.exception.InvalidTokenException
import com.retheviper.bbs.common.exception.PasswordNotMatchException
import com.retheviper.bbs.common.exception.UserNotFoundException
import com.retheviper.bbs.common.extension.notMatchesWith
import com.retheviper.bbs.constant.ErrorCode

class AuthUseCase(private val service: AuthService) {

    fun createToken(credential: Credential): String {
        val exist = service.findCredential(credential.username)
            ?: throw UserNotFoundException("User not found.")

        if (credential.password notMatchesWith exist.password) {
            throw PasswordNotMatchException("Invalid password.", ErrorCode.USER_PASSWORD_NOT_MATCH)
        }

        exist.userId ?: throw UserNotFoundException("User not found.")

        return service.createToken(exist.userId, exist.username)
    }

    fun refreshToken(token: String) {
        if (!service.isValidToken(token)) {
            throw InvalidTokenException("Invalid token.")
        }

        service.refreshToken(token)
    }
}