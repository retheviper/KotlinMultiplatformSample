package com.retheviper.chat.wiring

import com.retheviper.chat.messaging.application.MessagingCommandService
import com.retheviper.chat.messaging.application.NotificationEventBus
import com.retheviper.chat.messaging.application.MessagingQueryService
import com.retheviper.chat.messaging.application.LinkPreviewResolver
import com.retheviper.chat.messaging.infrastructure.persistence.DatabaseChannelRepository
import com.retheviper.chat.messaging.infrastructure.persistence.DatabaseMessageRepository
import com.retheviper.chat.messaging.infrastructure.persistence.DatabaseMessageReactionRepository
import com.retheviper.chat.messaging.infrastructure.persistence.DatabaseMentionNotificationRepository
import com.retheviper.chat.messaging.infrastructure.persistence.DatabaseWorkspaceRepository

data class ApplicationDependencies(
    val commandService: MessagingCommandService,
    val queryService: MessagingQueryService,
    val linkPreviewResolver: LinkPreviewResolver,
    val notificationEventBus: NotificationEventBus
) {
    companion object {
        fun create(): ApplicationDependencies {
            val workspaceRepository = DatabaseWorkspaceRepository()
            val channelRepository = DatabaseChannelRepository()
            val messageRepository = DatabaseMessageRepository()
            val messageReactionRepository = DatabaseMessageReactionRepository()
            val mentionNotificationRepository = DatabaseMentionNotificationRepository()
            val linkPreviewResolver = LinkPreviewResolver()
            val notificationEventBus = NotificationEventBus()

            return ApplicationDependencies(
                commandService = MessagingCommandService(
                    workspaceRepository = workspaceRepository,
                    channelRepository = channelRepository,
                    messageRepository = messageRepository,
                    messageReactionRepository = messageReactionRepository,
                    mentionNotificationRepository = mentionNotificationRepository,
                    notificationEventBus = notificationEventBus
                ),
                queryService = MessagingQueryService(
                    workspaceRepository = workspaceRepository,
                    channelRepository = channelRepository,
                    messageRepository = messageRepository,
                    messageReactionRepository = messageReactionRepository,
                    mentionNotificationRepository = mentionNotificationRepository
                ),
                linkPreviewResolver = linkPreviewResolver,
                notificationEventBus = notificationEventBus
            )
        }
    }
}
