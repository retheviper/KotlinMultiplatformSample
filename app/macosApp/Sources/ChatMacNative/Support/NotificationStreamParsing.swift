import Foundation

enum NotificationStreamLineResult: Equatable {
    case ignore
    case initializeOnly
    case signal
}

func notificationStreamLineResult(for line: String, initialized: Bool) -> NotificationStreamLineResult {
    guard line.hasPrefix("data:") else {
        return .ignore
    }

    let payload = line.dropFirst(5).trimmingCharacters(in: .whitespacesAndNewlines)
    if !initialized && payload == "connected" {
        return .initializeOnly
    }
    return .signal
}
