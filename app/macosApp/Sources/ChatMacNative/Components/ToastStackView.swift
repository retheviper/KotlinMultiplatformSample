import SwiftUI

struct ToastStackView: View {
    @ObservedObject var store: ChatStore

    var body: some View {
        VStack(alignment: .trailing, spacing: 10) {
            ForEach(store.toastNotifications.prefix(3)) { notification in
                Button {
                    Task { await store.openNotification(notification) }
                } label: {
                    HStack(alignment: .top, spacing: 12) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(notification.kind == .mention ? "Mention" : "Thread activity")
                                .font(.headline)
                            Text(notification.authorDisplayName)
                                .font(.subheadline.bold())
                            Text(notification.messagePreview)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                                .lineLimit(2)
                        }
                        Button("x") {
                            store.dismissToast(notification.id)
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(14)
                    .frame(width: 320, alignment: .leading)
                    .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
    }
}
