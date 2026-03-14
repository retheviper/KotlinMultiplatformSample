import AppKit
import Foundation
import UserNotifications

@MainActor
final class NotificationManager: NSObject {
    static let shared = NotificationManager()

    private var authorized = false
    private let foregroundDelegate = NotificationForegroundDelegate()
    private lazy var notificationCenter: UNUserNotificationCenter? = {
        guard Self.supportsSystemNotifications else { return nil }
        let center = UNUserNotificationCenter.current()
        center.delegate = foregroundDelegate
        return center
    }()

    private override init() {
        super.init()
    }

    func requestAuthorizationIfNeeded() {
        guard !authorized, let notificationCenter else { return }
        authorized = true
        notificationCenter.requestAuthorization(options: [.alert, .badge, .sound]) { _, _ in
        }
    }

    func show(notification: MentionNotificationResponse) {
        if let notificationCenter {
            let content = UNMutableNotificationContent()
            content.title = notification.kind == .mention ? "Mention" : "Thread activity"
            content.body = "\(notification.authorDisplayName): \(notification.messagePreview)"
            let request = UNNotificationRequest(
                identifier: notification.id,
                content: content,
                trigger: nil
            )
            notificationCenter.add(request)
            return
        }

        showFallbackNotification(notification: notification)
    }

    func updateBadge(count: Int) {
        NSApp.dockTile.badgeLabel = count > 0 ? String(count) : nil
    }

    private func showFallbackNotification(notification: MentionNotificationResponse) {
        let process = Process()
        process.executableURL = URL(fileURLWithPath: "/usr/bin/osascript")
        process.arguments = [
            "-e",
            "display notification \(appleScriptString("\(notification.authorDisplayName): \(notification.messagePreview)")) with title \(appleScriptString(notification.kind == .mention ? "Mention" : "Thread activity"))"
        ]
        try? process.run()
    }

    private func appleScriptString(_ value: String) -> String {
        let escaped = value
            .replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: "\"", with: "\\\"")
        return "\"\(escaped)\""
    }

    private static var supportsSystemNotifications: Bool {
        Bundle.main.bundleURL.pathExtension == "app"
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
