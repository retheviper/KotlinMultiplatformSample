import SwiftUI

struct MessageCardView: View {
    let message: MessageResponse
    let currentMemberId: String?
    let mentionUserIds: Set<String>
    let onOpenThread: () -> Void
    let onToggleReaction: (String) -> Void

    private let defaultEmojis = ["👍", "❤️", "😂", "🎉", "👀", "🚀"]

    private var isOwnMessage: Bool {
        currentMemberId == message.authorMemberId
    }

    private var cardBackground: Color {
        if isOwnMessage {
            return Color.accentColor.opacity(0.22)
        }
        return Color.white.opacity(0.065)
    }

    private var reactionBackground: Color {
        Color.white.opacity(0.07)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(message.authorDisplayName)
                    .font(.headline)
                Spacer()
                Text(message.createdAt)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            LinkifiedText(text: message.body, mentionUserIds: mentionUserIds)
                .textSelection(.enabled)

            if let preview = message.linkPreview {
                NativeLinkPreviewCard(preview: preview, onDismiss: nil)
            }

            if message.threadReplyCount > 0 {
                Button("\(message.threadReplyCount) replies") {
                    onOpenThread()
                }
                .buttonStyle(.link)
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(message.reactions, id: \.emoji) { reaction in
                        Button {
                            onToggleReaction(reaction.emoji)
                        } label: {
                            Text("\(reaction.emoji) \(reaction.count)")
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(reaction.memberIds.contains(currentMemberId ?? "") ? Color.accentColor.opacity(0.18) : reactionBackground)
                                .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                    }

                    Menu {
                        ForEach(defaultEmojis, id: \.self) { emoji in
                            Button(emoji) {
                                onToggleReaction(emoji)
                            }
                        }
                    } label: {
                        Image(systemName: "face.smiling")
                            .padding(8)
                            .background(reactionBackground)
                            .clipShape(Circle())
                    }
                    .menuStyle(.borderlessButton)
                    .fixedSize()
                }
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(cardBackground)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .onTapGesture {
            onOpenThread()
        }
    }
}
