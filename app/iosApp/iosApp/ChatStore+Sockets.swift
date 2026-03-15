import Foundation

@MainActor
extension ChatStore {
    func connectWebSocket(channelId: String) async throws {
        disconnectWebSocket()
        let socket = try await client.openChat(channelId: channelId)
        webSocket = socket
        connectedChannelId = channelId
        receiveTask = Task { [weak self] in
            await self?.receiveLoop(socket: socket)
        }
        try await sendRaw(ChatCommand(type: .loadRecent, authorMemberId: nil, body: nil, linkPreview: nil, parentMessageId: nil, messageId: nil, emoji: nil, limit: 50))
    }

    func disconnectWebSocket() {
        receiveTask?.cancel()
        receiveTask = nil
        webSocket = nil
        connectedChannelId = nil
    }

    func send(_ command: ChatCommand) async {
        do {
            try await ensureSocketConnectedForSelectedChannel()
            try await sendRaw(command)
        } catch {
            do {
                try await reconnectSelectedChannelSocket()
                try await sendRaw(command)
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }

    func sendRaw(_ command: ChatCommand) async throws {
        guard let webSocket else {
            throw ApiErrorResponse(code: "socket_not_connected", message: "Socket is not connected")
        }
        try await client.sendCommand(session: webSocket, command: command)
    }

    func ensureSocketConnectedForSelectedChannel() async throws {
        guard let channel = selectedChannel else { return }
        if webSocket == nil || connectedChannelId != channel.id {
            try await connectWebSocket(channelId: channel.id)
        }
    }

    func reconnectSelectedChannelSocket() async throws {
        guard let channel = selectedChannel else {
            throw ApiErrorResponse(code: "socket_not_connected", message: "Socket is not connected")
        }
        try await connectWebSocket(channelId: channel.id)
    }

    func receiveLoop(socket: ChatSocketSession) async {
        while !Task.isCancelled {
            do {
                let event = try await client.receiveEvent(session: socket)
                apply(event: event)
                if selectedMember != nil {
                    try? await refreshVisibleNotifications()
                }
            } catch {
                if !Task.isCancelled {
                    if webSocket?.raw === socket.raw {
                        webSocket = nil
                        connectedChannelId = nil
                    }
                    errorMessage = error.localizedDescription
                }
                break
            }
        }
    }

    func apply(event: ChatEvent) {
        switch event.type {
        case .snapshot:
            messages = event.messages
        case .messagePosted:
            guard let message = event.message else { return }
            messages.append(message)
        case .replyPosted:
            guard let message = event.message else { return }
            if threadRoot?.id == message.threadRootMessageId {
                threadReplies.append(message)
            }
            if let rootId = message.threadRootMessageId,
               let index = messages.firstIndex(where: { $0.id == rootId }) {
                let root = messages[index]
                messages[index] = MessageResponse(
                    id: root.id,
                    channelId: root.channelId,
                    authorMemberId: root.authorMemberId,
                    authorDisplayName: root.authorDisplayName,
                    body: root.body,
                    linkPreview: root.linkPreview,
                    threadRootMessageId: root.threadRootMessageId,
                    threadReplyCount: root.threadReplyCount + 1,
                    reactions: root.reactions,
                    createdAt: root.createdAt
                )
            }
        case .reactionUpdated:
            guard let updated = event.message else { return }
            if let index = messages.firstIndex(where: { $0.id == updated.id }) {
                messages[index] = updated
            }
            if threadRoot?.id == updated.id {
                threadRoot = updated
            }
            if let index = threadReplies.firstIndex(where: { $0.id == updated.id }) {
                threadReplies[index] = updated
            }
        case .error:
            errorMessage = event.error?.message ?? "WebSocket error"
        }
    }
}
