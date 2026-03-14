import Foundation

struct ApiErrorResponse: Codable, Error {
    let code: String
    let message: String
}

enum WorkspaceMemberRole: String, Codable {
    case owner = "OWNER"
    case admin = "ADMIN"
    case member = "MEMBER"
}

enum ChannelVisibility: String, Codable {
    case `public` = "PUBLIC"
    case `private` = "PRIVATE"
}

enum NotificationKind: String, Codable {
    case mention = "MENTION"
    case threadActivity = "THREAD_ACTIVITY"
}

enum ChatCommandType: String, Codable {
    case loadRecent = "LOAD_RECENT"
    case postMessage = "POST_MESSAGE"
    case replyMessage = "REPLY_MESSAGE"
    case toggleReaction = "TOGGLE_REACTION"
}

enum ChatEventType: String, Codable {
    case snapshot = "SNAPSHOT"
    case messagePosted = "MESSAGE_POSTED"
    case replyPosted = "REPLY_POSTED"
    case reactionUpdated = "REACTION_UPDATED"
    case error = "ERROR"
}

struct CreateWorkspaceRequest: Codable {
    let slug: String
    let name: String
    let ownerUserId: String
    let ownerDisplayName: String
}

struct WorkspaceResponse: Codable, Identifiable, Hashable {
    let id: String
    let slug: String
    let name: String
    let ownerMemberId: String
    let createdAt: String
}

struct AddWorkspaceMemberRequest: Codable {
    let userId: String
    let displayName: String
    let role: WorkspaceMemberRole

    init(userId: String, displayName: String, role: WorkspaceMemberRole = .member) {
        self.userId = userId
        self.displayName = displayName
        self.role = role
    }
}

struct UpdateWorkspaceMemberRequest: Codable {
    let displayName: String
}

struct WorkspaceMemberResponse: Codable, Identifiable, Hashable {
    let id: String
    let workspaceId: String
    let userId: String
    let displayName: String
    let role: WorkspaceMemberRole
    let joinedAt: String
}

struct CreateChannelRequest: Codable {
    let slug: String
    let name: String
    let topic: String?
    let visibility: ChannelVisibility
    let createdByMemberId: String
}

struct ChannelResponse: Codable, Identifiable, Hashable {
    let id: String
    let workspaceId: String
    let slug: String
    let name: String
    let topic: String?
    let visibility: ChannelVisibility
    let createdByMemberId: String
    let createdAt: String
}

struct ResolveLinkPreviewRequest: Codable {
    let url: String
}

struct ResolveLinkPreviewResponse: Codable {
    let preview: LinkPreviewResponse?
}

struct LinkPreviewResponse: Codable, Hashable {
    let url: String
    let title: String?
    let description: String?
    let imageUrl: String?
    let siteName: String?
}

struct PostMessageRequest: Codable {
    let authorMemberId: String
    let body: String
    let linkPreview: LinkPreviewResponse?
}

struct ToggleReactionRequest: Codable {
    let memberId: String
    let emoji: String
}

struct MarkNotificationsReadRequest: Codable {
    let notificationIds: [String]
}

struct MessageReactionResponse: Codable, Hashable {
    let emoji: String
    let count: Int
    let memberIds: [String]
}

struct MessageResponse: Codable, Identifiable, Hashable {
    let id: String
    let channelId: String
    let authorMemberId: String
    let authorDisplayName: String
    let body: String
    let linkPreview: LinkPreviewResponse?
    let threadRootMessageId: String?
    let threadReplyCount: Int
    let reactions: [MessageReactionResponse]
    let createdAt: String

    enum CodingKeys: String, CodingKey {
        case id
        case channelId
        case authorMemberId
        case authorDisplayName
        case body
        case linkPreview
        case threadRootMessageId
        case threadReplyCount
        case reactions
        case createdAt
    }

    init(
        id: String,
        channelId: String,
        authorMemberId: String,
        authorDisplayName: String,
        body: String,
        linkPreview: LinkPreviewResponse? = nil,
        threadRootMessageId: String? = nil,
        threadReplyCount: Int = 0,
        reactions: [MessageReactionResponse] = [],
        createdAt: String
    ) {
        self.id = id
        self.channelId = channelId
        self.authorMemberId = authorMemberId
        self.authorDisplayName = authorDisplayName
        self.body = body
        self.linkPreview = linkPreview
        self.threadRootMessageId = threadRootMessageId
        self.threadReplyCount = threadReplyCount
        self.reactions = reactions
        self.createdAt = createdAt
    }

    init(from decoder: any Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        channelId = try container.decode(String.self, forKey: .channelId)
        authorMemberId = try container.decode(String.self, forKey: .authorMemberId)
        authorDisplayName = try container.decode(String.self, forKey: .authorDisplayName)
        body = try container.decode(String.self, forKey: .body)
        linkPreview = try container.decodeIfPresent(LinkPreviewResponse.self, forKey: .linkPreview)
        threadRootMessageId = try container.decodeIfPresent(String.self, forKey: .threadRootMessageId)
        threadReplyCount = try container.decodeIfPresent(Int.self, forKey: .threadReplyCount) ?? 0
        reactions = try container.decodeIfPresent([MessageReactionResponse].self, forKey: .reactions) ?? []
        createdAt = try container.decode(String.self, forKey: .createdAt)
    }
}

struct MessagePageResponse: Codable {
    let messages: [MessageResponse]
}

struct ThreadResponse: Codable {
    let root: MessageResponse
    let replies: [MessageResponse]
}

struct MentionNotificationResponse: Codable, Identifiable, Hashable {
    let id: String
    let kind: NotificationKind
    let memberId: String
    let channelId: String
    let messageId: String
    let threadRootMessageId: String?
    let authorDisplayName: String
    let messagePreview: String
    let createdAt: String
    let readAt: String?
}

struct ChatCommand: Codable {
    let type: ChatCommandType
    let authorMemberId: String?
    let body: String?
    let linkPreview: LinkPreviewResponse?
    let parentMessageId: String?
    let messageId: String?
    let emoji: String?
    let limit: Int?
}

struct ChatEvent: Codable {
    let type: ChatEventType
    let message: MessageResponse?
    let messages: [MessageResponse]
    let error: ApiErrorResponse?

    enum CodingKeys: String, CodingKey {
        case type
        case message
        case messages
        case error
    }

    init(
        type: ChatEventType,
        message: MessageResponse? = nil,
        messages: [MessageResponse] = [],
        error: ApiErrorResponse? = nil
    ) {
        self.type = type
        self.message = message
        self.messages = messages
        self.error = error
    }

    init(from decoder: any Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        type = try container.decode(ChatEventType.self, forKey: .type)
        message = try container.decodeIfPresent(MessageResponse.self, forKey: .message)
        messages = try container.decodeIfPresent([MessageResponse].self, forKey: .messages) ?? []
        error = try container.decodeIfPresent(ApiErrorResponse.self, forKey: .error)
    }
}
