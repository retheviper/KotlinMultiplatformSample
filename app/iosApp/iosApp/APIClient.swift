import Foundation
import shared

struct APIClient {
    var baseURL: URL
    private let client: shared.MessagingClient

    init(baseURL: URL) {
        self.baseURL = baseURL
        let normalizedBaseURL = baseURL.absoluteString.hasSuffix("/") ? String(baseURL.absoluteString.dropLast()) : baseURL.absoluteString
        self.client = shared.MessagingClient(baseUrl: normalizedBaseURL)
    }

    func listWorkspaces() async throws -> [WorkspaceResponse] {
        let workspaces = try await client.listWorkspaces()
        return workspaces.map(\.native)
    }

    func createWorkspace(_ requestBody: CreateWorkspaceRequest) async throws -> WorkspaceResponse {
        let workspace = try await client.createWorkspace(request: requestBody.sharedRequest)
        return workspace.native
    }

    func listMembers(workspaceId: String) async throws -> [WorkspaceMemberResponse] {
        let members = try await client.listWorkspaceMembers(workspaceId: workspaceId)
        return members.map(\.native)
    }

    func addMember(workspaceId: String, requestBody: AddWorkspaceMemberRequest) async throws -> WorkspaceMemberResponse {
        let member = try await client.addWorkspaceMember(workspaceId: workspaceId, request: requestBody.sharedRequest)
        return member.native
    }

    func updateMember(memberId: String, displayName: String) async throws -> WorkspaceMemberResponse {
        let member = try await client.updateWorkspaceMember(
            memberId: memberId,
            request: UpdateWorkspaceMemberRequest(displayName: displayName).sharedRequest
        )
        return member.native
    }

    func listChannels(workspaceId: String) async throws -> [ChannelResponse] {
        let channels = try await client.listWorkspaceChannels(workspaceId: workspaceId)
        return channels.map(\.native)
    }

    func createChannel(workspaceId: String, requestBody: CreateChannelRequest) async throws -> ChannelResponse {
        let channel = try await client.createChannel(workspaceId: workspaceId, request: requestBody.sharedRequest)
        return channel.native
    }

    func listMessages(channelId: String, beforeMessageId: String? = nil, limit: Int = 50) async throws -> [MessageResponse] {
        let page = try await client.listChannelMessages(
            channelId: channelId,
            limit: Int32(limit),
            beforeMessageId: beforeMessageId
        )
        return page.messages.map(\.native)
    }

    func getThread(messageId: String) async throws -> ThreadResponse {
        let thread = try await client.getThread(messageId: messageId)
        return thread.native
    }

    func listNotifications(memberId: String, unreadOnly: Bool) async throws -> [MentionNotificationResponse] {
        let notifications = try await client.listNotifications(memberId: memberId, unreadOnly: unreadOnly)
        return notifications.map(\.native)
    }

    func markNotificationsRead(memberId: String, ids: [String]) async throws {
        try await client.markNotificationsRead(memberId: memberId, notificationIds: ids)
    }

    func resolveLinkPreview(url: String) async throws -> LinkPreviewResponse? {
        let response = try await client.resolveLinkPreview(url: url)
        return response.preview?.native
    }

    func openChat(channelId: String) async throws -> ChatSocketSession {
        let session = try await client.openChat(channelId: channelId)
        return ChatSocketSession(raw: session)
    }

    func sendCommand(session: ChatSocketSession, command: ChatCommand) async throws {
        try await client.sendCommand(session: session.raw, command: command.sharedCommand)
    }

    func receiveEvent(session: ChatSocketSession) async throws -> ChatEvent {
        let event = try await client.receiveEvent(session: session.raw)
        return event.native
    }

    func close() async throws {
        try await client.close()
    }

    func notificationStreamRequest(memberId: String) throws -> URLRequest {
        var components = URLComponents(url: baseURL, resolvingAgainstBaseURL: false)!
        components.path = "/api/v1/members/\(memberId)/notifications/stream"
        components.query = nil
        guard let url = components.url else {
            throw ApiErrorResponse(code: "invalid_url", message: "Unable to build notification stream URL.")
        }

        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("text/event-stream", forHTTPHeaderField: "Accept")
        request.timeoutInterval = 60 * 60
        return request
    }
}

struct ChatSocketSession {
    let raw: shared.Ktor_client_coreDefaultClientWebSocketSession
}

private extension CreateWorkspaceRequest {
    var sharedRequest: shared.CreateWorkspaceRequest {
        shared.CreateWorkspaceRequest(
            slug: slug,
            name: name,
            ownerUserId: ownerUserId,
            ownerDisplayName: ownerDisplayName
        )
    }
}

private extension AddWorkspaceMemberRequest {
    var sharedRequest: shared.AddWorkspaceMemberRequest {
        shared.AddWorkspaceMemberRequest(userId: userId, displayName: displayName, role: role.sharedRole)
    }
}

private extension UpdateWorkspaceMemberRequest {
    var sharedRequest: shared.UpdateWorkspaceMemberRequest {
        shared.UpdateWorkspaceMemberRequest(displayName: displayName)
    }
}

