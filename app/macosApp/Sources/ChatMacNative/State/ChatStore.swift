import Foundation

@MainActor
final class ChatStore: ObservableObject {
    enum Screen {
        case landing
        case memberSetup
        case workspace
    }

    @Published var baseURLString: String
    @Published var screen: Screen = .landing
    @Published var isLoading = false
    @Published var errorMessage: String?

    @Published var workspaces: [WorkspaceResponse] = []
    @Published var selectedWorkspace: WorkspaceResponse?
    @Published var members: [WorkspaceMemberResponse] = []
    @Published var selectedMember: WorkspaceMemberResponse?
    @Published var channels: [ChannelResponse] = []
    @Published var selectedChannel: ChannelResponse?
    @Published var messages: [MessageResponse] = []
    @Published var hasOlderMessages = false
    @Published var isLoadingOlderMessages = false
    @Published var threadRoot: MessageResponse?
    @Published var threadReplies: [MessageResponse] = []
    @Published var notifications: [MentionNotificationResponse] = []
    @Published var unreadNotificationCount = 0
    @Published var channelUnreadCounts: [String: Int] = [:]
    @Published var toastNotifications: [MentionNotificationResponse] = []
    @Published var showingNotifications = false

    @Published var workspaceSlug = ""
    @Published var workspaceName = ""
    @Published var ownerUserId = ""
    @Published var ownerDisplayName = ""

    @Published var memberUserId = ""
    @Published var memberDisplayName = ""
    @Published var memberRenameDraft = ""
    @Published var newChannelDialogPresented = false
    @Published var newChannelName = ""
    @Published var newChannelSlug = ""
    @Published var newChannelTopic = ""
    @Published var composerText = ""
    @Published var threadComposerText = ""
    @Published var composerPreview: LinkPreviewResponse?
    @Published var threadComposerPreview: LinkPreviewResponse?

    var client: APIClient
    var webSocket: URLSessionWebSocketTask?
    var receiveTask: Task<Void, Never>?
    var connectedChannelId: String?
    var notificationStreamTask: Task<Void, Never>?
    var notificationStreamMemberId: String?
    var seenUnreadNotificationIds = Set<String>()

    init(baseURLString: String) {
        self.baseURLString = baseURLString
        self.client = APIClient(baseURL: Self.normalizeBaseURL(baseURLString))
    }

    func connectServer() async {
        stopNotificationStream()
        refreshClient()
        await loadWorkspaces()
    }

    func loadWorkspaces() async {
        await perform { [self] in
            self.workspaces = try await self.client.listWorkspaces()
        }
    }

    func openWorkspace(_ workspace: WorkspaceResponse) async {
        stopNotificationStream()
        selectedWorkspace = workspace
        showingNotifications = false
        await perform { [self] in
            async let membersTask = self.client.listMembers(workspaceId: workspace.id)
            async let channelsTask = self.client.listChannels(workspaceId: workspace.id)
            let loadedMembers = try await membersTask
            let loadedChannels = try await channelsTask
            self.members = loadedMembers
            self.channels = loadedChannels
            self.screen = .memberSetup
        }
    }

    func createWorkspace() async {
        let request = CreateWorkspaceRequest(
            slug: workspaceSlug.trimmingCharacters(in: .whitespacesAndNewlines),
            name: workspaceName.trimmingCharacters(in: .whitespacesAndNewlines),
            ownerUserId: ownerUserId.trimmingCharacters(in: .whitespacesAndNewlines),
            ownerDisplayName: ownerDisplayName.trimmingCharacters(in: .whitespacesAndNewlines)
        )
        await perform { [self] in
            let workspace = try await self.client.createWorkspace(request)
            self.selectedWorkspace = workspace
            self.workspaces = try await self.client.listWorkspaces()
            self.members = try await self.client.listMembers(workspaceId: workspace.id)
            self.channels = try await self.client.listChannels(workspaceId: workspace.id)
            if let owner = self.members.first(where: { $0.id == workspace.ownerMemberId }) {
                self.selectedMember = owner
            }
            self.screen = .workspace
            try await self.enterInitialChannel()
            try await self.refreshNotifications(unreadOnly: true)
            self.startNotificationStreamIfNeeded()
        }
    }

