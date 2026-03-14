package com.retheviper.chat.app

import com.retheviper.chat.contract.ChatCommandType
import com.retheviper.chat.contract.ChatEvent
import com.retheviper.chat.contract.ChatEventType
import com.retheviper.chat.contract.LinkPreviewResponse
import com.retheviper.chat.contract.MessageReactionResponse
import com.retheviper.chat.contract.MessageResponse
import com.retheviper.chat.contract.ThreadResponse
import com.retheviper.chat.contract.WorkspaceMemberResponse
import com.retheviper.chat.contract.WorkspaceMemberRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class MessagingUiModelTest : FunSpec({
    test("snapshot replaces the visible feed") {
        val initial = ChatFeedState(messages = listOf(message(id = "m-1", body = "old")))
        val next = reduceChatFeed(
            current = initial,
            event = ChatEvent(
                type = ChatEventType.SNAPSHOT,
                messages = listOf(message(id = "m-2", body = "new"))
            )
        )

        next.messages shouldContainExactly listOf(message(id = "m-2", body = "new"))
    }

    test("message posted appends to the visible feed") {
        val appended = message(id = "m-2", body = "second")
        val next = reduceChatFeed(
            current = ChatFeedState(messages = listOf(message(id = "m-1", body = "first"))),
            event = ChatEvent(
                type = ChatEventType.MESSAGE_POSTED,
                message = appended
            )
        )

        next.messages shouldContainExactly listOf(
            message(id = "m-1", body = "first"),
            appended
        )
    }

    test("reply posted increments the visible thread reply count for the root message") {
        val next = reduceChatFeed(
            current = ChatFeedState(messages = listOf(message(id = "root-1", body = "first", threadReplyCount = 2))),
            event = ChatEvent(
                type = ChatEventType.REPLY_POSTED,
                message = message(id = "reply-1", body = "reply", threadRootMessageId = "root-1")
            )
        )

        next.messages.single().threadReplyCount shouldBe 3
    }

    test("reaction updated replaces the matching message in the visible feed") {
        val next = reduceChatFeed(
            current = ChatFeedState(messages = listOf(message(id = "root-1", body = "first"))),
            event = ChatEvent(
                type = ChatEventType.REACTION_UPDATED,
                message = message(
                    id = "root-1",
                    body = "first",
                    reactions = listOf(
                        MessageReactionResponse(
                            emoji = "👍",
                            count = 2,
                            memberIds = listOf("member-1", "member-2")
                        )
                    )
                )
            )
        )

        next.messages.single().reactions.single().count shouldBe 2
    }

    test("buildOutgoingChatCommand creates a channel post for root messages") {
        val command = buildOutgoingChatCommand(
            primaryAuthorId = "owner-1",
            body = "  hello world  ",
            replyParentMessageId = ""
        )

        command?.type shouldBe ChatCommandType.POST_MESSAGE
        command?.authorMemberId shouldBe "owner-1"
        command?.body shouldBe "hello world"
        command?.parentMessageId.shouldBeNull()
    }

    test("buildOutgoingChatCommand preserves a resolved link preview") {
        val preview = LinkPreviewResponse(url = "https://example.com/article", title = "Example")

        val command = buildOutgoingChatCommand(
            primaryAuthorId = "owner-1",
            body = "hello https://example.com/article",
            replyParentMessageId = "",
            linkPreview = preview
        )

        command?.linkPreview shouldBe preview
    }

    test("buildOutgoingChatCommand creates a thread reply for the selected root") {
        val command = buildOutgoingChatCommand(
            primaryAuthorId = "member-2",
            body = "reply",
            replyParentMessageId = "  root-1  "
        )

        command?.type shouldBe ChatCommandType.REPLY_MESSAGE
        command?.authorMemberId shouldBe "member-2"
        command?.body shouldBe "reply"
        command?.parentMessageId shouldBe "root-1"
    }

    test("buildOutgoingChatCommand returns null when the body is blank") {
        val command = buildOutgoingChatCommand(
            primaryAuthorId = "owner-1",
            body = "   ",
            replyParentMessageId = ""
        )

        command.shouldBeNull()
    }

    test("toThreadMessages returns root followed by replies") {
        val root = message(id = "root-1", body = "root")
        val reply = message(id = "reply-1", body = "reply", threadRootMessageId = "root-1")

        toThreadMessages(
            ThreadResponse(
                root = root,
                replies = listOf(reply)
            )
        ) shouldContainExactly listOf(root, reply)
    }

    test("planWorkspaceJoin returns existing member when user id already exists") {
        val existing = member(userId = "alice", displayName = "Alice")

        val plan = planWorkspaceJoin(
            members = listOf(existing),
            userId = "alice",
            displayName = "Ignored"
        )

        plan?.existingMember shouldBe existing
        plan?.createRequest.shouldBeNull()
    }

    test("planWorkspaceJoin returns create request for a new member") {
        val plan = planWorkspaceJoin(
            members = emptyList(),
            userId = "bob",
            displayName = "Bob"
        )

        plan?.existingMember.shouldBeNull()
        plan?.createRequest?.userId shouldBe "bob"
        plan?.createRequest?.displayName shouldBe "Bob"
    }

    test("suggestMentionCandidates returns matches for the active mention") {
        val candidates = suggestMentionCandidates(
            members = listOf(
                member(userId = "alice", displayName = "Alice"),
                member(userId = "bob", displayName = "Bob")
            ),
            text = "hello @bo"
        )

        candidates.map { it.userId } shouldContainExactly listOf("bob")
    }

    test("applyMentionCandidate replaces the active mention token") {
        applyMentionCandidate("hello @bo", "bob") shouldBe "hello @bob "
    }
})

private fun message(
    id: String,
    body: String,
    threadRootMessageId: String? = null,
    threadReplyCount: Int = 0,
    reactions: List<MessageReactionResponse> = emptyList()
): MessageResponse {
    return MessageResponse(
        id = id,
        channelId = "channel-1",
        authorMemberId = "member-1",
        authorDisplayName = "Alice",
        body = body,
        threadRootMessageId = threadRootMessageId,
        threadReplyCount = threadReplyCount,
        reactions = reactions,
        createdAt = "2026-03-14T00:00:00Z"
    )
}

private fun member(
    userId: String,
    displayName: String
): WorkspaceMemberResponse {
    return WorkspaceMemberResponse(
        id = "member-$userId",
        workspaceId = "workspace-1",
        userId = userId,
        displayName = displayName,
        role = WorkspaceMemberRole.MEMBER,
        joinedAt = "2026-03-14T00:00:00Z"
    )
}
