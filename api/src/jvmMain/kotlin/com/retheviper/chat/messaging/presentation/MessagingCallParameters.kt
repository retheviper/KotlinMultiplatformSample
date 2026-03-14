@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.retheviper.chat.messaging.presentation

import io.ktor.server.application.ApplicationCall
import kotlin.uuid.Uuid

internal fun ApplicationCall.workspaceId(): Uuid =
    Uuid.parse(parameters["workspaceId"] ?: error("workspaceId is required"))

internal fun ApplicationCall.channelId(): Uuid =
    Uuid.parse(parameters["channelId"] ?: error("channelId is required"))

internal fun ApplicationCall.messageId(): Uuid =
    Uuid.parse(parameters["messageId"] ?: error("messageId is required"))

internal fun ApplicationCall.memberId(): Uuid =
    Uuid.parse(parameters["memberId"] ?: error("memberId is required"))
