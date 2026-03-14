package com.retheviper.chat.messaging.application

import com.retheviper.chat.contract.ChannelVisibility
import com.retheviper.chat.contract.CreateChannelRequest
import com.retheviper.chat.contract.CreateWorkspaceRequest
import com.retheviper.chat.contract.LinkPreviewResponse
import com.retheviper.chat.contract.PostMessageRequest
import com.retheviper.chat.messaging.domain.Channel
import com.retheviper.chat.messaging.domain.ChannelRepository
import com.retheviper.chat.messaging.domain.DomainValidationException
import com.retheviper.chat.messaging.domain.Message
import com.retheviper.chat.messaging.domain.MessageReaction
import com.retheviper.chat.messaging.domain.MessageReactionRepository
import com.retheviper.chat.messaging.domain.MessageRepository
import com.retheviper.chat.messaging.domain.MentionNotification
import com.retheviper.chat.messaging.domain.MentionNotificationRepository
import com.retheviper.chat.messaging.domain.NotificationKind
import com.retheviper.chat.messaging.domain.Workspace
import com.retheviper.chat.messaging.domain.WorkspaceMember
import com.retheviper.chat.messaging.domain.WorkspaceRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class MessagingServicesTest : FunSpec({
    test("createWorkspace rejects duplicate slug") {
        val fixture = messagingFixture()

        runBlocking {
            fixture.commandService.createWorkspace(
                CreateWorkspaceRequest(
                    slug = "acme",
                    name = "Acme",
                    ownerUserId = "alice",
                    ownerDisplayName = "Alice"
                )
            )
        }

        val error = shouldThrow<DomainValidationException> {
            runBlocking {
                fixture.commandService.createWorkspace(
                    CreateWorkspaceRequest(
                        slug = "acme",
                        name = "Another Acme",
                        ownerUserId = "bob",
                        ownerDisplayName = "Bob"
                    )
                )
            }
        }

        error.message shouldBe "workspace slug already exists"
    }

    test("createWorkspace bootstraps a general channel") {
        val fixture = messagingFixture()

        val workspace = runBlocking {
            fixture.commandService.createWorkspace(
                CreateWorkspaceRequest(
                    slug = "acme",
                    name = "Acme",
                    ownerUserId = "alice",
                    ownerDisplayName = "Alice"
                )
            )
        }

        val channels = runBlocking { fixture.queryService.listChannels(workspace.id) }

        channels.map { it.slug } shouldContainExactly listOf("general")
        channels.single().createdByMemberId shouldBe workspace.ownerMemberId
    }

    test("createChannel rejects creator from another workspace") {
        val fixture = messagingFixture()

        val firstWorkspace = runBlocking {
            fixture.commandService.createWorkspace(
                CreateWorkspaceRequest(
                    slug = "acme",
                    name = "Acme",
                    ownerUserId = "alice",
                    ownerDisplayName = "Alice"
                )
            )
        }
        val secondWorkspace = runBlocking {
            fixture.commandService.createWorkspace(
                CreateWorkspaceRequest(
                    slug = "globex",
                    name = "Globex",
                    ownerUserId = "eve",
                    ownerDisplayName = "Eve"
                )
            )
        }

        val error = shouldThrow<DomainValidationException> {
            runBlocking {
                fixture.commandService.createChannel(
                    firstWorkspace.id,
                    CreateChannelRequest(
                        slug = "general",
                        name = "general",
                        topic = "Team updates",
                        visibility = ChannelVisibility.PUBLIC,
                        createdByMemberId = secondWorkspace.ownerMemberId.toString()
                    )
                )
            }
        }

        error.message shouldBe "channel creator must belong to the workspace"
    }

    test("replyToMessage always binds replies to the thread root") {
        val fixture = messagingFixture()
        val workspace = runBlocking {
            fixture.commandService.createWorkspace(
                CreateWorkspaceRequest(
                    slug = "acme",
                    name = "Acme",
                    ownerUserId = "alice",
                    ownerDisplayName = "Alice"
                )
            )
        }
        val channel = runBlocking {
                fixture.commandService.createChannel(
                    workspace.id,
                    CreateChannelRequest(
                        slug = "design",
                        name = "design",
                        topic = "Team updates",
                        visibility = ChannelVisibility.PUBLIC,
                        createdByMemberId = workspace.ownerMemberId.toString()
                    )
                )
        }
        val root = runBlocking {
            fixture.commandService.postChannelMessage(
                channel.id,
                PostMessageRequest(
                    authorMemberId = workspace.ownerMemberId.toString(),
                    body = "hello"
                )
            )
        }
        val firstReply = runBlocking {
            fixture.commandService.replyToMessage(
                root.id,
                PostMessageRequest(
                    authorMemberId = workspace.ownerMemberId.toString(),
                    body = "first reply"
                )
            )
        }
        val nestedReply = runBlocking {
            fixture.commandService.replyToMessage(
                firstReply.id,
                PostMessageRequest(
                    authorMemberId = workspace.ownerMemberId.toString(),
                    body = "nested reply"
                )
            )
        }

        firstReply.threadRootMessageId shouldBe root.id
        nestedReply.threadRootMessageId shouldBe root.id
    }

    test("listChannelMessages rejects limits outside the supported range") {
        val fixture = messagingFixture()

        val error = shouldThrow<DomainValidationException> {
            runBlocking {
                fixture.queryService.listChannelMessages(Uuid.generateV7(), beforeMessageId = null, limit = 101)
            }
        }

        error.message shouldBe "limit must be between 1 and 100"
    }

    test("getThread resolves the root even when queried by reply id") {
        val fixture = messagingFixture()
        val workspace = runBlocking {
            fixture.commandService.createWorkspace(
                CreateWorkspaceRequest(
                    slug = "acme",
                    name = "Acme",
                    ownerUserId = "alice",
                    ownerDisplayName = "Alice"
                )
            )
        }
        val channel = runBlocking {
                fixture.commandService.createChannel(
                    workspace.id,
                    CreateChannelRequest(
                        slug = "design",
                        name = "design",
                        topic = "Team updates",
                        visibility = ChannelVisibility.PUBLIC,
                        createdByMemberId = workspace.ownerMemberId.toString()
                    )
                )
        }
        val root = runBlocking {
            fixture.commandService.postChannelMessage(
                channel.id,
                PostMessageRequest(
                    authorMemberId = workspace.ownerMemberId.toString(),
                    body = "root"
                )
            )
        }
        val reply = runBlocking {
            fixture.commandService.replyToMessage(
                root.id,
                PostMessageRequest(
                    authorMemberId = workspace.ownerMemberId.toString(),
                    body = "reply"
                )
            )
        }
        val secondReply = runBlocking {
            fixture.commandService.replyToMessage(
                root.id,
                PostMessageRequest(
                    authorMemberId = workspace.ownerMemberId.toString(),
                    body = "second reply"
                )
            )
        }

        val thread = runBlocking { fixture.queryService.getThread(reply.id) }

        thread.first shouldBe root
        thread.second shouldContainExactly listOf(reply, secondReply)
    }

    test("getWorkspaceBySlug resolves an existing workspace") {
        val fixture = messagingFixture()
        val workspace = runBlocking {
            fixture.commandService.createWorkspace(
                CreateWorkspaceRequest(
                    slug = "acme",
                    name = "Acme",
                    ownerUserId = "alice",
                    ownerDisplayName = "Alice"
                )
            )
        }

        val resolved = runBlocking { fixture.queryService.getWorkspaceBySlug("acme") }

        resolved shouldBe workspace
    }

    test("listWorkspaces returns created workspaces in insertion order") {
        val fixture = messagingFixture()

        val first = runBlocking {
            fixture.commandService.createWorkspace(
                CreateWorkspaceRequest(
                    slug = "acme",
                    name = "Acme",
                    ownerUserId = "alice",
                    ownerDisplayName = "Alice"
                )
            )
        }
        val second = runBlocking {
            fixture.commandService.createWorkspace(
                CreateWorkspaceRequest(
                    slug = "globex",
                    name = "Globex",
                    ownerUserId = "eve",
                    ownerDisplayName = "Eve"
                )
            )
        }

        runBlocking { fixture.queryService.listWorkspaces() } shouldContainExactly listOf(first, second)
    }

    test("mentions create unread notifications and can be marked read") {
        val fixture = messagingFixture()
        val workspace = runBlocking {
            fixture.commandService.createWorkspace(
                CreateWorkspaceRequest(
                    slug = "acme",
                    name = "Acme",
                    ownerUserId = "alice",
                    ownerDisplayName = "Alice"
                )
            )
        }
        val bob = runBlocking {
            fixture.commandService.addMember(
                workspace.id,
                com.retheviper.chat.contract.AddWorkspaceMemberRequest(
                    userId = "bob",
                    displayName = "Bob"
                )
            )
        }
        val channel = runBlocking { fixture.queryService.listChannels(workspace.id).single() }

        runBlocking {
            fixture.commandService.postChannelMessage(
                channel.id,
                PostMessageRequest(
                    authorMemberId = workspace.ownerMemberId.toString(),
                    body = "hello @bob"
                )
            )
        }

        val unread = runBlocking { fixture.queryService.listMemberNotifications(bob.id, unreadOnly = true) }
        unread.size shouldBe 1
        unread.single().messagePreview shouldBe "hello @bob"

        runBlocking {
            fixture.commandService.markNotificationsRead(bob.id, unread.map { it.id })
        }

        runBlocking { fixture.queryService.listMemberNotifications(bob.id, unreadOnly = true) } shouldContainExactly emptyList()
    }

    test("thread replies notify prior participants after they have replied") {
        val fixture = messagingFixture()
        val workspace = runBlocking {
            fixture.commandService.createWorkspace(
                CreateWorkspaceRequest(
                    slug = "acme",
                    name = "Acme",
                    ownerUserId = "alice",
                    ownerDisplayName = "Alice"
                )
            )
        }
        val bob = runBlocking {
            fixture.commandService.addMember(
                workspace.id,
                com.retheviper.chat.contract.AddWorkspaceMemberRequest(
                    userId = "bob",
                    displayName = "Bob"
                )
            )
        }
        val channel = runBlocking { fixture.queryService.listChannels(workspace.id).single() }
        val root = runBlocking {
            fixture.commandService.postChannelMessage(
                channel.id,
                PostMessageRequest(
                    authorMemberId = workspace.ownerMemberId.toString(),
                    body = "root"
                )
            )
        }

        runBlocking {
            fixture.commandService.replyToMessage(
                root.id,
                PostMessageRequest(
                    authorMemberId = bob.id.toString(),
                    body = "I am in"
                )
            )
        }
        runBlocking {
            fixture.commandService.replyToMessage(
                root.id,
                PostMessageRequest(
                    authorMemberId = workspace.ownerMemberId.toString(),
                    body = "follow-up"
                )
            )
        }

        val unread = runBlocking { fixture.queryService.listMemberNotifications(bob.id, unreadOnly = true) }
        unread.size shouldBe 1
        unread.single().kind shouldBe NotificationKind.THREAD_ACTIVITY
        unread.single().threadRootMessageId shouldBe root.id
        unread.single().messagePreview shouldBe "follow-up"
    }

    test("thread replies notify members who were previously mentioned on the root message") {
        val fixture = messagingFixture()
        val workspace = runBlocking {
            fixture.commandService.createWorkspace(
                CreateWorkspaceRequest(
                    slug = "acme",
                    name = "Acme",
                    ownerUserId = "alice",
                    ownerDisplayName = "Alice"
                )
            )
        }
        val bob = runBlocking {
            fixture.commandService.addMember(
                workspace.id,
                com.retheviper.chat.contract.AddWorkspaceMemberRequest(
                    userId = "bob",
                    displayName = "Bob"
                )
            )
        }
        val channel = runBlocking { fixture.queryService.listChannels(workspace.id).single() }
        val root = runBlocking {
            fixture.commandService.postChannelMessage(
                channel.id,
                PostMessageRequest(
                    authorMemberId = workspace.ownerMemberId.toString(),
                    body = "hello @bob"
                )
            )
        }

        runBlocking {
            fixture.commandService.markNotificationsRead(
                bob.id,
                fixture.queryService.listMemberNotifications(bob.id, unreadOnly = true).map { it.id }
            )
        }
        runBlocking {
            fixture.commandService.replyToMessage(
                root.id,
                PostMessageRequest(
                    authorMemberId = workspace.ownerMemberId.toString(),
                    body = "thread follow-up"
                )
            )
        }

        val unread = runBlocking { fixture.queryService.listMemberNotifications(bob.id, unreadOnly = true) }
        unread.size shouldBe 1
        unread.single().kind shouldBe NotificationKind.THREAD_ACTIVITY
        unread.single().threadRootMessageId shouldBe root.id
        unread.single().messagePreview shouldBe "thread follow-up"
    }

    test("thread replies notify members who were previously mentioned inside the thread") {
        val fixture = messagingFixture()
        val workspace = runBlocking {
            fixture.commandService.createWorkspace(
                CreateWorkspaceRequest(
                    slug = "acme",
                    name = "Acme",
                    ownerUserId = "alice",
                    ownerDisplayName = "Alice"
                )
            )
        }
        val bob = runBlocking {
            fixture.commandService.addMember(
                workspace.id,
                com.retheviper.chat.contract.AddWorkspaceMemberRequest(
                    userId = "bob",
                    displayName = "Bob"
                )
            )
        }
        val channel = runBlocking { fixture.queryService.listChannels(workspace.id).single() }
        val root = runBlocking {
            fixture.commandService.postChannelMessage(
                channel.id,
                PostMessageRequest(
                    authorMemberId = workspace.ownerMemberId.toString(),
                    body = "root"
                )
            )
        }

        runBlocking {
            fixture.commandService.replyToMessage(
                root.id,
                PostMessageRequest(
                    authorMemberId = workspace.ownerMemberId.toString(),
                    body = "hello @bob"
                )
            )
        }
        runBlocking {
            fixture.commandService.markNotificationsRead(
                bob.id,
                fixture.queryService.listMemberNotifications(bob.id, unreadOnly = true).map { it.id }
            )
        }
        runBlocking {
            fixture.commandService.replyToMessage(
                root.id,
                PostMessageRequest(
                    authorMemberId = workspace.ownerMemberId.toString(),
                    body = "another follow-up"
                )
            )
        }

        val unread = runBlocking { fixture.queryService.listMemberNotifications(bob.id, unreadOnly = true) }
        unread.size shouldBe 1
        unread.single().kind shouldBe NotificationKind.THREAD_ACTIVITY
        unread.single().threadRootMessageId shouldBe root.id
        unread.single().messagePreview shouldBe "another follow-up"
    }

    test("updateMember changes the display name") {
        val fixture = messagingFixture()
        val workspace = runBlocking {
            fixture.commandService.createWorkspace(
                CreateWorkspaceRequest(
                    slug = "acme",
                    name = "Acme",
                    ownerUserId = "alice",
                    ownerDisplayName = "Alice"
                )
            )
        }

        val updated = runBlocking {
            fixture.commandService.updateMember(
                workspace.ownerMemberId,
                com.retheviper.chat.contract.UpdateWorkspaceMemberRequest(displayName = "Alice Cooper")
            )
        }

        updated.displayName shouldBe "Alice Cooper"
        runBlocking { fixture.queryService.listMembers(workspace.id).single { it.id == workspace.ownerMemberId } }.displayName shouldBe "Alice Cooper"
    }

    test("postChannelMessage keeps a link preview only when the preview url exists in the body") {
        val fixture = messagingFixture()
        val workspace = runBlocking {
            fixture.commandService.createWorkspace(
                CreateWorkspaceRequest(
                    slug = "acme",
                    name = "Acme",
                    ownerUserId = "alice",
                    ownerDisplayName = "Alice"
                )
            )
        }
        val channel = runBlocking { fixture.queryService.listChannels(workspace.id).single() }

        val stored = runBlocking {
            fixture.commandService.postChannelMessage(
                channel.id,
                PostMessageRequest(
                    authorMemberId = workspace.ownerMemberId.toString(),
                    body = "look at https://example.com/article",
                    linkPreview = LinkPreviewResponse(
                        url = "https://example.com/article",
                        title = "Example"
                    )
                )
            )
        }
        val dropped = runBlocking {
            fixture.commandService.postChannelMessage(
                channel.id,
                PostMessageRequest(
                    authorMemberId = workspace.ownerMemberId.toString(),
                    body = "no link here",
                    linkPreview = LinkPreviewResponse(
                        url = "https://example.com/article",
                        title = "Example"
                    )
                )
            )
        }

        stored.linkPreview?.url shouldBe "https://example.com/article"
        dropped.linkPreview shouldBe null
    }

    test("toggleReaction aggregates counts and removes the member reaction on second click") {
        val fixture = messagingFixture()
        val workspace = runBlocking {
            fixture.commandService.createWorkspace(
                CreateWorkspaceRequest(
                    slug = "acme",
                    name = "Acme",
                    ownerUserId = "alice",
                    ownerDisplayName = "Alice"
                )
            )
        }
        val bob = runBlocking {
            fixture.commandService.addMember(
                workspace.id,
                com.retheviper.chat.contract.AddWorkspaceMemberRequest(
                    userId = "bob",
                    displayName = "Bob"
                )
            )
        }
        val channel = runBlocking { fixture.queryService.listChannels(workspace.id).single() }
        val root = runBlocking {
            fixture.commandService.postChannelMessage(
                channel.id,
                PostMessageRequest(
                    authorMemberId = workspace.ownerMemberId.toString(),
                    body = "Ship it"
                )
            )
        }

        runBlocking { fixture.commandService.toggleReaction(root.id, workspace.ownerMemberId, "👍") }
        val updatedOnce = runBlocking { fixture.commandService.toggleReaction(root.id, bob.id, "👍") }

        updatedOnce.reactions.single().emoji shouldBe "👍"
        updatedOnce.reactions.single().count shouldBe 2

        val updatedTwice = runBlocking { fixture.commandService.toggleReaction(root.id, bob.id, "👍") }
        updatedTwice.reactions.single().count shouldBe 1
        updatedTwice.reactions.single().memberIds shouldContainExactly listOf(workspace.ownerMemberId)
    }
})

