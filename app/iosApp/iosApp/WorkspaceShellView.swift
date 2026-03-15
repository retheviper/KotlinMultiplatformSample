import SwiftUI

struct WorkspaceShellView: View {
    @ObservedObject var store: ChatStore
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @State private var showingRenameDialog = false
    @State private var showingSidebar = false

    var body: some View {
        Group {
            if horizontalSizeClass == .compact {
                compactLayout
            } else {
                regularLayout
            }
        }
        .animation(.easeInOut(duration: 0.2), value: store.threadRoot?.id)
        .sheet(isPresented: $showingRenameDialog) {
            RenameMemberSheet(store: store)
        }
        .sheet(isPresented: $store.newChannelDialogPresented) {
            CreateChannelSheet(store: store)
        }
        .onChange(of: store.selectedChannel?.id) { _, _ in
            showingSidebar = false
        }
    }

    private var regularLayout: some View {
        HStack(spacing: 0) {
            sidebar(showNotificationsShortcut: true)
                .frame(width: 280)
            Divider()
            ZStack(alignment: .trailing) {
                mainContent
                if store.threadRoot != nil {
                    ThreadView(store: store)
                        .frame(width: 360)
                        .background(Color.platformWindowBackground)
                        .transition(.move(edge: .trailing).combined(with: .opacity))
                        .shadow(color: .black.opacity(0.12), radius: 18, x: -4, y: 0)
                }
            }
        }
    }

    private var compactLayout: some View {
        NavigationStack {
            ZStack {
                mainContent
                if showingSidebar {
                    compactSidebarDrawer
                        .transition(.move(edge: .leading).combined(with: .opacity))
                        .zIndex(1)
                }
            }
            .safeAreaInset(edge: .top) {
                if !showingSidebar && store.threadRoot == nil {
                    compactTopBar
                }
            }
            .navigationDestination(isPresented: Binding(
                get: { store.threadRoot != nil },
                set: { if !$0 { store.closeThread() } }
            )) {
                ThreadView(store: store)
                    .toolbar(.hidden, for: .navigationBar)
            }
        }
    }