    func joinSelectedMember(_ member: WorkspaceMemberResponse) async {
        selectedMember = member
        screen = .workspace
        await perform { [self] in
            try await self.enterInitialChannel()
            try await self.refreshNotifications(unreadOnly: true)
            self.startNotificationStreamIfNeeded()
        }
    }

    func createMember() async {
        guard let workspace = selectedWorkspace else { return }
        let request = AddWorkspaceMemberRequest(
            userId: memberUserId.trimmingCharacters(in: .whitespacesAndNewlines),
            displayName: memberDisplayName.trimmingCharacters(in: .whitespacesAndNewlines)
        )
        await perform { [self] in
            let member = try await self.client.addMember(workspaceId: workspace.id, requestBody: request)
            self.members = try await self.client.listMembers(workspaceId: workspace.id)
            self.selectedMember = member
            self.screen = .workspace
            try await self.enterInitialChannel()
            try await self.refreshNotifications(unreadOnly: true)
            self.startNotificationStreamIfNeeded()
        }
    }

    func selectChannel(_ channel: ChannelResponse) async {
        selectedChannel = channel
        showingNotifications = false
        composerText = ""
        composerPreview = nil
        await perform { [self] in
            self.messages = try await self.client.listMessages(channelId: channel.id, limit: 50)
            self.hasOlderMessages = self.messages.count >= 50
            try await self.connectWebSocket(channelId: channel.id)
            try await self.refreshNotifications(unreadOnly: true)
            try await self.markChannelNotificationsRead(channelId: channel.id)
        }
    }

    func createChannel() async {
        guard let workspace = selectedWorkspace, let member = selectedMember else { return }
        let request = CreateChannelRequest(
            slug: newChannelSlug.trimmingCharacters(in: .whitespacesAndNewlines),
            name: newChannelName.trimmingCharacters(in: .whitespacesAndNewlines),
            topic: {
                let topic = newChannelTopic.trimmingCharacters(in: .whitespacesAndNewlines)
                return topic.isEmpty ? nil : topic
            }(),
            visibility: .public,
            createdByMemberId: member.id
        )
        await perform { [self] in
            let channel = try await self.client.createChannel(workspaceId: workspace.id, requestBody: request)
            self.channels = try await self.client.listChannels(workspaceId: workspace.id)
            self.newChannelName = ""
            self.newChannelSlug = ""
            self.newChannelTopic = ""
            self.newChannelDialogPresented = false
            await self.selectChannel(channel)
        }
    }

    func loadNotifications() async {
        showingNotifications = true
        await perform { [self] in
            try await self.refreshNotifications(unreadOnly: false)
        }
    }

    func openNotification(_ notification: MentionNotificationResponse) async {
        guard let channel = channels.first(where: { $0.id == notification.channelId }) else { return }
        showingNotifications = false
        await selectChannel(channel)
        if let threadRootMessageId = notification.threadRootMessageId {
            await openThread(messageId: threadRootMessageId)
        } else {
            await openThread(messageId: notification.messageId)
        }
        if let member = selectedMember {
            await perform { [self] in
                try await self.client.markNotificationsRead(memberId: member.id, ids: [notification.id])
                try await self.refreshVisibleNotifications()
            }
        }
    }

    func openThread(messageId: String) async {
        await perform { [self] in
            let thread = try await self.client.getThread(messageId: messageId)
            self.threadRoot = thread.root
            self.threadReplies = thread.replies
            try await self.markThreadNotificationsRead(rootMessageId: thread.root.id)
        }
    }

    func closeThread() {
        threadRoot = nil
        threadReplies = []
        threadComposerText = ""
        threadComposerPreview = nil
    }

