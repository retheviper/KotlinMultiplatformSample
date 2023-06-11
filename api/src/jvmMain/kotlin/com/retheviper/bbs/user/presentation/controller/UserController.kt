package com.retheviper.bbs.user.presentation.controller

import com.retheviper.bbs.common.extension.from
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.model.request.CreateUserRequest
import com.retheviper.bbs.model.response.GetUserResponse
import com.retheviper.bbs.user.domain.model.User
import com.retheviper.bbs.user.domain.usecase.UserUseCase

class UserController(private val usecase: UserUseCase) {

    fun find(id: UserId): GetUserResponse {
        val dto = usecase.find(id)
        return GetUserResponse.from(dto)
    }

    fun create(request: CreateUserRequest): GetUserResponse {
        val dto = usecase.create(User.from(request))
        return GetUserResponse.from(dto)
    }
}