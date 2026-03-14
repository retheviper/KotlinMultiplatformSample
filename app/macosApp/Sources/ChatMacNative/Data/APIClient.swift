import Foundation

struct APIClient {
    var baseURL: URL
    private let session: URLSession
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    init(baseURL: URL, session: URLSession = .shared) {
        self.baseURL = baseURL
        self.session = session
    }

    func listWorkspaces() async throws -> [WorkspaceResponse] {
        try await request(path: "/api/v1/workspaces")
    }

    func getWorkspaceBySlug(_ slug: String) async throws -> WorkspaceResponse {
        try await request(path: "/api/v1/workspaces/by-slug/\(slug.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? slug)")
    }

    func createWorkspace(_ requestBody: CreateWorkspaceRequest) async throws -> WorkspaceResponse {
        try await request(path: "/api/v1/workspaces", method: "POST", body: requestBody)
    }

    func listMembers(workspaceId: String) async throws -> [WorkspaceMemberResponse] {
        try await request(path: "/api/v1/workspaces/\(workspaceId)/members")
    }

    func addMember(workspaceId: String, requestBody: AddWorkspaceMemberRequest) async throws -> WorkspaceMemberResponse {
        try await request(path: "/api/v1/workspaces/\(workspaceId)/members", method: "POST", body: requestBody)
    }

    func updateMember(memberId: String, displayName: String) async throws -> WorkspaceMemberResponse {
        try await request(path: "/api/v1/members/\(memberId)", method: "PUT", body: UpdateWorkspaceMemberRequest(displayName: displayName))
    }

    func listChannels(workspaceId: String) async throws -> [ChannelResponse] {
        try await request(path: "/api/v1/workspaces/\(workspaceId)/channels")
    }

    func createChannel(workspaceId: String, requestBody: CreateChannelRequest) async throws -> ChannelResponse {
        try await request(path: "/api/v1/workspaces/\(workspaceId)/channels", method: "POST", body: requestBody)
    }

    func listMessages(channelId: String, beforeMessageId: String? = nil, limit: Int = 50) async throws -> [MessageResponse] {
        var items = [
            URLQueryItem(name: "limit", value: String(limit))
        ]
        if let beforeMessageId {
            items.append(URLQueryItem(name: "beforeMessageId", value: beforeMessageId))
        }
        let page: MessagePageResponse = try await request(path: "/api/v1/channels/\(channelId)/messages", queryItems: items)
        return page.messages
    }

    func getThread(messageId: String) async throws -> ThreadResponse {
        try await request(path: "/api/v1/messages/\(messageId)/thread")
    }

    func toggleReaction(messageId: String, memberId: String, emoji: String) async throws -> MessageResponse {
        try await request(
            path: "/api/v1/messages/\(messageId)/reactions/toggle",
            method: "POST",
            body: ToggleReactionRequest(memberId: memberId, emoji: emoji)
        )
    }

    func listNotifications(memberId: String, unreadOnly: Bool) async throws -> [MentionNotificationResponse] {
        try await request(
            path: "/api/v1/members/\(memberId)/notifications",
            queryItems: [URLQueryItem(name: "unreadOnly", value: unreadOnly ? "true" : "false")]
        )
    }

    func markNotificationsRead(memberId: String, ids: [String]) async throws {
        let _: EmptyResponse = try await request(
            path: "/api/v1/members/\(memberId)/notifications/read",
            method: "POST",
            body: MarkNotificationsReadRequest(notificationIds: ids)
        )
    }

    func resolveLinkPreview(url: String) async throws -> LinkPreviewResponse? {
        let response: ResolveLinkPreviewResponse = try await request(
            path: "/api/v1/link-preview/resolve",
            method: "POST",
            body: ResolveLinkPreviewRequest(url: url)
        )
        return response.preview
    }

    func webSocketURL(channelId: String) -> URL {
        var components = URLComponents(url: baseURL, resolvingAgainstBaseURL: false)!
        components.scheme = components.scheme == "https" ? "wss" : "ws"
        components.path = "/ws/channels/\(channelId)"
        components.query = nil
        return components.url!
    }

    func notificationStreamRequest(memberId: String) throws -> URLRequest {
        var components = URLComponents(url: baseURL, resolvingAgainstBaseURL: false)!
        components.path = "/api/v1/members/\(memberId)/notifications/stream"
        components.query = nil
        guard let url = components.url else {
            throw ApiErrorResponse(code: "invalid_url", message: "Unable to build notification stream URL.")
        }

        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("text/event-stream", forHTTPHeaderField: "Accept")
        request.timeoutInterval = 60 * 60
        return request
    }

    private func requestInternal<Response: Decodable>(
        path: String,
        method: String = "GET",
        queryItems: [URLQueryItem] = [],
        body: AnyEncodable? = nil
    ) async throws -> Response {
        var components = URLComponents(url: baseURL, resolvingAgainstBaseURL: false)!
        components.path = path
        if !queryItems.isEmpty {
            components.queryItems = queryItems
        }
        guard let url = components.url else {
            throw ApiErrorResponse(code: "invalid_url", message: "Unable to build request URL.")
        }

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let body {
            request.httpBody = try encoder.encode(body)
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }

        let (data, response) = try await session.data(for: request)
        let status = (response as? HTTPURLResponse)?.statusCode ?? 500
        guard 200..<300 ~= status else {
            if let apiError = try? decoder.decode(ApiErrorResponse.self, from: data) {
                throw apiError
            }
            throw ApiErrorResponse(code: "http_\(status)", message: String(data: data, encoding: .utf8) ?? "Request failed.")
        }

        if Response.self == EmptyResponse.self {
            return EmptyResponse() as! Response
        }
        return try decoder.decode(Response.self, from: data)
    }

    private func request<Response: Decodable>(
        path: String,
        method: String = "GET",
        queryItems: [URLQueryItem] = []
    ) async throws -> Response {
        try await requestInternal(path: path, method: method, queryItems: queryItems, body: nil)
    }

    private func request<Response: Decodable, Body: Encodable>(
        path: String,
        method: String = "GET",
        queryItems: [URLQueryItem] = [],
        body: Body
    ) async throws -> Response {
        try await requestInternal(path: path, method: method, queryItems: queryItems, body: AnyEncodable(body))
    }
}

private struct EmptyResponse: Decodable {}

private struct AnyEncodable: Encodable {
    private let encodeBody: (Encoder) throws -> Void

    init<T: Encodable>(_ wrapped: T) {
        self.encodeBody = wrapped.encode(to:)
    }

    func encode(to encoder: Encoder) throws {
        try encodeBody(encoder)
    }
}
