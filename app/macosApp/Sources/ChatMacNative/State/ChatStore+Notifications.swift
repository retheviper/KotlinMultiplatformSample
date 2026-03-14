import Foundation

@MainActor
extension ChatStore {
    func refreshNotifications(unreadOnly: Bool) async throws {
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

    func startNotificationStreamIfNeeded() {
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
                } catch {
                    guard let self else { return }
                    await self.handleNotificationStreamFailure(error)
                }
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func stopNotificationStream() {
        notificationStreamTask?.cancel()
        notificationStreamTask = nil
        notificationStreamMemberId = nil
    }

    func handleNotificationSignal() async {
        do {
            try await refreshVisibleNotifications()
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func handleNotificationStreamFailure(_ error: Error) async {
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

    func refreshVisibleNotifications() async throws {
        try await refreshNotifications(unreadOnly: !showingNotifications)
    }

    func markChannelNotificationsRead(channelId: String) async throws {
        guard let member = selectedMember else { return }
        let ids = notifications.filter { $0.channelId == channelId && $0.threadRootMessageId == nil }.map(\.id)
        guard !ids.isEmpty else { return }
        try await client.markNotificationsRead(memberId: member.id, ids: ids)
        try await refreshVisibleNotifications()
    }

    func markThreadNotificationsRead(rootMessageId: String) async throws {
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
