import XCTest
@testable import KMPs

final class ChatEventDecodingTests: XCTestCase {
    func testDecodesMessagePostedEventWithoutMessagesField() throws {
        let payload = """
        {
          "type": "MESSAGE_POSTED",
          "message": {
            "id": "message-1",
            "channelId": "channel-1",
            "authorMemberId": "member-1",
            "authorDisplayName": "Alice",
            "body": "hello",
            "createdAt": "2026-03-14T00:00:00Z"
          }
        }
        """.data(using: .utf8)!

        let event = try JSONDecoder().decode(ChatEvent.self, from: payload)

        XCTAssertEqual(event.type, .messagePosted)
        XCTAssertEqual(event.message?.id, "message-1")
        XCTAssertEqual(event.message?.threadReplyCount, 0)
        XCTAssertEqual(event.message?.reactions, [])
        XCTAssertTrue(event.messages.isEmpty)
        XCTAssertNil(event.error)
    }

    func testDecodesSnapshotEventWithoutMessageField() throws {
        let payload = """
        {
          "type": "SNAPSHOT",
          "messages": [
            {
              "id": "message-1",
              "channelId": "channel-1",
              "authorMemberId": "member-1",
              "authorDisplayName": "Alice",
              "body": "hello",
              "createdAt": "2026-03-14T00:00:00Z"
            }
          ]
        }
        """.data(using: .utf8)!

        let event = try JSONDecoder().decode(ChatEvent.self, from: payload)

        XCTAssertEqual(event.type, .snapshot)
        XCTAssertEqual(event.messages.count, 1)
        XCTAssertEqual(event.messages.first?.threadReplyCount, 0)
        XCTAssertEqual(event.messages.first?.reactions, [])
        XCTAssertNil(event.message)
    }
}
