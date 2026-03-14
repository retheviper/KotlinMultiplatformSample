package com.retheviper.chat.plugins

import com.retheviper.chat.contract.AddWorkspaceMemberRequest
import com.retheviper.chat.contract.CreateChannelRequest
import com.retheviper.chat.contract.CreateWorkspaceRequest
import com.retheviper.chat.contract.PostMessageRequest
import com.retheviper.chat.contract.UpdateWorkspaceMemberRequest
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult

fun Application.configureValidation() {
    install(RequestValidation) {
        validate<CreateWorkspaceRequest> { request ->
            if (request.slug.isBlank() || request.name.isBlank() || request.ownerUserId.isBlank() || request.ownerDisplayName.isBlank()) {
                ValidationResult.Invalid("workspace payload has blank fields")
            } else {
                ValidationResult.Valid
            }
        }
        validate<AddWorkspaceMemberRequest> { request ->
            if (request.userId.isBlank() || request.displayName.isBlank()) {
                ValidationResult.Invalid("member payload has blank fields")
            } else {
                ValidationResult.Valid
            }
        }
        validate<UpdateWorkspaceMemberRequest> { request ->
            if (request.displayName.isBlank()) {
                ValidationResult.Invalid("member update payload has blank fields")
            } else {
                ValidationResult.Valid
            }
        }
        validate<CreateChannelRequest> { request ->
            if (request.slug.isBlank() || request.name.isBlank() || request.createdByMemberId.isBlank()) {
                ValidationResult.Invalid("channel payload has blank fields")
            } else {
                ValidationResult.Valid
            }
        }
        validate<PostMessageRequest> { request ->
            if (request.authorMemberId.isBlank() || request.body.isBlank()) {
                ValidationResult.Invalid("message payload has blank fields")
            } else {
                ValidationResult.Valid
            }
        }
    }
}