@OptIn(ExperimentalUuidApi::class)
private data class MessagingFixture(
    val commandService: MessagingCommandService,
    val queryService: MessagingQueryService
)

@OptIn(ExperimentalUuidApi::class)
private fun messagingFixture(): MessagingFixture {
    val workspaceRepository = InMemoryWorkspaceRepository()
    val channelRepository = InMemoryChannelRepository()
    val messageRepository = InMemoryMessageRepository()
    val messageReactionRepository = InMemoryMessageReactionRepository()
    val mentionNotificationRepository = InMemoryMentionNotificationRepository()

    return MessagingFixture(
        commandService = MessagingCommandService(
            workspaceRepository = workspaceRepository,
            channelRepository = channelRepository,
            messageRepository = messageRepository,
            messageReactionRepository = messageReactionRepository,
            mentionNotificationRepository = mentionNotificationRepository,
            notificationEventBus = NotificationEventBus(),
            transactionsEnabled = false
        ),
        queryService = MessagingQueryService(
            workspaceRepository = workspaceRepository,
            channelRepository = channelRepository,
            messageRepository = messageRepository,
            messageReactionRepository = messageReactionRepository,
            mentionNotificationRepository = mentionNotificationRepository,
            transactionsEnabled = false
        )
    )
}

@OptIn(ExperimentalUuidApi::class)
private class InMemoryWorkspaceRepository : WorkspaceRepository {
    private val workspaces = linkedMapOf<Uuid, Workspace>()
    private val members = linkedMapOf<Uuid, WorkspaceMember>()

