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
                    .background(Color(nsColor: .textBackgroundColor))
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
