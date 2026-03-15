import SwiftUI

struct ServerChip: View {
    @Binding var baseURLString: String

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "network")
            TextField("Server", text: $baseURLString)
                .textFieldStyle(.roundedBorder)
        }
        .padding(12)
        .background(.ultraThinMaterial, in: Capsule())
    }
}

struct MentionSuggestionsView: View {
    let candidates: [WorkspaceMemberResponse]
    let onSelect: (WorkspaceMemberResponse) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            ForEach(candidates.prefix(5)) { member in
                Button {
                    onSelect(member)
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(member.displayName)
                            Text("@\(member.userId)")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                }
                .buttonStyle(.plain)
            }
        }
        .background(Color.platformControlBackground)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

struct LinkifiedText: View {
    let text: String
    let mentionUserIds: Set<String>

    var body: some View {
        Text(attributedText)
    }

    private var attributedText: AttributedString {
        var output = AttributedString(text)
        let nsText = text as NSString
        let detector = try? NSDataDetector(types: NSTextCheckingResult.CheckingType.link.rawValue)
        detector?.matches(in: text, range: NSRange(location: 0, length: nsText.length)).forEach { match in
            guard let range = Range(match.range, in: output), let url = match.url else { return }
            output[range].link = url
            output[range].foregroundColor = .accentColor
            output[range].underlineStyle = .single
        }
        let pattern = "@([A-Za-z0-9._-]+)"
        if let regex = try? NSRegularExpression(pattern: pattern) {
            regex.matches(in: text, range: NSRange(location: 0, length: nsText.length)).forEach { match in
                guard match.numberOfRanges > 1,
                      let fullRange = Range(match.range(at: 0), in: output),
                      let userIdRange = Range(match.range(at: 1), in: text) else { return }
                let userId = String(text[userIdRange])
                guard mentionUserIds.contains(userId) else { return }
                output[fullRange].backgroundColor = .accentColor.opacity(0.18)
                output[fullRange].foregroundColor = .accentColor
            }
        }
        return output
    }
}
