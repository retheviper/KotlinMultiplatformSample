import SwiftUI

struct CreateChannelSheet: View {
    @ObservedObject var store: ChatStore
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("Create channel")
                    .font(.title3.bold())
                Spacer()
                Button("Close") {
                    dismiss()
                }
            }

            VStack(alignment: .leading, spacing: 12) {
                TextField("Channel name", text: $store.newChannelName)
                    .textFieldStyle(.roundedBorder)
                    .onChange(of: store.newChannelName) { _, value in
                        if store.newChannelSlug.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                            store.newChannelSlug = value
                                .lowercased()
                                .replacingOccurrences(of: " ", with: "-")
                        }
                    }

                TextField("Channel slug", text: $store.newChannelSlug)
                    .textFieldStyle(.roundedBorder)

                TextField("Description", text: $store.newChannelTopic, axis: .vertical)
                    .textFieldStyle(.roundedBorder)
                    .lineLimit(3...5)
            }

            HStack {
                Spacer()
                Button("Create") {
                    Task { await store.createChannel() }
                }
                .buttonStyle(.borderedProminent)
                .disabled(
                    store.newChannelName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
                    store.newChannelSlug.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                )
            }
        }
        .padding(24)
        .presentationDetents([.medium])
    }
}

struct RenameMemberSheet: View {
    @ObservedObject var store: ChatStore
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Update display name")
                .font(.title3.bold())
            TextField("Display name", text: $store.memberRenameDraft)
                .textFieldStyle(.roundedBorder)
            HStack {
                Spacer()
                Button("Cancel") {
                    dismiss()
                }
                Button("Save") {
                    Task {
                        await store.updateSelectedMemberDisplayName()
                        dismiss()
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(store.memberRenameDraft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
        }
        .padding(24)
        .presentationDetents([.fraction(0.28)])
    }
}

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
                    .frame(width: 300, alignment: .leading)
                    .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
    }
}
