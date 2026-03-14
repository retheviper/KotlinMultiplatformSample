import Foundation

func mentionCandidates(for text: String, members: [WorkspaceMemberResponse]) -> [WorkspaceMemberResponse] {
    guard let query = currentMentionQuery(in: text) else { return [] }
    return members.filter {
        query.isEmpty ||
        $0.userId.localizedCaseInsensitiveContains(query) ||
        $0.displayName.localizedCaseInsensitiveContains(query)
    }
}

func replaceMentionCandidate(in text: String, with userId: String) -> String {
    guard let range = mentionTokenRange(in: text) else { return text }
    var updated = text
    updated.replaceSubrange(range, with: "@\(userId) ")
    return updated
}

private func currentMentionQuery(in text: String) -> String? {
    guard let range = mentionTokenRange(in: text) else { return nil }
    return String(text[range].dropFirst())
}

private func mentionTokenRange(in text: String) -> Range<String.Index>? {
    let start = text.lastIndex(of: "@")
    guard let start else { return nil }
    let after = text.index(after: start)
    if start > text.startIndex {
        let before = text[text.index(before: start)]
        if !before.isWhitespace {
            return nil
        }
    }
    let suffix = text[after...]
    if let whitespace = suffix.firstIndex(where: { $0.isWhitespace }) {
        return start..<whitespace
    }
    return start..<text.endIndex
}