    private var compactSidebarDrawer: some View {
        GeometryReader { proxy in
            let drawerWidth = min(352, proxy.size.width * 0.84)

            HStack(spacing: 0) {
                sidebar(showNotificationsShortcut: false)
                    .frame(width: drawerWidth)
                    .frame(maxHeight: .infinity, alignment: .top)
                    .padding(.top, proxy.safeAreaInsets.top + 16)
                    .padding(.bottom, max(proxy.safeAreaInsets.bottom, 12) + 10)
                    .background(
                        ZStack {
                            Rectangle()
                                .fill(.thickMaterial)
                            LinearGradient(
                                colors: [
                                    Color.white.opacity(0.28),
                                    Color.white.opacity(0.08),
                                    Color.cyan.opacity(0.10)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                            .blendMode(.plusLighter)
                        }
                    )
                    .clipShape(
                        UnevenRoundedRectangle(
                            topLeadingRadius: 0,
                            bottomLeadingRadius: 0,
                            bottomTrailingRadius: 32,
                            topTrailingRadius: 32,
                            style: .continuous
                        )
                    )
                    .overlay(alignment: .trailing) {
                        Rectangle()
                            .fill(Color.white.opacity(0.22))
                            .frame(width: 1)
                    }
                    .shadow(color: .black.opacity(0.10), radius: 26, x: 10, y: 0)
                    .ignoresSafeArea(edges: .vertical)

                Button {
                    showingSidebar = false
                } label: {
                    Rectangle()
                        .fill(Color.black.opacity(0.10))
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
                .buttonStyle(.plain)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        }
    }

    private var compactTopBar: some View {
        HStack(spacing: 12) {
            Button {
                showingSidebar = true
            } label: {
                Image(systemName: "sidebar.left")
                    .font(.title3.weight(.semibold))
                    .frame(width: 42, height: 42)
            }
            .buttonStyle(.plain)
            .background(.regularMaterial, in: Circle())

            VStack(alignment: .leading, spacing: 2) {
                Text(store.selectedWorkspace?.name ?? "Workspace")
                    .font(.headline.weight(.semibold))
                Text(store.showingNotifications ? "Notifications" : (store.selectedChannel?.name ?? "Choose a channel"))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            Button {
                Task { await store.loadNotifications() }
            } label: {
                ZStack(alignment: .topTrailing) {
                    Image(systemName: "bell")
                        .font(.title3.weight(.semibold))
                        .frame(width: 42, height: 42)
                    if store.unreadNotificationCount > 0 {
                        Text("\(min(store.unreadNotificationCount, 99))")
                            .font(.caption2.bold())
                            .padding(.horizontal, 5)
                            .padding(.vertical, 1)
                            .background(Color.accentColor)
                            .clipShape(Capsule())
                            .offset(x: 10, y: -8)
                    }
                }
            }
            .buttonStyle(.plain)
            .background(.regularMaterial, in: Circle())
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(.bar, in: RoundedRectangle(cornerRadius: 28, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .strokeBorder(Color.white.opacity(0.26), lineWidth: 1)
        )
        .padding(.horizontal, 12)
        .padding(.top, 6)
    }

    private var mainContent: some View {
        Group {
            if store.showingNotifications {
                NotificationsView(store: store)
            } else {
                ChannelView(store: store)
            }
        }
    }

    private func sidebar(showNotificationsShortcut: Bool) -> some View {
        VStack(alignment: .leading, spacing: 20) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(store.selectedWorkspace?.name ?? "Workspace")
                        .font(.title3.bold())
                    Text(store.selectedWorkspace?.slug ?? "")
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Button {
                    store.signOutWorkspace()
                } label: {
                    Image(systemName: "rectangle.portrait.and.arrow.right")
                }
                .buttonStyle(.plain)
            }
            .padding(.bottom, 4)

            if showNotificationsShortcut {
                Button {
                    Task { await store.loadNotifications() }
                } label: {
                    HStack {
                        Text("Notifications")
                        Spacer()
                        if store.unreadNotificationCount > 0 {
                            Text("\(store.unreadNotificationCount)")
                                .font(.caption.bold())
                                .padding(.horizontal, 8)
                                .padding(.vertical, 3)
                                .background(Color.accentColor.opacity(0.18))
                                .clipShape(Capsule())
                        }
                    }
                    .padding(12)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(store.showingNotifications ? Color.accentColor.opacity(0.14) : Color.clear)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .buttonStyle(.plain)
            }

            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text("Channels")
                        .font(.headline)
                    Spacer()
                    Button {
                        store.newChannelDialogPresented = true
                    } label: {
                        Image(systemName: "plus")
                    }
                    .buttonStyle(.plain)
                }

                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 8) {
                        ForEach(store.channels) { channel in
                            Button {
                                Task { await store.selectChannel(channel) }
                            } label: {
                                HStack {
                                    Text("# \(channel.name)")
                                    Spacer()
                                    if let unreadCount = store.channelUnreadCounts[channel.id], unreadCount > 0 {
                                        Text("\(unreadCount)")
                                            .font(.caption.bold())
                                            .padding(.horizontal, 8)
                                            .padding(.vertical, 3)
                                            .background(Color.accentColor.opacity(0.18))
                                            .clipShape(Capsule())
                                    }
                                }
                                .padding(10)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(store.selectedChannel?.id == channel.id && !store.showingNotifications ? Color.accentColor.opacity(0.14) : Color.clear)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
            }

            Spacer()

            if let member = store.selectedMember {
                VStack(alignment: .leading, spacing: 4) {
                    Text(member.displayName)
                        .font(.headline)
                    Text("@\(member.userId)")
                        .foregroundStyle(.secondary)
                }
                .padding(14)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.platformControlBackground)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .onTapGesture {
                    store.memberRenameDraft = member.displayName
                    showingRenameDialog = true
                }
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 18)
    }
}
