import SwiftUI

struct NativeLinkPreviewCard: View {
    let preview: LinkPreviewResponse
    let onDismiss: (() -> Void)?
    @State private var showingImageDialog = false

    private var isImageOnly: Bool {
        preview.imageUrl == preview.url &&
        (preview.title?.isEmpty ?? true) &&
        (preview.description?.isEmpty ?? true) &&
        (preview.siteName?.isEmpty ?? true)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            if let imageUrl = preview.imageUrl, let url = URL(string: imageUrl) {
                RemotePreviewImage(
                    url: url,
                    contentDescription: preview.title ?? preview.siteName ?? "Link preview image",
                    contentMode: .fill
                )
                .frame(maxWidth: .infinity, minHeight: 180, maxHeight: 220)
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                .onTapGesture {
                    showingImageDialog = true
                }
            }

            if !isImageOnly, let siteName = preview.siteName {
                Text(siteName)
                    .font(.headline)
            }
            if !isImageOnly, let title = preview.title {
                Text(title)
                    .font(.subheadline.bold())
            }
            if !isImageOnly, let description = preview.description {
                Text(description)
                    .foregroundStyle(.secondary)
                    .lineLimit(3)
            }

            HStack {
                if !isImageOnly {
                    Link(preview.url, destination: URL(string: preview.url)!)
                        .font(.caption)
                        .lineLimit(1)
                }
                Spacer()
                if let onDismiss {
                    Button("x") {
                        onDismiss()
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .padding(12)
        .background(Color.accentColor.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .sheet(isPresented: $showingImageDialog) {
            if let imageUrl = preview.imageUrl {
                ImagePreviewSheet(imageUrl: imageUrl, title: preview.title)
            }
        }
    }
}
