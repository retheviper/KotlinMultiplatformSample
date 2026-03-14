import SwiftUI

struct LandingView: View {
    @ObservedObject var store: ChatStore

    var body: some View {
        HStack(spacing: 24) {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Text("Join workspace")
                        .font(.title2.bold())
                    Spacer()
                    Button("Reload") {
                        Task { await store.loadWorkspaces() }
                    }
                }

                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(store.workspaces) { workspace in
                            Button {
                                Task { await store.openWorkspace(workspace) }
                            } label: {
                                HStack {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(workspace.name)
                                            .font(.headline)
                                        Text(workspace.slug)
                                            .foregroundStyle(.secondary)
                                    }
                                    Spacer()
                                    Text("Open")
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
                Text("Create workspace")
                    .font(.title2.bold())
                Group {
                    TextField("Slug", text: $store.workspaceSlug)
                    TextField("Workspace name", text: $store.workspaceName)
                    TextField("Owner user id", text: $store.ownerUserId)
                    TextField("Owner display name", text: $store.ownerDisplayName)
                }
                .textFieldStyle(.roundedBorder)

                Button("Create") {
                    Task { await store.createWorkspace() }
                }
                .buttonStyle(.borderedProminent)
                .disabled(store.workspaceSlug.isEmpty || store.workspaceName.isEmpty || store.ownerUserId.isEmpty || store.ownerDisplayName.isEmpty)

                Spacer()
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .padding(28)
            .background(Color(nsColor: .underPageBackgroundColor))
            .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        }
        .padding(32)
        .overlay(alignment: .topTrailing) {
            ServerChip(baseURLString: $store.baseURLString)
                .padding(32)
        }
    }
}
