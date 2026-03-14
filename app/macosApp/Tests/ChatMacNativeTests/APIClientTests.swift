import XCTest
@testable import ChatMacNative

final class APIClientTests: XCTestCase {
    func testWebSocketURLUsesWsForHttpBaseURL() {
        let client = APIClient(baseURL: URL(string: "http://localhost:8080")!)

        let url = client.webSocketURL(channelId: "channel-1")

        XCTAssertEqual(url.absoluteString, "ws://localhost:8080/ws/channels/channel-1")
    }

    func testWebSocketURLUsesWssForHttpsBaseURL() {
        let client = APIClient(baseURL: URL(string: "https://chat.example.com")!)

        let url = client.webSocketURL(channelId: "channel-1")

        XCTAssertEqual(url.absoluteString, "wss://chat.example.com/ws/channels/channel-1")
    }
}
