import XCTest
@testable import KMPs

final class MentionHelpersTests: XCTestCase {
    func testSuggestsMembersForActiveMentionQuery() {
        let members = [
            WorkspaceMemberResponse(id: "1", workspaceId: "w", userId: "alice", displayName: "Alice", role: .member, joinedAt: "now"),
            WorkspaceMemberResponse(id: "2", workspaceId: "w", userId: "bob", displayName: "Bob", role: .member, joinedAt: "now")
        ]

        let results = mentionCandidates(for: "hello @ali", members: members)

        XCTAssertEqual(results.map(\.userId), ["alice"])
    }

    func testDoesNotSuggestWhenAtSignIsPartOfAnotherToken() {
        let members = [
            WorkspaceMemberResponse(id: "1", workspaceId: "w", userId: "alice", displayName: "Alice", role: .member, joinedAt: "now")
        ]

        let results = mentionCandidates(for: "email@test.com", members: members)

        XCTAssertTrue(results.isEmpty)
    }

    func testReplaceMentionCandidateRewritesTrailingToken() {
        let updated = replaceMentionCandidate(in: "ping @ali", with: "alice")
        XCTAssertEqual(updated, "ping @alice ")
    }
}
