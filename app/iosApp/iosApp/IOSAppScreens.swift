import SwiftUI

struct IOSRootView: View {
    @StateObject private var store = ChatStore(
        baseURLString: ProcessInfo.processInfo.environment["MESSAGING_BASE_URL"] ?? "http://localhost:8080"
    )

    var body: some View {
        Group {
            switch store.screen {
            case .landing:
                LandingView(store: store)
            case .memberSetup:
                MemberSetupView(store: store)
            case .workspace:
                WorkspaceShellView(store: store)
            }
        }
        .background {
            ZStack {
                Color.platformWindowBackground
                LinearGradient(
                    colors: [
                        Color.accentColor.opacity(0.10),
                        Color.clear,
                        Color.cyan.opacity(0.06)
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            }
            .ignoresSafeArea()
        }
        .task {
            NotificationManager.shared.requestAuthorizationIfNeeded()
            await store.connectServer()
        }
        .overlay(alignment: .topTrailing) {
            ToastStackView(store: store)
                .padding()
        }
        .alert("Error", isPresented: Binding(
            get: { store.errorMessage != nil },
            set: { if !$0 { store.errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) {
                store.errorMessage = nil
            }
        } message: {
            Text(store.errorMessage ?? "")
        }
    }
}

struct LandingView: View {
    @ObservedObject var store: ChatStore
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    var body: some View {
        Group {
            if horizontalSizeClass == .compact {
                VStack(spacing: 16) {
                    joinWorkspacePanel
                    createWorkspacePanel
                }
            } else {
                HStack(spacing: 24) {
                    joinWorkspacePanel
                    createWorkspacePanel
                }
            }
        }
        .padding()
        .overlay(alignment: .topTrailing) {
            ServerChip(baseURLString: $store.baseURLString)
                .padding()
        }
    }

    private var joinWorkspacePanel: some View {
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
                            .background(Color.platformControlBackground)
                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .padding(24)
        .background(Color.platformPanelBackground)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
    }

    private var createWorkspacePanel: some View {
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
        .padding(24)
        .background(Color.platformPanelBackground)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
    }
}

struct MemberSetupView: View {
    @ObservedObject var store: ChatStore
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    var body: some View {
        Group {
            if horizontalSizeClass == .compact {
                VStack(spacing: 16) {
                    memberList
                    createMemberPanel
                }
            } else {
                HStack(spacing: 24) {
                    memberList
                    createMemberPanel
                }
            }
        }
        .padding()
    }

    private var memberList: some View {
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
                            .background(Color.platformControlBackground)
                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .padding(24)
        .background(Color.platformPanelBackground)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
    }

    private var createMemberPanel: some View {
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
        .padding(24)
        .background(Color.platformPanelBackground)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
    }
}
