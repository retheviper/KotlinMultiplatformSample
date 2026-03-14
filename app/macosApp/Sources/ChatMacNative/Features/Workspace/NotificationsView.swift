import SwiftUI

struct NotificationsView: View {
    @ObservedObject var store: ChatStore

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text("Notifications")
                    .font(.title3.bold())
                Spacer()
                Button("Refresh") {
                    Task { await store.loadNotifications() }
                }
            }
            .padding(20)
            Divider()

            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(store.notifications) { notification in
                        Button {
                            Task { await store.openNotification(notification) }
                        } label: {
                            NotificationCard(notification: notification)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(20)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct NotificationCard: View {
    let notification: MentionNotificationResponse

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 6) {
                    HStack(spacing: 8) {
                        Text(notification.kind == .mention ? "Mention" : "Thread activity")
                            .font(.caption.weight(.semibold))
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(
                                Capsule(style: .continuous)
                                    .fill(notification.readAt == nil ? Color.accentColor.opacity(0.18) : Color.white.opacity(0.08))
                            )
                        if notification.readAt == nil {
                            Circle()
                                .fill(Color.accentColor)
                                .frame(width: 8, height: 8)
                        }
                    }
                    Text(notification.authorDisplayName)
                        .font(.headline.weight(.semibold))
                    Text(notification.messagePreview)
                        .font(.body)
                        .foregroundStyle(.primary.opacity(0.82))
                        .lineLimit(3)
                }
                Spacer(minLength: 12)
                Text(notification.createdAt.formattedNotificationTime)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(notification.readAt == nil ? Color.accentColor.opacity(0.10) : Color.white.opacity(0.04))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(notification.readAt == nil ? Color.accentColor.opacity(0.30) : Color.white.opacity(0.06), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .contentShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

private extension String {
    var formattedNotificationTime: String {
        if let instant = ISO8601DateFormatter().date(from: self) {
            return instant.formatted(date: .omitted, time: .shortened)
        }
        return self
    }
}
