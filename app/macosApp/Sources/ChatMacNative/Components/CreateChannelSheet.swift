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
        .frame(minWidth: 420)
    }
}
