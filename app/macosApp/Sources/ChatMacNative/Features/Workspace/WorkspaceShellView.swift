import SwiftUI

struct WorkspaceShellView: View {
    @ObservedObject var store: ChatStore
    @State private var showingRenameDialog = false

    var body: some View {
        HStack(spacing: 0) {
            sidebar
                .frame(width: 280)
            Divider()
            ZStack(alignment: .trailing) {
                if store.showingNotifications {
                    NotificationsView(store: store)
                } else {
                    ChannelView(store: store)
                }
                if store.threadRoot != nil {
                    ThreadView(store: store)
                        .frame(width: 360)
                        .background(Color(nsColor: .windowBackgroundColor))
                        .transition(.move(edge: .trailing).combined(with: .opacity))
                        .shadow(color: .black.opacity(0.12), radius: 18, x: -4, y: 0)
                }
            }
        }
        .animation(.easeInOut(duration: 0.2), value: store.threadRoot?.id)
    }

    private var sidebar: some View {
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
                .contentShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            .buttonStyle(.plain)

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
                                .contentShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
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
                .background(Color(nsColor: .controlBackgroundColor))
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .onTapGesture {
                    store.memberRenameDraft = member.displayName
                    showingRenameDialog = true
                }
            }
        }
        .padding(20)
        .background(Color(nsColor: .underPageBackgroundColor))
        .sheet(isPresented: $showingRenameDialog) {
            RenameMemberSheet(store: store)
        }
        .sheet(isPresented: $store.newChannelDialogPresented) {
            CreateChannelSheet(store: store)
        }
    }
}
