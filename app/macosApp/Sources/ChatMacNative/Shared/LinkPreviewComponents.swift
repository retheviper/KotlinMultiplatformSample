import SwiftUI
import WebKit

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
                if !isImageOnly, let url = URL(string: preview.url) {
                    Link(preview.url, destination: url)
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
                .background(Color.platformPanelBackground)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            } else {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }

            HStack {
                if let url = URL(string: imageUrl) {
                    Link("Open in Browser", destination: url)
                    ShareLink(item: url) {
                        Text("Share")
                    }
                }
                Spacer()
            }
        }
        .padding(24)
        #if os(macOS)
        .frame(minWidth: 720, minHeight: 560)
        #endif
    }
}

struct RemotePreviewImage: View {
    let url: URL
    let contentDescription: String
    let contentMode: ContentMode

    private var isSVG: Bool {
        let ext = url.pathExtension.lowercased()
        return ext == "svg" || ext == "svgz"
    }

    var body: some View {
        Group {
            if isSVG {
                SVGPreviewView(url: url)
            } else {
                AsyncImage(url: url) { image in
                    switch contentMode {
                    case .fit:
                        image.resizable().scaledToFit()
                    default:
                        image.resizable().scaledToFill()
                    }
                } placeholder: {
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(Color.accentColor.opacity(0.08))
                }
            }
        }
        .accessibilityLabel(contentDescription)
    }
}

private struct SVGPreviewView: View {
    let url: URL

    var body: some View {
        Group {
            if #available(iOS 26.0, macOS 26.0, *) {
                ModernSVGWebView(url: url)
            } else {
                LegacySVGWebView(url: url)
            }
        }
    }
}

@available(iOS 26.0, macOS 26.0, *)
private struct ModernSVGWebView: View {
    let url: URL
    @State private var page = WebPage()

    var body: some View {
        WebView(page)
            .task(id: url) {
                page.load(url)
            }
    }
}

#if canImport(UIKit)
import UIKit

private struct LegacySVGWebView: UIViewRepresentable {
    let url: URL

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.isOpaque = false
        webView.backgroundColor = .clear
        webView.scrollView.backgroundColor = .clear
        webView.load(URLRequest(url: url))
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {
        guard uiView.url != url else { return }
        uiView.load(URLRequest(url: url))
    }
}
#elseif canImport(AppKit)
import AppKit

private struct LegacySVGWebView: NSViewRepresentable {
    let url: URL

    func makeNSView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.setValue(false, forKey: "drawsBackground")
        webView.load(URLRequest(url: url))
        return webView
    }

    func updateNSView(_ nsView: WKWebView, context: Context) {
        guard nsView.url != url else { return }
        nsView.load(URLRequest(url: url))
    }
}
#endif