    override suspend fun existsBySlug(slug: String): Boolean {
        return workspaces.values.any { it.slug == slug.trim() }
    }

    override suspend fun saveWorkspace(workspace: Workspace) {
        workspaces[workspace.id] = workspace
    }

    override suspend fun findWorkspace(id: Uuid): Workspace? {
        return workspaces[id]
    }

    override suspend fun findWorkspaceBySlug(slug: String): Workspace? {
        return workspaces.values.firstOrNull { it.slug == slug.trim() }
    }

    override suspend fun listWorkspaces(): List<Workspace> {
        return workspaces.values.toList()
    }

    override suspend fun saveMember(member: WorkspaceMember) {
        members[member.id] = member
    }

    override suspend fun updateMemberDisplayName(memberId: Uuid, displayName: String) {
        val existing = members[memberId] ?: return
        members[memberId] = existing.copy(displayName = displayName)
    }

    override suspend fun findMember(id: Uuid): WorkspaceMember? {
        return members[id]
    }

    override suspend fun findMemberByUserId(workspaceId: Uuid, userId: String): WorkspaceMember? {
        return members.values.firstOrNull { it.workspaceId == workspaceId && it.userId == userId.trim() }
    }

    override suspend fun listMembers(workspaceId: Uuid): List<WorkspaceMember> {
        return members.values.filter { it.workspaceId == workspaceId }
    }
}

