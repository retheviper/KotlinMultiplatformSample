import AppKit
import SwiftUI

struct LinkifiedText: View {
    let text: String
    let mentionUserIds: Set<String>

    var body: some View {
        Text(attributedText)
            .environment(\.openURL, OpenURLAction { url in
                NSWorkspace.shared.open(url)
                return .handled
            })
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
