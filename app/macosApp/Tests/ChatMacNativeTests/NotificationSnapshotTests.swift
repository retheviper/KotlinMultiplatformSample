import XCTest
@testable import KMPs

final class NotificationSnapshotTests: XCTestCase {
    func testAggregatesUnreadCountsAndChannelBadges() {
        let unread = [
            makeNotification(id: "n1", channelId: "c1", threadRootMessageId: nil),
            makeNotification(id: "n2", channelId: "c1", threadRootMessageId: "m1"),
            makeNotification(id: "n3", channelId: "c2", threadRootMessageId: nil)
        ]

        let snapshot = deriveNotificationSnapshot(
            unreadNotifications: unread,
            previousSeenUnreadNotificationIds: [],
            existingToasts: [],
            showingNotifications: false,
            selectedChannelId: nil,
            selectedThreadRootId: nil
        )

        XCTAssertEqual(snapshot.unreadCount, 3)
        XCTAssertEqual(snapshot.channelUnreadCounts["c1"], 2)
        XCTAssertEqual(snapshot.channelUnreadCounts["c2"], 1)
        XCTAssertEqual(snapshot.toastNotifications.map(\.id), ["n3", "n2", "n1"])
    }

    func testSuppressesChannelToastWhenAlreadyViewingRootChannel() {
        let unread = [
            makeNotification(id: "n1", channelId: "c1", threadRootMessageId: nil)
        ]

        let snapshot = deriveNotificationSnapshot(
            unreadNotifications: unread,
            previousSeenUnreadNotificationIds: [],
            existingToasts: [],
            showingNotifications: false,
            selectedChannelId: "c1",
            selectedThreadRootId: nil
        )

        XCTAssertTrue(snapshot.toastNotifications.isEmpty)
    }

    func testSuppressesThreadToastWhenViewingSameThread() {
        let unread = [
            makeNotification(id: "n1", channelId: "c1", threadRootMessageId: "root-1")
        ]

        let snapshot = deriveNotificationSnapshot(
            unreadNotifications: unread,
            previousSeenUnreadNotificationIds: [],
            existingToasts: [],
            showingNotifications: false,
            selectedChannelId: "c1",
            selectedThreadRootId: "root-1"
        )

        XCTAssertTrue(snapshot.toastNotifications.isEmpty)
    }

    func testDropsStaleToastWhenNotificationIsNoLongerUnread() {
        let existingToast = makeNotification(id: "stale", channelId: "c1", threadRootMessageId: nil)

        let snapshot = deriveNotificationSnapshot(
            unreadNotifications: [],
            previousSeenUnreadNotificationIds: ["stale"],
            existingToasts: [existingToast],
            showingNotifications: false,
            selectedChannelId: nil,
            selectedThreadRootId: nil
        )

        XCTAssertTrue(snapshot.toastNotifications.isEmpty)
    }

    func testSuppressesToastsWhileNotificationsScreenIsVisible() {
        let unread = [
            makeNotification(id: "n1", channelId: "c1", threadRootMessageId: nil)
        ]

        let snapshot = deriveNotificationSnapshot(
            unreadNotifications: unread,
            previousSeenUnreadNotificationIds: [],
            existingToasts: [],
            showingNotifications: true,
            selectedChannelId: nil,
            selectedThreadRootId: nil
        )

        XCTAssertTrue(snapshot.toastNotifications.isEmpty)
    }

    func testKeepsThreadToastWhenViewingDifferentThreadInSameChannel() {
        let unread = [
            makeNotification(id: "n1", channelId: "c1", threadRootMessageId: "root-1")
        ]

        let snapshot = deriveNotificationSnapshot(
            unreadNotifications: unread,
            previousSeenUnreadNotificationIds: [],
            existingToasts: [],
            showingNotifications: false,
            selectedChannelId: "c1",
            selectedThreadRootId: "other-root"
        )

        XCTAssertEqual(snapshot.toastNotifications.map(\.id), ["n1"])
    }

    func testPrependsNewToastsAheadOfExistingUnreadToasts() {
        let existingToast = makeNotification(id: "older", channelId: "c1", threadRootMessageId: nil)
        let unread = [
            existingToast,
            makeNotification(id: "newer", channelId: "c2", threadRootMessageId: nil)
        ]

        let snapshot = deriveNotificationSnapshot(
            unreadNotifications: unread,
            previousSeenUnreadNotificationIds: ["older"],
            existingToasts: [existingToast],
            showingNotifications: false,
            selectedChannelId: nil,
            selectedThreadRootId: nil
        )

        XCTAssertEqual(snapshot.toastNotifications.map(\.id), ["newer", "older"])
    }

    private func makeNotification(id: String, channelId: String, threadRootMessageId: String?) -> MentionNotificationResponse {
        MentionNotificationResponse(
            id: id,
            kind: .mention,
            memberId: "member-1",
            channelId: channelId,
            messageId: "message-\(id)",
            threadRootMessageId: threadRootMessageId,
            authorDisplayName: "Alice",
            messagePreview: "hello",
            createdAt: "2026-03-14T00:00:00Z",
            readAt: nil
        )
    }
}