@OptIn(ExperimentalUuidApi::class)
private class InMemoryChannelRepository : ChannelRepository {
    private val channels = linkedMapOf<Uuid, Channel>()

    override suspend fun existsBySlug(workspaceId: Uuid, slug: String): Boolean {
        return channels.values.any { it.workspaceId == workspaceId && it.slug == slug.trim() }
    }

    override suspend fun saveChannel(channel: Channel) {
        channels[channel.id] = channel
    }

    override suspend fun findChannel(id: Uuid): Channel? {
        return channels[id]
    }

    override suspend fun listChannels(workspaceId: Uuid): List<Channel> {
        return channels.values.filter { it.workspaceId == workspaceId }
    }
}

@OptIn(ExperimentalUuidApi::class)
private class InMemoryMessageRepository : MessageRepository {
    private val messages = linkedMapOf<Uuid, Message>()

    override suspend fun saveMessage(message: Message) {
        messages[message.id] = message
    }

    override suspend fun findMessage(id: Uuid): Message? {
        return messages[id]
    }

    override suspend fun listChannelMessages(channelId: Uuid, beforeMessageId: Uuid?, limit: Int): List<Message> {
        val beforeMessage = beforeMessageId?.let(messages::get)
        return messages.values
            .filter { it.channelId == channelId }
            .filter { beforeMessage == null || it.createdAt < beforeMessage.createdAt }
            .take(limit)
    }