private extension CreateChannelRequest {
    var sharedRequest: shared.CreateChannelRequest {
        shared.CreateChannelRequest(
            slug: slug,
            name: name,
            topic: topic,
            visibility: visibility.sharedVisibility,
            createdByMemberId: createdByMemberId
        )
    }
}

private extension ChatCommand {
    var sharedCommand: shared.ChatCommand {
        shared.ChatCommand(
            type: type.sharedType,
            authorMemberId: authorMemberId,
            body: body,
            linkPreview: linkPreview?.sharedPreview,
            parentMessageId: parentMessageId,
            messageId: messageId,
            emoji: emoji,
            limit: limit.map { shared.KotlinInt(int: Int32($0)) }
        )
    }
}

private extension LinkPreviewResponse {
    var sharedPreview: shared.LinkPreviewResponse {
        shared.LinkPreviewResponse(
            url: url,
            title: title,
            description: description,
            imageUrl: imageUrl,
            siteName: siteName
        )
    }
}

private extension WorkspaceMemberRole {
    var sharedRole: shared.WorkspaceMemberRole {
        switch self {
        case .owner:
            .owner
        case .admin:
            .admin
        case .member:
            .member
        }
    }
}

private extension ChannelVisibility {
    var sharedVisibility: shared.ChannelVisibility {
        switch self {
        case .public:
            .public_
        case .private:
            .private_
        }
    }
}

private extension ChatCommandType {
    var sharedType: shared.ChatCommandType {
        switch self {
        case .loadRecent:
            .loadRecent
        case .postMessage:
            .postMessage
        case .replyMessage:
            .replyMessage
        case .toggleReaction:
            .toggleReaction
        }
    }
}

private extension shared.ApiErrorResponse {
    var native: ApiErrorResponse {
        ApiErrorResponse(code: code, message: message)
    }
}

private extension shared.WorkspaceResponse {
    var native: WorkspaceResponse {
        WorkspaceResponse(id: id, slug: slug, name: name, ownerMemberId: ownerMemberId, createdAt: createdAt)
    }
}

private extension shared.WorkspaceMemberResponse {
    var native: WorkspaceMemberResponse {
        WorkspaceMemberResponse(
            id: id,
            workspaceId: workspaceId,
            userId: userId,
            displayName: displayName,
            role: role.native,
            joinedAt: joinedAt
        )
    }
}

private extension shared.ChannelResponse {
    var native: ChannelResponse {
        ChannelResponse(
            id: id,
            workspaceId: workspaceId,
            slug: slug,
            name: name,
            topic: topic,
            visibility: visibility.native,
            createdByMemberId: createdByMemberId,
            createdAt: createdAt
        )
    }
}

private extension shared.LinkPreviewResponse {
    var native: LinkPreviewResponse {
        LinkPreviewResponse(url: url, title: title, description: description_, imageUrl: imageUrl, siteName: siteName)
    }
}

private extension shared.MessageReactionResponse {
    var native: MessageReactionResponse {
        MessageReactionResponse(emoji: emoji, count: Int(count), memberIds: memberIds)
    }
}

private extension shared.MessageResponse {
    var native: MessageResponse {
        MessageResponse(
            id: id,
            channelId: channelId,
            authorMemberId: authorMemberId,
            authorDisplayName: authorDisplayName,
            body: body,
            linkPreview: linkPreview?.native,
            threadRootMessageId: threadRootMessageId,
            threadReplyCount: Int(threadReplyCount),
            reactions: reactions.map(\.native),
            createdAt: createdAt
        )
    }
}

private extension shared.ThreadResponse {
    var native: ThreadResponse {
        ThreadResponse(root: root.native, replies: replies.map(\.native))
    }
}

private extension shared.MentionNotificationResponse {
    var native: MentionNotificationResponse {
        MentionNotificationResponse(
            id: id,
            kind: kind.native,
            memberId: memberId,
            channelId: channelId,
            messageId: messageId,
            threadRootMessageId: threadRootMessageId,
            authorDisplayName: authorDisplayName,
            messagePreview: messagePreview,
            createdAt: createdAt,
            readAt: readAt
        )
    }
}

private extension shared.ChatEvent {
    var native: ChatEvent {
        ChatEvent(
            type: type.native,
            message: message?.native,
            messages: messages.map(\.native),
            error: error?.native
        )
    }
}

private extension shared.WorkspaceMemberRole {
    var native: WorkspaceMemberRole {
        switch self {
        case .owner:
            .owner
        case .admin:
            .admin
        default:
            .member
        }
    }
}

private extension shared.ChannelVisibility {
    var native: ChannelVisibility {
        switch self {
        case .public_:
            .public
        default:
            .private
        }
    }
}

private extension shared.NotificationKind {
    var native: NotificationKind {
        switch self {
        case .mention:
            .mention
        default:
            .threadActivity
        }
    }
}

private extension shared.ChatEventType {
    var native: ChatEventType {
        switch self {
        case .snapshot:
            .snapshot
        case .messagePosted:
            .messagePosted
        case .replyPosted:
            .replyPosted
        case .reactionUpdated:
            .reactionUpdated
        default:
            .error
        }
    }
}
