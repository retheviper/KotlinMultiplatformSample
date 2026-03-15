import SwiftUI

struct ChannelView: View {
    @ObservedObject var store: ChatStore

    private var composerMentionCandidates: [WorkspaceMemberResponse] {
        mentionCandidates(for: store.composerText, members: store.members)
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(store.selectedChannel.map { "# \($0.name)" } ?? "Choose a channel")
                        .font(.title3.bold())
                    if let topic = store.selectedChannel?.topic, !topic.isEmpty {
                        Text(topic)
                            .foregroundStyle(.secondary)
                    }
                }
                Spacer()
            }
            .padding(20)

            Divider()

            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 12) {
                        if store.hasOlderMessages {
                            Button(store.isLoadingOlderMessages ? "Loading..." : "Read more") {
                                Task { await store.loadOlderMessages() }
                            }
                            .buttonStyle(.plain)
                            .disabled(store.isLoadingOlderMessages)
                        }
                        ForEach(store.messages) { message in
                            MessageCardView(
                                message: message,
                                currentMemberId: store.selectedMember?.id,
                                mentionUserIds: Set(store.members.map(\.userId)),
                                allowsThreadOpenTap: true,
                                usesInlineReactionPicker: false,
                                onOpenThread: {
                                    Task {
                                        await store.openThread(messageId: message.id)
                                        await store.markThreadAsReadIfNeeded()
                                    }
                                },
                                onToggleReaction: { emoji in
                                    Task { await store.toggleReaction(messageId: message.id, emoji: emoji) }
                                }
                            )
                            .id(message.id)
                        }
                    }
                    .padding(20)
                }
                .onChange(of: store.messages.last?.id) { _, latestId in
                    guard let latestId else { return }
                    withAnimation {
                        proxy.scrollTo(latestId, anchor: .bottom)
                    }
                }
            }

            Divider()

            VStack(spacing: 10) {
                if !composerMentionCandidates.isEmpty {
                    MentionSuggestionsView(candidates: composerMentionCandidates) { member in
                        store.composerText = replaceMentionCandidate(in: store.composerText, with: member.userId)
                    }
                }
                if let preview = store.composerPreview {
                    NativeLinkPreviewCard(preview: preview) {
                        store.composerPreview = nil
                    }
                }
                TextEditor(text: $store.composerText)
                    .font(.body)
                    .frame(minHeight: 58, maxHeight: 104)
                    .padding(8)
                    .background(Color.platformTextBackground)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .onChange(of: store.composerText) { _, _ in
                        Task { await store.resolveComposerPreview() }
                    }
                    .onTapGesture {
                        store.closeThread()
                    }

                HStack {
                    Spacer()
                    Button("Send") {
                        Task { await store.sendMessage() }
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(store.selectedChannel == nil || store.selectedMember == nil || store.composerText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
            .padding(20)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct ThreadView: View {
    @ObservedObject var store: ChatStore

    private var threadMentionCandidates: [WorkspaceMemberResponse] {
        mentionCandidates(for: store.threadComposerText, members: store.members)
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text("Thread")
                    .font(.title3.bold())
                Spacer()
                Button {
                    store.closeThread()
                } label: {
                    Image(systemName: "xmark")
                }
                .buttonStyle(.plain)
            }
            .padding(20)

            Divider()

            ScrollView {
                VStack(spacing: 12) {
                    if let root = store.threadRoot {
                        ThreadMessageView(
                            message: root,
                            currentMemberId: store.selectedMember?.id,
                            mentionUserIds: Set(store.members.map(\.userId)),
                            onToggleReaction: { emoji in
                                Task { await store.toggleReactionInOpenThread(messageId: root.id, emoji: emoji) }
                            }
                        )
                    }

                    ForEach(store.threadReplies) { reply in
                        ThreadMessageView(
                            message: reply,
                            currentMemberId: store.selectedMember?.id,
                            mentionUserIds: Set(store.members.map(\.userId)),
                            onToggleReaction: { emoji in
                                Task { await store.toggleReactionInOpenThread(messageId: reply.id, emoji: emoji) }
                            }
                        )
                        .padding(.leading, 18)
                    }
                }
                .padding(20)
            }

            Divider()

            VStack(spacing: 10) {
                if !threadMentionCandidates.isEmpty {
                    MentionSuggestionsView(candidates: threadMentionCandidates) { member in
                        store.threadComposerText = replaceMentionCandidate(in: store.threadComposerText, with: member.userId)
                    }
                }
                if let preview = store.threadComposerPreview {
                    NativeLinkPreviewCard(preview: preview) {
                        store.threadComposerPreview = nil
                    }
                }
                TextEditor(text: $store.threadComposerText)
                    .frame(minHeight: 58, maxHeight: 104)
                    .padding(8)
                    .background(Color.platformTextBackground)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .onChange(of: store.threadComposerText) { _, _ in
                        Task { await store.resolveThreadComposerPreview() }
                    }
                HStack {
                    Spacer()
                    Button("Reply") {
                        Task { await store.sendThreadReply() }
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(store.threadRoot == nil || store.threadComposerText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
            .padding(20)
        }
        .background(Color.platformPanelBackground)
    }
}

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