    override suspend fun listThread(rootMessageId: Uuid): List<Message> {
        return messages.values.filter { it.threadRootMessageId == rootMessageId }
    }

    override suspend fun listThreadReplyCounts(rootMessageIds: List<Uuid>): Map<Uuid, Int> {
        return messages.values
            .mapNotNull { it.threadRootMessageId }
            .filter { it in rootMessageIds }
            .groupingBy { it }
            .eachCount()
    }

    override suspend fun listThreadParticipantIds(rootMessageId: Uuid, beforeCreatedAt: Instant): Set<Uuid> {
        return messages.values
            .asSequence()
            .filter { it.threadRootMessageId == rootMessageId && it.createdAt < beforeCreatedAt }
            .mapTo(linkedSetOf()) { it.authorMemberId }
    }
}

@OptIn(ExperimentalUuidApi::class)
private class InMemoryMentionNotificationRepository : MentionNotificationRepository {
    private val notifications = linkedMapOf<Uuid, MentionNotification>()

    override suspend fun saveNotifications(notifications: List<MentionNotification>) {
        notifications.forEach { notification -> this.notifications[notification.id] = notification }
    }

    override suspend fun listMemberNotifications(memberId: Uuid, unreadOnly: Boolean): List<MentionNotification> {
        return notifications.values
            .filter { it.memberId == memberId }
            .filter { !unreadOnly || it.readAt == null }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun listThreadSubscriberIds(rootMessageId: Uuid): Set<Uuid> {
        return notifications.values
            .filter { it.messageId == rootMessageId || it.threadRootMessageId == rootMessageId }
            .mapTo(linkedSetOf()) { it.memberId }
    }

    override suspend fun markRead(memberId: Uuid, notificationIds: List<Uuid>, readAt: Instant) {
        notificationIds.forEach { id ->
            val current = notifications[id] ?: return@forEach
            if (current.memberId == memberId) {
                notifications[id] = current.copy(readAt = readAt)
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
private class InMemoryMessageReactionRepository : MessageReactionRepository {
    private val reactions = linkedMapOf<Uuid, MessageReaction>()

    override suspend fun saveReaction(reaction: MessageReaction) {
        reactions[reaction.id] = reaction
    }

    override suspend fun findReaction(messageId: Uuid, memberId: Uuid, emoji: String): MessageReaction? {
        return reactions.values.firstOrNull {
            it.messageId == messageId && it.memberId == memberId && it.emoji == emoji
        }
    }

    override suspend fun deleteReaction(reactionId: Uuid) {
        reactions.remove(reactionId)
    }

    override suspend fun listReactions(messageIds: List<Uuid>): List<MessageReaction> {
        return reactions.values.filter { it.messageId in messageIds }
    }
}
