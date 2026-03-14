import SwiftUI

struct RenameMemberSheet: View {
    @ObservedObject var store: ChatStore
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Update display name")
                .font(.title3.bold())
            TextField("Display name", text: $store.memberRenameDraft)
                .textFieldStyle(.roundedBorder)
            HStack {
                Spacer()
                Button("Cancel") {
                    dismiss()
                }
                Button("Save") {
                    Task {
                        await store.updateSelectedMemberDisplayName()
                        dismiss()
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(store.memberRenameDraft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
        }
        .padding(24)
        .frame(width: 360)
    }
}
