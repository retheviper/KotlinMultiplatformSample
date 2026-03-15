import SwiftUI

private func reactionDisplayLabel(_ value: String) -> String {
    switch value {
    case ":+1:":
        return "👍"
    case ":heart:":
        return "❤️"
    case ":joy:":
        return "😂"
    case ":tada:":
        return "🎉"
    case ":eyes:":
        return "👀"
    case ":rocket:":
        return "🚀"
    default:
        return value
    }
}

private struct EmojiText: View {
    let value: String
    let size: CGFloat

    init(_ value: String, size: CGFloat) {
        self.value = value
        self.size = size
    }

    private var twemojiURL: URL? {
        let codePoints = value.unicodeScalars
            .filter { $0.value != 0xFE0F }
            .map { String($0.value, radix: 16, uppercase: false) }
            .joined(separator: "-")
        return URL(string: "https://cdn.jsdelivr.net/gh/jdecked/twemoji@latest/assets/72x72/\(codePoints).png")
    }

    var body: some View {
        Group {
            #if targetEnvironment(simulator)
            if let twemojiURL {
                AsyncImage(url: twemojiURL) { phase in
                    switch phase {
                    case let .success(image):
                        image
                            .resizable()
                            .interpolation(.high)
                            .scaledToFit()
                    default:
                        Text(value)
                            .font(.system(size: size))
                    }
                }
            } else {
                Text(value)
                    .font(.system(size: size))
            }
            #else
            Text(value)
                .font(.system(size: size))
            #endif
        }
        .frame(width: size, height: size)
    }
}

struct MessageCardView: View {
    let message: MessageResponse
    let currentMemberId: String?
    let mentionUserIds: Set<String>
    let allowsThreadOpenTap: Bool
    let usesInlineReactionPicker: Bool
    let onOpenThread: () -> Void
    let onToggleReaction: (String) -> Void
    @State private var showingReactionPicker = false

    private let defaultEmojis = ["👍", "❤️", "😂", "🎉", "👀", "🚀"]

    private var isOwnMessage: Bool {
        currentMemberId == message.authorMemberId
    }

    private var cardBackground: Color {
        isOwnMessage ? Color.accentColor.opacity(0.22) : Color.white.opacity(0.065)
    }

    @ViewBuilder
    private var reactionControls: some View {
        if usesInlineReactionPicker {
            HStack(spacing: 8) {
                reactionButtons
            }
        } else {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    reactionButtons
                }
            }
        }
    }

    @ViewBuilder
    private var reactionButtons: some View {
        ForEach(message.reactions, id: \.emoji) { reaction in
            Button {
                onToggleReaction(reaction.emoji)
            } label: {
                HStack(spacing: 6) {
                    EmojiText(reactionDisplayLabel(reaction.emoji), size: 20)
                        .frame(width: 20, height: 20)
                    Text("\(reaction.count)")
                        .font(.subheadline.weight(.medium))
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(reaction.memberIds.contains(currentMemberId ?? "") ? Color.accentColor.opacity(0.18) : Color.white.opacity(0.07))
                .clipShape(Capsule())
            }
            .buttonStyle(.plain)
        }

        Button {
            if usesInlineReactionPicker {
                showingReactionPicker.toggle()
            } else {
                showingReactionPicker = true
            }
        } label: {
            Image(systemName: "face.smiling")
                .padding(8)
                .background(Color.white.opacity(0.07))
                .clipShape(Circle())
        }
        .buttonStyle(.plain)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
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
                    .buttonStyle(.borderless)
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .fill(cardBackground.opacity(0.55))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .strokeBorder(Color.white.opacity(0.22), lineWidth: 1)
            )
            .modifier(ThreadOpenTapModifier(isEnabled: allowsThreadOpenTap, action: onOpenThread))

            reactionControls

            if usesInlineReactionPicker && showingReactionPicker {
                InlineReactionPicker(emojis: defaultEmojis) { emoji in
                    showingReactionPicker = false
                    onToggleReaction(emoji)
                }
                .transition(.opacity.combined(with: .scale(scale: 0.96, anchor: .topLeading)))
            }
        }
        .sheet(isPresented: Binding(
            get: { showingReactionPicker && !usesInlineReactionPicker },
            set: { showingReactionPicker = $0 }
        )) {
            ReactionPickerSheet(emojis: defaultEmojis) { emoji in
                showingReactionPicker = false
                onToggleReaction(emoji)
            }
        }
    }
}

