package com.retheviper.chat.wiring

import com.retheviper.chat.messaging.application.MessagingCommandService
import com.retheviper.chat.messaging.application.NotificationEventBus
import com.retheviper.chat.messaging.application.MessagingQueryService
import com.retheviper.chat.messaging.application.LinkPreviewResolver
import com.retheviper.chat.messaging.infrastructure.persistence.ChannelPersistenceRepository
import com.retheviper.chat.messaging.infrastructure.persistence.MessagePersistenceRepository
import com.retheviper.chat.messaging.infrastructure.persistence.MessageReactionPersistenceRepository
import com.retheviper.chat.messaging.infrastructure.persistence.MentionNotificationPersistenceRepository
import com.retheviper.chat.messaging.infrastructure.persistence.WorkspacePersistenceRepository

data class ApplicationDependencies(
    val commandService: MessagingCommandService,
    val queryService: MessagingQueryService,
    val linkPreviewResolver: LinkPreviewResolver,
    val notificationEventBus: NotificationEventBus
) {
    companion object {
        fun create(): ApplicationDependencies {
            val workspaceRepository = WorkspacePersistenceRepository()
            val channelRepository = ChannelPersistenceRepository()
            val messageRepository = MessagePersistenceRepository()
            val messageReactionRepository = MessageReactionPersistenceRepository()
            val mentionNotificationRepository = MentionNotificationPersistenceRepository()
            val linkPreviewResolver = LinkPreviewResolver()
            val notificationEventBus = NotificationEventBus()

            return ApplicationDependencies(
                commandService = MessagingCommandService(
                    workspaceRepository = workspaceRepository,
                    channelRepository = channelRepository,
                    messageRepository = messageRepository,
                    messageReactionRepository = messageReactionRepository,
                    mentionNotificationRepository = mentionNotificationRepository,
                    notificationEventBus = notificationEventBus,
                    transactionsEnabled = true
                ),
                queryService = MessagingQueryService(
                    workspaceRepository = workspaceRepository,
                    channelRepository = channelRepository,
                    messageRepository = messageRepository,
                    messageReactionRepository = messageReactionRepository,
                    mentionNotificationRepository = mentionNotificationRepository,
                    transactionsEnabled = true
                ),
                linkPreviewResolver = linkPreviewResolver,
                notificationEventBus = notificationEventBus
            )
        }
    }
}
