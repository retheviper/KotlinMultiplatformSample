package com.retheviper.chat.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import com.retheviper.chat.client.MessagingClient
import com.retheviper.chat.client.NotificationStreamHandle
import com.retheviper.chat.contract.ChannelResponse
import com.retheviper.chat.contract.ChatCommand
import com.retheviper.chat.contract.ChatCommandType
import com.retheviper.chat.contract.ChatEvent
import com.retheviper.chat.contract.ChatEventType
import com.retheviper.chat.contract.LinkPreviewResponse
import com.retheviper.chat.contract.MentionNotificationResponse
import com.retheviper.chat.contract.NotificationKind
import com.retheviper.chat.contract.UpdateWorkspaceMemberRequest
import com.retheviper.chat.contract.WorkspaceMemberResponse
import com.retheviper.chat.contract.WorkspaceResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class MessagingAppState(
    private val environment: MessagingAppEnvironment,
    private val scope: CoroutineScope
) {
    val client = MessagingClient(environment.baseUrl)

    var screen by mutableStateOf(AppScreen.LANDING)
    var status by mutableStateOf(AppDefaults.initialStatus)
    val workspaces = mutableStateListOf<WorkspaceResponse>()
    var workspace by mutableStateOf<WorkspaceResponse?>(null)
    var currentMember by mutableStateOf<WorkspaceMemberResponse?>(null)
    val workspaceMembers = mutableStateListOf<WorkspaceMemberResponse>()
    val workspaceChannels = mutableStateListOf<ChannelResponse>()
    var channel by mutableStateOf<ChannelResponse?>(null)
    val messages = mutableStateListOf<com.retheviper.chat.contract.MessageResponse>()
    val threadMessages = mutableStateListOf<com.retheviper.chat.contract.MessageResponse>()
    val unreadNotifications = mutableStateListOf<MentionNotificationResponse>()
    val allNotifications = mutableStateListOf<MentionNotificationResponse>()
    val toastNotifications = mutableStateListOf<ToastNotification>()
    var notificationStreamHandle by mutableStateOf<NotificationStreamHandle?>(null)
    var selectedRootId by mutableStateOf<String?>(null)
    var chatRuntime by mutableStateOf<ChatRuntime?>(null)
    var focusedMessageId by mutableStateOf<String?>(null)
    var focusedThreadMessageId by mutableStateOf<String?>(null)
    var centerView by mutableStateOf(WorkspaceCenterView.CHANNEL)
    var hasOlderMessages by mutableStateOf(false)
    var loadingOlderMessages by mutableStateOf(false)
    var scrollToLatestOnNextSnapshot by mutableStateOf(false)

    var createWorkspaceSlug by mutableStateOf(AppDefaults.workspaceSlug)
    var createWorkspaceName by mutableStateOf(AppDefaults.workspaceName)
    var createOwnerUserId by mutableStateOf(AppDefaults.ownerUserId)
    var createOwnerDisplayName by mutableStateOf(AppDefaults.ownerDisplayName)
    var joinUserId by mutableStateOf(AppDefaults.memberUserId)
    var joinDisplayName by mutableStateOf(AppDefaults.memberDisplayName)
    var channelSlug by mutableStateOf(AppDefaults.channelSlug)
    var channelName by mutableStateOf(AppDefaults.channelName)
    var channelTopic by mutableStateOf(AppDefaults.channelTopic)
    var messageBody by mutableStateOf("")
    var threadMessageBody by mutableStateOf("")
    var messageLinkPreview by mutableStateOf<LinkPreviewResponse?>(null)
    var threadLinkPreview by mutableStateOf<LinkPreviewResponse?>(null)
    var dismissedMessagePreviewUrl by mutableStateOf<String?>(null)
    var dismissedThreadPreviewUrl by mutableStateOf<String?>(null)
    var profileDisplayName by mutableStateOf(AppDefaults.memberDisplayName)

    suspend fun refreshWorkspaceList() {
        workspaces.replaceWith(client.listWorkspaces())
    }

    suspend fun refreshWorkspaceContext(targetWorkspace: WorkspaceResponse) {
        workspaceMembers.replaceWith(client.listWorkspaceMembers(targetWorkspace.id))
        workspaceChannels.replaceWith(client.listWorkspaceChannels(targetWorkspace.id))
    }

    suspend fun disconnectChat() {
        val runtime = chatRuntime ?: return
        chatRuntime = null
        runtime.close()
    }

    fun disconnectChatAsync() {
        val runtime = chatRuntime ?: return
        chatRuntime = null
        scope.launch(Dispatchers.Default) {
            runCatching { runtime.close() }
        }
    }

    fun disconnectNotificationStreamAsync() {
        val handle = notificationStreamHandle ?: return
        notificationStreamHandle = null
        scope.launch(Dispatchers.Default) {
            runCatching { handle.close() }
        }
    }

    fun resetWorkspaceSession() {
        screen = AppScreen.LANDING
        workspace = null
        currentMember = null
        channel = null
        workspaceMembers.clear()
        workspaceChannels.clear()
        messages.clear()
        threadMessages.clear()
        unreadNotifications.clear()
        allNotifications.clear()
        toastNotifications.clear()
        selectedRootId = null
        focusedMessageId = null
        focusedThreadMessageId = null
        centerView = WorkspaceCenterView.CHANNEL
        threadMessageBody = ""
        messageBody = ""
        messageLinkPreview = null
        threadLinkPreview = null
        dismissedMessagePreviewUrl = null
        dismissedThreadPreviewUrl = null
        disconnectChatAsync()
        disconnectNotificationStreamAsync()
        status = AppDefaults.initialStatus
    }

    suspend fun updateMessageBody(body: String, thread: Boolean = false) {
        if (thread) {
            threadMessageBody = body
            resolvePreviewForBody(
                body = body,
                currentPreview = threadLinkPreview,
                dismissedUrl = dismissedThreadPreviewUrl,
                onPreviewResolved = { preview -> threadLinkPreview = preview },
                onDismissedUrlChange = { dismissedThreadPreviewUrl = it }
            )
        } else {
            messageBody = body
            resolvePreviewForBody(
                body = body,
                currentPreview = messageLinkPreview,
                dismissedUrl = dismissedMessagePreviewUrl,
                onPreviewResolved = { preview -> messageLinkPreview = preview },
                onDismissedUrlChange = { dismissedMessagePreviewUrl = it }
            )
        }
    }

    fun dismissMessagePreview(thread: Boolean = false) {
        if (thread) {
            dismissedThreadPreviewUrl = threadLinkPreview?.url ?: extractFirstUrl(threadMessageBody)
            threadLinkPreview = null
        } else {
            dismissedMessagePreviewUrl = messageLinkPreview?.url ?: extractFirstUrl(messageBody)
            messageLinkPreview = null
        }
    }

    private suspend fun resolvePreviewForBody(
        body: String,
        currentPreview: LinkPreviewResponse?,
        dismissedUrl: String?,
        onPreviewResolved: (LinkPreviewResponse?) -> Unit,
        onDismissedUrlChange: (String?) -> Unit
    ) {
        val url = extractFirstUrl(body)
        if (url == null) {
            onPreviewResolved(null)
            onDismissedUrlChange(null)
            return
        }
        if (dismissedUrl == url) {
            onPreviewResolved(null)
            return
        }
        if (currentPreview?.url == url) return
        onDismissedUrlChange(null)
        val preview = runCatching { client.resolveLinkPreview(url).preview }.getOrNull()
        onPreviewResolved(preview)
    }

    private fun enqueueToast(notification: ToastNotification) {
        if (toastNotifications.none { it.id == notification.id }) {
            toastNotifications.add(0, notification)
        }
        scope.launch {
            delay(4000)
            toastNotifications.removeAll { it.id == notification.id }
        }
    }

    suspend fun refreshNotifications(showToast: Boolean) {
        val member = currentMember ?: return
        val previousUnread = unreadNotifications.toList()
        val includeHistory = shouldLoadNotificationHistory(centerView, allNotifications)
        val (latest, latestAll) = coroutineScope {
            val unreadDeferred = async { client.listNotifications(member.id, unreadOnly = true) }
            val allDeferred = if (includeHistory) async { client.listNotifications(member.id, unreadOnly = false) } else null
            unreadDeferred.await() to (allDeferred?.await() ?: allNotifications.toList())
        }
        unreadNotifications.replaceWith(latest)
        allNotifications.replaceWith(latestAll)
        if (showToast) {
            findNewUnreadNotifications(previousUnread, latest).take(3).forEach { notification ->
                enqueueToast(
                    ToastNotification(
                        id = notification.id,
                        title = notificationTitle(notification),
                        body = notification.messagePreview,
                        channelId = notification.channelId,
                        messageId = notification.messageId,
                        threadRootMessageId = notification.threadRootMessageId,
                        readNotificationId = notification.id
                    )
                )
            }
        }
    }

    suspend fun markNotificationsRead(notificationIds: List<String>) {
        val member = currentMember ?: return
        if (notificationIds.isEmpty()) return
        val readIds = notificationIds.toHashSet()
        runCatching {
            client.markNotificationsRead(member.id, notificationIds)
            val updated = applyNotificationRead(
                current = NotificationRefreshState(unreadNotifications.toList(), allNotifications.toList()),
                readIds = readIds
            )
            unreadNotifications.replaceWith(updated.unreadNotifications)
            allNotifications.replaceWith(updated.allNotifications)
        }.onFailure {
            status = it.message ?: AppStatus.failedUpdateNotifications
        }
    }

    suspend fun updateCurrentMemberDisplayName(displayName: String) {
        val member = currentMember ?: return
        val updated = client.updateWorkspaceMember(member.id, UpdateWorkspaceMemberRequest(displayName = displayName))
        currentMember = updated
        workspaceMembers.replaceWith(workspaceMembers.map { existing -> if (existing.id == updated.id) updated else existing })
        if (joinUserId == updated.userId) joinDisplayName = updated.displayName
        profileDisplayName = updated.displayName
        status = AppStatus.profileUpdated
    }

    suspend fun connectChannel(targetChannel: ChannelResponse) {
        channel = targetChannel
        centerView = WorkspaceCenterView.CHANNEL
        messages.clear()
        threadMessages.clear()
        hasOlderMessages = false
        loadingOlderMessages = false
        scrollToLatestOnNextSnapshot = true
        selectedRootId = null
        messageBody = ""
        threadMessageBody = ""
        messageLinkPreview = null
        threadLinkPreview = null
        dismissedMessagePreviewUrl = null
        dismissedThreadPreviewUrl = null
        focusedMessageId = null
        focusedThreadMessageId = null
        disconnectChat()
        status = AppStatus.connectingChannel(targetChannel.slug)

        runCatching {
            val initialMessages = client.listChannelMessages(targetChannel.id, limit = 50).messages
            messages.replaceWith(initialMessages)
            hasOlderMessages = initialMessages.size >= 50
            focusedMessageId = initialMessages.lastOrNull()?.id
            scrollToLatestOnNextSnapshot = false
            val session = client.openChat(targetChannel.id)
            val collector = scope.launch {
                while (true) {
                    when (val event = client.receiveEvent(session)) {
                        is ChatEvent -> when (event.type) {
                            ChatEventType.SNAPSHOT,
                            ChatEventType.MESSAGE_POSTED,
                            ChatEventType.REPLY_POSTED,
                            ChatEventType.REACTION_UPDATED -> handleChatEvent(event)
                            ChatEventType.ERROR -> status = event.error?.message ?: AppStatus.socketError
                        }
                    }
                }
            }
            client.sendCommand(session, ChatCommand(type = ChatCommandType.LOAD_RECENT, limit = 50))
            chatRuntime = ChatRuntime(session, collector)
        }.onSuccess {
            markNotificationsRead(unreadNotifications.filter { it.channelId == targetChannel.id }.map { it.id })
            status = AppStatus.channelConnected(targetChannel.slug)
        }.onFailure {
            status = it.message ?: AppStatus.channelConnectionFailed
        }
    }

    private suspend fun handleChatEvent(event: ChatEvent) {
        if (event.type == ChatEventType.SNAPSHOT) {
            hasOlderMessages = event.messages.size >= 50
            if (scrollToLatestOnNextSnapshot) {
                focusedMessageId = event.messages.lastOrNull()?.id
                scrollToLatestOnNextSnapshot = false
            }
        }
        messages.replaceWith(reduceChatFeed(ChatFeedState(messages.toList()), event).messages)
        event.message?.let { updated ->
            if (threadMessages.any { it.id == updated.id }) {
                threadMessages.replaceWith(threadMessages.toList().replaceMessage(updated))
            }
        }
        if (selectedRootId != null && event.message?.threadRootMessageId == selectedRootId) {
            loadThread(client, selectedRootId, threadMessages) { status = it }
        }
        if (
            event.message?.body?.contains("@${currentMember?.userId}") == true ||
            (
                event.type == ChatEventType.REPLY_POSTED &&
                    event.message?.authorMemberId != currentMember?.id &&
                    event.message?.threadRootMessageId != null &&
                    event.message.threadRootMessageId != selectedRootId
                )
        ) {
            refreshNotifications(showToast = true)
        }
    }

    suspend fun openWorkspace(targetWorkspace: WorkspaceResponse, member: WorkspaceMemberResponse?) {
        workspace = targetWorkspace
        currentMember = member
        profileDisplayName = member?.displayName ?: joinDisplayName
        channel = null
        messages.clear()
        threadMessages.clear()
        selectedRootId = null
        messageBody = ""
        threadMessageBody = ""
        messageLinkPreview = null
        threadLinkPreview = null
        dismissedMessagePreviewUrl = null
        dismissedThreadPreviewUrl = null
        focusedMessageId = null
        focusedThreadMessageId = null
        disconnectChat()
        refreshWorkspaceContext(targetWorkspace)
        screen = if (member == null) AppScreen.JOIN_WORKSPACE else AppScreen.WORKSPACE
        centerView = WorkspaceCenterView.CHANNEL
        status = if (member == null) AppStatus.signInPrompt else AppStatus.workspaceOpened
        if (member != null) {
            refreshNotifications(showToast = false)
            workspaceChannels.firstOrNull()?.let { connectChannel(it) }
        }
    }

    suspend fun navigateToNotification(notification: ToastNotification) {
        val targetChannel = workspaceChannels.firstOrNull { it.id == notification.channelId } ?: return
        if (channel?.id != targetChannel.id) connectChannel(targetChannel)
        if (notification.threadRootMessageId != null) {
            selectedRootId = notification.threadRootMessageId
            focusedMessageId = notification.threadRootMessageId
            focusedThreadMessageId = notification.messageId
            loadThread(client, notification.threadRootMessageId, threadMessages) { status = it }
            markNotificationsRead(threadNotificationIdsToMarkRead(unreadNotifications, notification.threadRootMessageId))
        } else {
            selectedRootId = null
            threadMessages.clear()
            focusedMessageId = notification.messageId
            focusedThreadMessageId = null
            threadLinkPreview = null
            dismissedThreadPreviewUrl = null
        }
        toastNotifications.removeAll { it.id == notification.id }
        notification.readNotificationId?.let { markNotificationsRead(listOf(it)) }
    }

    suspend fun loadOlderMessages() {
        val targetChannel = channel ?: return
        val oldestMessageId = messages.firstOrNull()?.id ?: return
        if (loadingOlderMessages) return
        loadingOlderMessages = true
        runCatching {
            client.listChannelMessages(targetChannel.id, limit = 50, beforeMessageId = oldestMessageId).messages
        }.onSuccess { olderMessages ->
            if (olderMessages.isNotEmpty()) messages.replaceWith(olderMessages + messages)
            hasOlderMessages = olderMessages.size >= 50
        }.onFailure {
            status = it.message ?: AppStatus.failedLoadOlderMessages
        }
        loadingOlderMessages = false
    }

    fun startNotificationStreamIfNeeded() {
        disconnectNotificationStreamAsync()
        if (screen == AppScreen.WORKSPACE && currentMember != null && workspace != null) {
            notificationStreamHandle = environment.notificationStreamConnector.connect(
                baseUrl = environment.baseUrl,
                memberId = currentMember!!.id,
                onNotificationSignal = {
                    scope.launch {
                        runCatching { refreshNotifications(showToast = true) }
                            .onFailure { status = it.message ?: AppStatus.failedRefreshNotifications }
                    }
                },
                onFailure = { throwable ->
                    scope.launch { status = throwable.message ?: AppStatus.notificationStreamDisconnected }
                }
            )
        }
    }

    suspend fun refreshOrPollNotifications() {
        if (screen != AppScreen.WORKSPACE || currentMember == null || workspace == null) {
            unreadNotifications.clear()
            allNotifications.clear()
            toastNotifications.clear()
            return
        }
        if (notificationStreamHandle != null) {
            runCatching { refreshNotifications(showToast = false) }
                .onFailure { status = it.message ?: AppStatus.failedLoadNotifications }
            return
        }
        while (true) {
            runCatching { refreshNotifications(showToast = true) }
                .onFailure { status = it.message ?: AppStatus.failedLoadNotifications }
            delay(5000)
        }
    }

    suspend fun toggleReaction(message: com.retheviper.chat.contract.MessageResponse, emoji: String) {
        val member = currentMember ?: return
        runCatching {
            client.toggleReaction(messageId = message.id, memberId = member.id, emoji = emoji)
        }.onSuccess { updated ->
            messages.replaceWith(messages.toList().replaceMessage(updated))
            if (threadMessages.any { it.id == updated.id }) {
                threadMessages.replaceWith(threadMessages.toList().replaceMessage(updated))
            }
        }.onFailure {
            status = it.message ?: AppStatus.reactionFailed
        }
    }

    suspend fun dispose() {
        disconnectNotificationStreamAsync()
        disconnectChat()
        client.close()
    }
}

@Composable
internal fun rememberMessagingAppState(
    environment: MessagingAppEnvironment,
    scope: CoroutineScope
): MessagingAppState {
    return remember(environment.baseUrl, environment.platformName, scope) {
        MessagingAppState(environment = environment, scope = scope)
    }
}