    func loadOlderMessages() async {
        guard let channel = selectedChannel, let oldestId = messages.first?.id, !isLoadingOlderMessages else { return }
        isLoadingOlderMessages = true
        do {
            let older = try await client.listMessages(channelId: channel.id, beforeMessageId: oldestId, limit: 50)
            messages = older + messages
            hasOlderMessages = older.count >= 50
        } catch let apiError as ApiErrorResponse {
            errorMessage = apiError.message
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoadingOlderMessages = false
    }

    func sendMessage() async {
        guard let member = selectedMember, !composerText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        let command = ChatCommand(
            type: .postMessage,
            authorMemberId: member.id,
            body: composerText,
            linkPreview: composerPreview,
            parentMessageId: nil,
            messageId: nil,
            emoji: nil,
            limit: nil
        )
        composerText = ""
        composerPreview = nil
        await send(command)
    }

    func sendThreadReply() async {
        guard let member = selectedMember, let root = threadRoot,
              !threadComposerText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        let command = ChatCommand(
            type: .replyMessage,
            authorMemberId: member.id,
            body: threadComposerText,
            linkPreview: threadComposerPreview,
            parentMessageId: root.id,
            messageId: nil,
            emoji: nil,
            limit: nil
        )
        threadComposerText = ""
        threadComposerPreview = nil
        await send(command)
        await openThread(messageId: root.id)
    }

    func toggleReaction(messageId: String, emoji: String) async {
        guard let member = selectedMember else { return }
        let command = ChatCommand(
            type: .toggleReaction,
            authorMemberId: member.id,
            body: nil,
            linkPreview: nil,
            parentMessageId: nil,
            messageId: messageId,
            emoji: emoji,
            limit: nil
        )
        await send(command)
    }

    func updateSelectedMemberDisplayName() async {
        guard let member = selectedMember else { return }
        let draft = memberRenameDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !draft.isEmpty else { return }
        await perform { [self] in
            let updated = try await self.client.updateMember(memberId: member.id, displayName: draft)
            self.selectedMember = updated
            self.memberRenameDraft = updated.displayName
            if let workspace = self.selectedWorkspace {
                self.members = try await self.client.listMembers(workspaceId: workspace.id)
            }
        }
    }

    func dismissToast(_ id: String) {
        toastNotifications.removeAll { $0.id == id }
    }

    func resolveComposerPreview() async {
        composerPreview = await resolvePreview(text: composerText)
    }

    func resolveThreadComposerPreview() async {
        threadComposerPreview = await resolvePreview(text: threadComposerText)
    }

    func markThreadAsReadIfNeeded() async {
        guard let member = selectedMember, let root = threadRoot else { return }
        let ids = notifications.filter { $0.threadRootMessageId == root.id || $0.messageId == root.id }.map(\.id)
        guard !ids.isEmpty else { return }
        await perform { [self] in
            try await self.client.markNotificationsRead(memberId: member.id, ids: ids)
            try await self.refreshVisibleNotifications()
        }
    }

    func signOutWorkspace() {
        disconnectWebSocket()
        stopNotificationStream()
        selectedWorkspace = nil
        selectedMember = nil
        selectedChannel = nil
        messages = []
        channels = []
        members = []
        notifications = []
        unreadNotificationCount = 0
        channelUnreadCounts = [:]
        toastNotifications = []
        hasOlderMessages = false
        isLoadingOlderMessages = false
        seenUnreadNotificationIds = []
        closeThread()
        screen = .landing
    }

    func enterInitialChannel() async throws {
        if channels.isEmpty, let workspace = selectedWorkspace {
            channels = try await client.listChannels(workspaceId: workspace.id)
        }
        if let firstChannel = channels.first {
            await selectChannel(firstChannel)
        }
    }

    func resolvePreview(text: String) async -> LinkPreviewResponse? {
        guard let url = firstURL(in: text) else { return nil }
        return try? await client.resolveLinkPreview(url: url)
    }

    func firstURL(in text: String) -> String? {
        let detector = try? NSDataDetector(types: NSTextCheckingResult.CheckingType.link.rawValue)
        let range = NSRange(text.startIndex..<text.endIndex, in: text)
        return detector?.matches(in: text, range: range).first?.url?.absoluteString
    }

    func refreshClient() {
        client = APIClient(baseURL: Self.normalizeBaseURL(baseURLString))
    }

    func perform(_ work: @escaping () async throws -> Void) async {
        isLoading = true
        errorMessage = nil
        do {
            try await work()
        } catch let apiError as ApiErrorResponse {
            errorMessage = apiError.message
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    private static func normalizeBaseURL(_ raw: String) -> URL {
        if raw.contains("://"), let url = URL(string: raw) {
            return url
        }
        return URL(string: "http://\(raw)") ?? URL(string: "http://localhost:8080")!
    }
}
