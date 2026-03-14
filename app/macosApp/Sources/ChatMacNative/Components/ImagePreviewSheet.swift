import AppKit
import SwiftUI

struct ImagePreviewSheet: View {
    let imageUrl: String
    let title: String?
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text(title ?? "Image")
                    .font(.title3.bold())
                Spacer()
                Button("Close") {
                    dismiss()
                }
            }

            if let url = URL(string: imageUrl) {
                RemotePreviewImage(
                    url: url,
                    contentDescription: title ?? "Image preview",
                    contentMode: .fit
                )
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color(nsColor: .underPageBackgroundColor))
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            } else {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }

            HStack {
                Button("Copy URL") {
                    NSPasteboard.general.clearContents()
                    NSPasteboard.general.setString(imageUrl, forType: .string)
                }
                Button("Open in Browser") {
                    if let url = URL(string: imageUrl) {
                        NSWorkspace.shared.open(url)
                    }
                }
                Button("Download") {
                    downloadImage()
                }
                .buttonStyle(.borderedProminent)
                Spacer()
            }
        }
        .padding(24)
        .frame(minWidth: 720, minHeight: 560)
    }

    private func downloadImage() {
        guard let url = URL(string: imageUrl) else { return }
        let panel = NSSavePanel()
        panel.nameFieldStringValue = url.lastPathComponent.isEmpty ? "image" : url.lastPathComponent
        guard panel.runModal() == .OK, let destination = panel.url else { return }
        Task.detached {
            do {
                let (data, _) = try await URLSession.shared.data(from: url)
                try data.write(to: destination)
            } catch {
            }
        }
    }
}
