import SwiftUI

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
                        MessageCardView(
                            message: root,
                            currentMemberId: store.selectedMember?.id,
                            mentionUserIds: Set(store.members.map(\.userId)),
                            onOpenThread: {},
                            onToggleReaction: { emoji in
                                Task { await store.toggleReaction(messageId: root.id, emoji: emoji) }
                            }
                        )
                    }

                    ForEach(store.threadReplies) { reply in
                        MessageCardView(
                            message: reply,
                            currentMemberId: store.selectedMember?.id,
                            mentionUserIds: Set(store.members.map(\.userId)),
                            onOpenThread: {},
                            onToggleReaction: { emoji in
                                Task { await store.toggleReaction(messageId: reply.id, emoji: emoji) }
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
                    .frame(minHeight: 88)
                    .padding(8)
                    .background(Color(nsColor: .textBackgroundColor))
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
        .background(Color(nsColor: .underPageBackgroundColor))
    }
}
