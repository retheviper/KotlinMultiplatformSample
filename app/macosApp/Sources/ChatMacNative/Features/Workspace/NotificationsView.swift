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
                            VStack(alignment: .leading, spacing: 8) {
                                HStack {
                                    Text(notification.kind == .mention ? "Mention" : "Thread activity")
                                        .font(.headline)
                                    Spacer()
                                    if notification.readAt == nil {
                                        Circle()
                                            .fill(Color.accentColor)
                                            .frame(width: 8, height: 8)
                                    }
                                }
                                Text(notification.authorDisplayName)
                                    .font(.subheadline.bold())
                                Text(notification.messagePreview)
                                    .foregroundStyle(.secondary)
                                    .lineLimit(3)
                            }
                            .padding(16)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color(nsColor: .controlBackgroundColor))
                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                            .contentShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
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
