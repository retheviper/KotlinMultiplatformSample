import Foundation

struct NotificationSnapshot {
    let unreadCount: Int
    let channelUnreadCounts: [String: Int]
    let toastNotifications: [MentionNotificationResponse]
    let seenUnreadNotificationIds: Set<String>
}

func deriveNotificationSnapshot(
    unreadNotifications: [MentionNotificationResponse],
    previousSeenUnreadNotificationIds: Set<String>,
    existingToasts: [MentionNotificationResponse],
    showingNotifications: Bool,
    selectedChannelId: String?,
    selectedThreadRootId: String?
) -> NotificationSnapshot {
    let unreadCount = unreadNotifications.count
    let channelUnreadCounts = Dictionary(unreadNotifications.map { ($0.channelId, 1) }, uniquingKeysWith: +)
    let unreadIds = Set(unreadNotifications.map(\.id))

    let newUnread = unreadNotifications.filter { notification in
        !previousSeenUnreadNotificationIds.contains(notification.id) &&
            !shouldSuppressToast(
                notification: notification,
                showingNotifications: showingNotifications,
                selectedChannelId: selectedChannelId,
                selectedThreadRootId: selectedThreadRootId
            )
    }

    var toastNotifications = existingToasts.filter { unreadIds.contains($0.id) }
    if !newUnread.isEmpty {
        toastNotifications.insert(contentsOf: newUnread.reversed(), at: 0)
    }

    return NotificationSnapshot(
        unreadCount: unreadCount,
        channelUnreadCounts: channelUnreadCounts,
        toastNotifications: toastNotifications,
        seenUnreadNotificationIds: unreadIds
    )
}

func shouldSuppressToast(
    notification: MentionNotificationResponse,
    showingNotifications: Bool,
    selectedChannelId: String?,
    selectedThreadRootId: String?
) -> Bool {
    if showingNotifications {
        return true
    }
    if selectedChannelId != notification.channelId {
        return false
    }
    if let threadRootMessageId = notification.threadRootMessageId {
        return selectedThreadRootId == threadRootMessageId
    }
    return selectedThreadRootId == nil
}
