import Foundation
import UserNotifications

@MainActor
final class NotificationManager: NSObject {
    static let shared = NotificationManager()

    private var authorized = false
    private let foregroundDelegate = NotificationForegroundDelegate()
    private lazy var notificationCenter: UNUserNotificationCenter = {
        let center = UNUserNotificationCenter.current()
        center.delegate = foregroundDelegate
        return center
    }()

    func requestAuthorizationIfNeeded() {
        guard !authorized else { return }
        authorized = true
        notificationCenter.requestAuthorization(options: [.alert, .badge, .sound]) { _, _ in }
    }

    func show(notification: MentionNotificationResponse) {
        let content = UNMutableNotificationContent()
        content.title = notification.kind == .mention ? "Mention" : "Thread activity"
        content.body = "\(notification.authorDisplayName): \(notification.messagePreview)"
        let request = UNNotificationRequest(identifier: notification.id, content: content, trigger: nil)
        notificationCenter.add(request)
    }

    func updateBadge(count: Int) {
        UNUserNotificationCenter.current().setBadgeCount(count)
    }
}

private final class NotificationForegroundDelegate: NSObject, UNUserNotificationCenterDelegate {
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .badge, .sound])
    }
}
