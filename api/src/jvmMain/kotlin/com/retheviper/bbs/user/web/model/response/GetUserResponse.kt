package com.retheviper.bbs.user.web.model.response

import com.retheviper.bbs.user.domain.model.UserDto
import kotlinx.serialization.Serializable

@Serializable
data class GetUserResponse(
    val id: Int,
    val username: String,
    val name: String,
    val mail: String
) {
    companion object {
        fun from(dto: UserDto) = GetUserResponse(
            id = checkNotNull(dto.id),
            username = dto.username,
            name = dto.name,
            mail = dto.mail
        )
    }
}