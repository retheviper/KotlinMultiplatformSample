import SwiftUI

struct ServerChip: View {
    @Binding var baseURLString: String

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "network")
            TextField("Server", text: $baseURLString)
                .textFieldStyle(.roundedBorder)
                .frame(width: 260)
        }
        .padding(12)
        .background(.ultraThinMaterial, in: Capsule())
    }
}
