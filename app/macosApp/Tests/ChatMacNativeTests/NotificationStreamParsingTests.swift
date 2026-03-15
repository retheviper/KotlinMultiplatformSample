import XCTest
@testable import KMPs

final class NotificationStreamParsingTests: XCTestCase {
    func testIgnoresNonDataLines() {
        XCTAssertEqual(notificationStreamLineResult(for: "event: ping", initialized: false), .ignore)
    }

    func testConnectedLineOnlyInitializesOnce() {
        XCTAssertEqual(notificationStreamLineResult(for: "data: connected", initialized: false), .initializeOnly)
        XCTAssertEqual(notificationStreamLineResult(for: "data: connected", initialized: true), .signal)
    }

    func testRefreshLineEmitsSignal() {
        XCTAssertEqual(notificationStreamLineResult(for: "data: refresh", initialized: false), .signal)
    }
}
