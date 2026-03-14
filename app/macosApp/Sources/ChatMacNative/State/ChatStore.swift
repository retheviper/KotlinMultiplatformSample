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

    private var client: APIClient
    private var webSocket: URLSessionWebSocketTask?
    private var receiveTask: Task<Void, Never>?
    private var connectedChannelId: String?
    private var notificationStreamTask: Task<Void, Never>?
    private var notificationStreamMemberId: String?
    private var seenUnreadNotificationIds = Set<String>()

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

    private func enterInitialChannel() async throws {
        if channels.isEmpty, let workspace = selectedWorkspace {
            channels = try await client.listChannels(workspaceId: workspace.id)
        }
        if let firstChannel = channels.first {
            await selectChannel(firstChannel)
        }
    }

    private func resolvePreview(text: String) async -> LinkPreviewResponse? {
        guard let url = firstURL(in: text) else { return nil }
        return try? await client.resolveLinkPreview(url: url)
    }

    private func firstURL(in text: String) -> String? {
        let detector = try? NSDataDetector(types: NSTextCheckingResult.CheckingType.link.rawValue)
        let range = NSRange(text.startIndex..<text.endIndex, in: text)
        return detector?.matches(in: text, range: range).first?.url?.absoluteString
    }

    private func refreshNotifications(unreadOnly: Bool) async throws {
        guard let member = selectedMember else { return }
        notifications = try await client.listNotifications(memberId: member.id, unreadOnly: unreadOnly)
        let unread = try await client.listNotifications(memberId: member.id, unreadOnly: true)
        let snapshot = deriveNotificationSnapshot(
            unreadNotifications: unread,
            previousSeenUnreadNotificationIds: seenUnreadNotificationIds,
            existingToasts: toastNotifications,
            showingNotifications: showingNotifications,
            selectedChannelId: selectedChannel?.id,
            selectedThreadRootId: threadRoot?.id
        )
        unreadNotificationCount = snapshot.unreadCount
        channelUnreadCounts = snapshot.channelUnreadCounts
        toastNotifications = snapshot.toastNotifications
        let newUnread = unread.filter { !seenUnreadNotificationIds.contains($0.id) && snapshot.toastNotifications.contains($0) }
        if !newUnread.isEmpty {
            newUnread.forEach { notification in
                NotificationManager.shared.show(notification: notification)
                Task { [weak self] in
                    try? await Task.sleep(nanoseconds: 4_000_000_000)
                    await MainActor.run {
                        self?.dismissToast(notification.id)
                    }
                }
            }
        }
        seenUnreadNotificationIds = snapshot.seenUnreadNotificationIds
        NotificationManager.shared.updateBadge(count: unreadNotificationCount)
    }

    private func refreshClient() {
        client = APIClient(baseURL: Self.normalizeBaseURL(baseURLString))
    }

    private func startNotificationStreamIfNeeded() {
        guard screen == .workspace, let member = selectedMember else {
            stopNotificationStream()
            return
        }
        if notificationStreamMemberId == member.id, notificationStreamTask != nil {
            return
        }

        stopNotificationStream()

        do {
            let request = try client.notificationStreamRequest(memberId: member.id)
            notificationStreamMemberId = member.id
            notificationStreamTask = Task { [weak self] in
                do {
                    try await Self.consumeNotificationStream(request: request) { [weak self] in
                        guard let self else { return }
                        await self.handleNotificationSignal()
                    }
                } catch is CancellationError {
                    // Ignore cancellation while switching sessions or screens.
                } catch {
                    guard let self else { return }
                    await self.handleNotificationStreamFailure(error)
                }
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func stopNotificationStream() {
        notificationStreamTask?.cancel()
        notificationStreamTask = nil
        notificationStreamMemberId = nil
    }

    private func handleNotificationSignal() async {
        do {
            try await refreshVisibleNotifications()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func handleNotificationStreamFailure(_ error: Error) async {
        _ = error
        if Task.isCancelled {
            return
        }
        notificationStreamTask = nil
        notificationStreamMemberId = nil
        do {
            try await refreshVisibleNotifications()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func refreshVisibleNotifications() async throws {
        try await refreshNotifications(unreadOnly: !showingNotifications)
    }

    private func perform(_ work: @escaping () async throws -> Void) async {
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

    private func connectWebSocket(channelId: String) async throws {
        disconnectWebSocket()
        let socket = URLSession.shared.webSocketTask(with: client.webSocketURL(channelId: channelId))
        socket.resume()
        webSocket = socket
        connectedChannelId = channelId
        receiveTask = Task { [weak self] in
            await self?.receiveLoop(socket: socket)
        }
        try await sendRaw(ChatCommand(type: .loadRecent, authorMemberId: nil, body: nil, linkPreview: nil, parentMessageId: nil, messageId: nil, emoji: nil, limit: 50))
    }

    private func disconnectWebSocket() {
        receiveTask?.cancel()
        receiveTask = nil
        webSocket?.cancel(with: .goingAway, reason: nil)
        webSocket = nil
        connectedChannelId = nil
    }

    private func send(_ command: ChatCommand) async {
        do {
            try await ensureSocketConnectedForSelectedChannel()
            try await sendRaw(command)
        } catch {
            do {
                try await reconnectSelectedChannelSocket()
                try await sendRaw(command)
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }

    private func sendRaw(_ command: ChatCommand) async throws {
        guard let webSocket else {
            throw ApiErrorResponse(code: "socket_not_connected", message: "Socket is not connected")
        }
        let payload = String(decoding: try JSONEncoder().encode(command), as: UTF8.self)
        try await webSocket.send(.string(payload))
    }

    private func ensureSocketConnectedForSelectedChannel() async throws {
        guard let channel = selectedChannel else { return }
        if webSocket == nil || connectedChannelId != channel.id {
            try await connectWebSocket(channelId: channel.id)
        }
    }

    private func reconnectSelectedChannelSocket() async throws {
        guard let channel = selectedChannel else {
            throw ApiErrorResponse(code: "socket_not_connected", message: "Socket is not connected")
        }
        try await connectWebSocket(channelId: channel.id)
    }

    private func receiveLoop(socket: URLSessionWebSocketTask) async {
        while !Task.isCancelled {
            do {
                let message = try await socket.receive()
                let data: Data
                switch message {
                case .data(let payload):
                    data = payload
                case .string(let text):
                    data = Data(text.utf8)
                @unknown default:
                    continue
                }
                let event = try JSONDecoder().decode(ChatEvent.self, from: data)
                apply(event: event)
                if selectedMember != nil {
                    try? await refreshVisibleNotifications()
                }
            } catch {
                if !Task.isCancelled {
                    if webSocket === socket {
                        webSocket = nil
                        connectedChannelId = nil
                    }
                    errorMessage = error.localizedDescription
                }
                break
            }
        }
    }

    private func apply(event: ChatEvent) {
        switch event.type {
        case .snapshot:
            messages = event.messages
        case .messagePosted:
            guard let message = event.message else { return }
            messages.append(message)
        case .replyPosted:
            guard let message = event.message else { return }
            if threadRoot?.id == message.threadRootMessageId {
                threadReplies.append(message)
            }
            if let rootId = message.threadRootMessageId,
               let index = messages.firstIndex(where: { $0.id == rootId }) {
                let root = messages[index]
                messages[index] = MessageResponse(
                    id: root.id,
                    channelId: root.channelId,
                    authorMemberId: root.authorMemberId,
                    authorDisplayName: root.authorDisplayName,
                    body: root.body,
                    linkPreview: root.linkPreview,
                    threadRootMessageId: root.threadRootMessageId,
                    threadReplyCount: root.threadReplyCount + 1,
                    reactions: root.reactions,
                    createdAt: root.createdAt
                )
            }
        case .reactionUpdated:
            guard let updated = event.message else { return }
            if let index = messages.firstIndex(where: { $0.id == updated.id }) {
                messages[index] = updated
            }
            if threadRoot?.id == updated.id {
                threadRoot = updated
            }
            if let index = threadReplies.firstIndex(where: { $0.id == updated.id }) {
                threadReplies[index] = updated
            }
        case .error:
            errorMessage = event.error?.message ?? "WebSocket error"
        }
    }

    private static func normalizeBaseURL(_ raw: String) -> URL {
        if raw.contains("://"), let url = URL(string: raw) {
            return url
        }
        return URL(string: "http://\(raw)") ?? URL(string: "http://localhost:8080")!
    }

    private func markChannelNotificationsRead(channelId: String) async throws {
        guard let member = selectedMember else { return }
        let ids = notifications.filter { $0.channelId == channelId && $0.threadRootMessageId == nil }.map(\.id)
        guard !ids.isEmpty else { return }
        try await client.markNotificationsRead(memberId: member.id, ids: ids)
        try await refreshVisibleNotifications()
    }

    private func markThreadNotificationsRead(rootMessageId: String) async throws {
        guard let member = selectedMember else { return }
        let ids = notifications.filter {
            $0.threadRootMessageId == rootMessageId || $0.messageId == rootMessageId
        }.map(\.id)
        guard !ids.isEmpty else { return }
        try await client.markNotificationsRead(memberId: member.id, ids: ids)
        try await refreshVisibleNotifications()
    }

    private nonisolated static func consumeNotificationStream(
        request: URLRequest,
        onSignal: @escaping @Sendable () async -> Void
    ) async throws {
        let (bytes, response) = try await URLSession.shared.bytes(for: request)
        let statusCode = (response as? HTTPURLResponse)?.statusCode ?? 500
        guard (200..<300).contains(statusCode) else {
            throw ApiErrorResponse(code: "http_\(statusCode)", message: "Notification stream request failed.")
        }

        var initialized = false
        for try await line in bytes.lines {
            if Task.isCancelled {
                break
            }
            switch notificationStreamLineResult(for: line, initialized: initialized) {
            case .ignore:
                continue
            case .initializeOnly:
                initialized = true
                continue
            case .signal:
                initialized = true
                await onSignal()
            }
        }
    }
}
