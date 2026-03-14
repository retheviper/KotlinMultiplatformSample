import SwiftUI

struct MemberSetupView: View {
    @ObservedObject var store: ChatStore

    var body: some View {
        HStack(spacing: 24) {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Button("Back") {
                        store.screen = .landing
                    }
                    Text(store.selectedWorkspace?.name ?? "Workspace")
                        .font(.title2.bold())
                }

                Text("Select member")
                    .foregroundStyle(.secondary)

                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(store.members) { member in
                            Button {
                                Task { await store.joinSelectedMember(member) }
                            } label: {
                                HStack {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(member.displayName)
                                            .font(.headline)
                                        Text("@\(member.userId)")
                                            .foregroundStyle(.secondary)
                                    }
                                    Spacer()
                                    Text(member.role.rawValue)
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                                .padding()
                                .background(Color(nsColor: .controlBackgroundColor))
                                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .padding(28)
            .background(Color(nsColor: .underPageBackgroundColor))
            .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))

            VStack(alignment: .leading, spacing: 16) {
                Text("Create member")
                    .font(.title2.bold())
                TextField("User id", text: $store.memberUserId)
                    .textFieldStyle(.roundedBorder)
                TextField("Display name", text: $store.memberDisplayName)
                    .textFieldStyle(.roundedBorder)
                Button("Join workspace") {
                    Task { await store.createMember() }
                }
                .buttonStyle(.borderedProminent)
                .disabled(store.memberUserId.isEmpty || store.memberDisplayName.isEmpty)
                Spacer()
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .padding(28)
            .background(Color(nsColor: .underPageBackgroundColor))
            .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        }
        .padding(32)
    }
}