struct ThreadMessageView: View {
    let message: MessageResponse
    let currentMemberId: String?
    let mentionUserIds: Set<String>
    let onToggleReaction: (String) -> Void
    @State private var showingReactionPicker = false

    private let defaultEmojis = ["👍", "❤️", "😂", "🎉", "👀", "🚀"]

    private var isOwnMessage: Bool {
        currentMemberId == message.authorMemberId
    }

    private var cardTint: Color {
        isOwnMessage ? Color.accentColor.opacity(0.18) : Color.white.opacity(0.08)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
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

                if let preview = message.linkPreview {
                    NativeLinkPreviewCard(preview: preview, onDismiss: nil)
                }
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .fill(cardTint)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .strokeBorder(Color.white.opacity(0.22), lineWidth: 1)
            )

            ThreadReactionBar(
                reactions: message.reactions,
                currentMemberId: currentMemberId,
                showingPicker: $showingReactionPicker,
                onToggleReaction: onToggleReaction
            )

            if showingReactionPicker {
                InlineReactionPicker(emojis: defaultEmojis) { emoji in
                    showingReactionPicker = false
                    onToggleReaction(emoji)
                }
                .transition(.opacity.combined(with: .scale(scale: 0.96, anchor: .topLeading)))
            }
        }
        .animation(.easeInOut(duration: 0.16), value: showingReactionPicker)
    }
}

private struct ThreadOpenTapModifier: ViewModifier {
    let isEnabled: Bool
    let action: () -> Void

    func body(content: Content) -> some View {
        if isEnabled {
            content
                .contentShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                .onTapGesture(perform: action)
        } else {
            content
        }
    }
}

private struct ThreadReactionBar: View {
    let reactions: [MessageReactionResponse]
    let currentMemberId: String?
    @Binding var showingPicker: Bool
    let onToggleReaction: (String) -> Void

    var body: some View {
        HStack(spacing: 8) {
            ForEach(reactions, id: \.emoji) { reaction in
                Button {
                    onToggleReaction(reaction.emoji)
                } label: {
                    HStack(spacing: 6) {
                        EmojiText(reactionDisplayLabel(reaction.emoji), size: 18)
                            .frame(width: 18, height: 18)
                        Text("\(reaction.count)")
                            .font(.subheadline.weight(.medium))
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(
                        reaction.memberIds.contains(currentMemberId ?? "")
                            ? Color.accentColor.opacity(0.18)
                            : Color.white.opacity(0.07),
                        in: Capsule()
                    )
                }
                .buttonStyle(.plain)
            }

            Button {
                showingPicker.toggle()
            } label: {
                Image(systemName: showingPicker ? "xmark" : "face.smiling")
                    .font(.subheadline.weight(.semibold))
                    .frame(width: 34, height: 34)
                    .background(Color.white.opacity(0.07), in: Circle())
            }
            .buttonStyle(.plain)
        }
        .contentShape(Rectangle())
    }
}

private struct ReactionPickerSheet: View {
    let emojis: [String]
    let onSelect: (String) -> Void
    @Environment(\.dismiss) private var dismiss

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 12), count: 3)

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack {
                Text("Add reaction")
                    .font(.title3.bold())
                Spacer()
                Button("Close") {
                    dismiss()
                }
            }

            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(emojis, id: \.self) { emoji in
                    Button {
                        onSelect(emoji)
                    } label: {
                        EmojiText(reactionDisplayLabel(emoji), size: 34)
                            .frame(maxWidth: .infinity)
                            .frame(height: 72)
                            .background(Color.platformControlBackground)
                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(24)
        .background(.regularMaterial)
        .presentationDetents([.fraction(0.34)])
    }
}

private struct InlineReactionPicker: View {
    let emojis: [String]
    let onSelect: (String) -> Void

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 10), count: 3)

    var body: some View {
        LazyVGrid(columns: columns, spacing: 10) {
            ForEach(emojis, id: \.self) { emoji in
                Button {
                    onSelect(emoji)
                } label: {
                    EmojiText(reactionDisplayLabel(emoji), size: 28)
                        .frame(maxWidth: .infinity)
                        .frame(height: 54)
                        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 16, style: .continuous)
                                .strokeBorder(Color.white.opacity(0.16), lineWidth: 1)
                        )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(12)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(Color.white.opacity(0.18), lineWidth: 1)
        )
    }
}
