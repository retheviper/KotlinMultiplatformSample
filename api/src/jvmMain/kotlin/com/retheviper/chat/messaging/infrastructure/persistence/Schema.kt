@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.retheviper.chat.messaging.infrastructure.persistence

import org.jetbrains.exposed.v1.core.Table

object WorkspacesTable : Table("workspaces") {
    val id = uuid("id")
    val slug = varchar("slug", 100)
    val name = varchar("name", 200)
    val ownerMemberId = uuid("owner_member_id")
    val createdAtEpochMillis = long("created_at_epoch_millis")

    override val primaryKey = PrimaryKey(id)
}

object WorkspaceMembersTable : Table("workspace_members") {
    val id = uuid("id")
    val workspaceId = uuid("workspace_id")
    val userId = varchar("user_id", 120)
    val displayName = varchar("display_name", 120)
    val role = varchar("role", 20)
    val joinedAtEpochMillis = long("joined_at_epoch_millis")

    override val primaryKey = PrimaryKey(id)
}

object ChannelsTable : Table("channels") {
    val id = uuid("id")
    val workspaceId = uuid("workspace_id")
    val slug = varchar("slug", 100)
    val name = varchar("name", 120)
    val topic = text("topic").nullable()
    val visibility = varchar("visibility", 20)
    val createdByMemberId = uuid("created_by_member_id")
    val createdAtEpochMillis = long("created_at_epoch_millis")

    override val primaryKey = PrimaryKey(id)
}

object MessagesTable : Table("messages") {
    val id = uuid("id")
    val channelId = uuid("channel_id")
    val authorMemberId = uuid("author_member_id")
    val body = text("body")
    val previewUrl = text("preview_url").nullable()
    val previewTitle = text("preview_title").nullable()
    val previewDescription = text("preview_description").nullable()
    val previewImageUrl = text("preview_image_url").nullable()
    val previewSiteName = varchar("preview_site_name", 255).nullable()
    val threadRootMessageId = uuid("thread_root_message_id").nullable()
    val createdAtEpochMillis = long("created_at_epoch_millis")

    override val primaryKey = PrimaryKey(id)
}

object MessageReactionsTable : Table("message_reactions") {
    val id = uuid("id")
    val messageId = uuid("message_id")
    val memberId = uuid("member_id")
    val emoji = varchar("emoji", 32)
    val createdAtEpochMillis = long("created_at_epoch_millis")

    override val primaryKey = PrimaryKey(id)
}

object MentionNotificationsTable : Table("mention_notifications") {
    val id = uuid("id")
    val kind = varchar("kind", 32)
    val memberId = uuid("member_id")
    val channelId = uuid("channel_id")
    val messageId = uuid("message_id")
    val threadRootMessageId = uuid("thread_root_message_id").nullable()
    val authorDisplayName = varchar("author_display_name", 120)
    val messagePreview = text("message_preview")
    val createdAtEpochMillis = long("created_at_epoch_millis")
    val readAtEpochMillis = long("read_at_epoch_millis").nullable()

    override val primaryKey = PrimaryKey(id)
}
