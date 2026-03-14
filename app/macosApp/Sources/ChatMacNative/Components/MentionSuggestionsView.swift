import SwiftUI

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
        .background(Color(nsColor: .controlBackgroundColor))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}
