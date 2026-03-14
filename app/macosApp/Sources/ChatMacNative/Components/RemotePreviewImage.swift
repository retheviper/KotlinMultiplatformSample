import SwiftUI
import WebKit

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
                SVGWebView(url: url)
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

private struct SVGWebView: NSViewRepresentable {
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
