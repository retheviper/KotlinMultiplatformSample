package com.retheviper.chat.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.retheviper.chat.client.MessagingClient
import com.retheviper.chat.client.NotificationStreamClient
import com.retheviper.chat.client.NotificationStreamHandle
import com.retheviper.chat.client.PlatformClientConfig
import com.retheviper.chat.contract.AddWorkspaceMemberRequest
import com.retheviper.chat.contract.ChannelResponse
import com.retheviper.chat.contract.ChannelVisibility
import com.retheviper.chat.contract.ChatCommand
import com.retheviper.chat.contract.ChatCommandType
import com.retheviper.chat.contract.ChatEvent
import com.retheviper.chat.contract.ChatEventType
import com.retheviper.chat.contract.CreateChannelRequest
import com.retheviper.chat.contract.CreateWorkspaceRequest
import com.retheviper.chat.contract.LinkPreviewResponse
import com.retheviper.chat.contract.MentionNotificationResponse
import com.retheviper.chat.contract.NotificationKind
import com.retheviper.chat.contract.UpdateWorkspaceMemberRequest
import com.retheviper.chat.contract.WorkspaceMemberResponse
import com.retheviper.chat.contract.WorkspaceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class ToastNotification(
    val id: String,
    val title: String,
    val body: String,
    val channelId: String,
    val messageId: String,
    val threadRootMessageId: String?,
    val readNotificationId: String? = null
)

data class AppNotificationEvent(
    val id: String,
    val title: String,
    val body: String
)

@Composable
fun MessagingApp(
    onUnreadNotificationCountChange: (Int) -> Unit = {},
    onNotificationEvent: (AppNotificationEvent) -> Unit = {},
    onWindowTitleChange: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val client = remember { MessagingClient(PlatformClientConfig.baseUrl) }
    val appFontFamily = rememberAppFontFamily()

    var screen by remember { mutableStateOf(AppScreen.LANDING) }
    var status by remember { mutableStateOf("Choose a workspace or create one.") }
    val workspaces = remember { mutableStateListOf<WorkspaceResponse>() }
    var workspace by remember { mutableStateOf<WorkspaceResponse?>(null) }
    var currentMember by remember { mutableStateOf<WorkspaceMemberResponse?>(null) }
    val workspaceMembers = remember { mutableStateListOf<WorkspaceMemberResponse>() }
    val workspaceChannels = remember { mutableStateListOf<ChannelResponse>() }
    var channel by remember { mutableStateOf<ChannelResponse?>(null) }
    val messages = remember { mutableStateListOf<com.retheviper.chat.contract.MessageResponse>() }
    val threadMessages = remember { mutableStateListOf<com.retheviper.chat.contract.MessageResponse>() }
    val unreadNotifications = remember { mutableStateListOf<MentionNotificationResponse>() }
    val allNotifications = remember { mutableStateListOf<MentionNotificationResponse>() }
    val toastNotifications = remember { mutableStateListOf<ToastNotification>() }
    var notificationStreamHandle by remember { mutableStateOf<NotificationStreamHandle?>(null) }
    var selectedRootId by remember { mutableStateOf<String?>(null) }
    var chatRuntime by remember { mutableStateOf<ChatRuntime?>(null) }
    var focusedMessageId by remember { mutableStateOf<String?>(null) }
    var focusedThreadMessageId by remember { mutableStateOf<String?>(null) }
    var centerView by remember { mutableStateOf(WorkspaceCenterView.CHANNEL) }
    var hasOlderMessages by remember { mutableStateOf(false) }
    var loadingOlderMessages by remember { mutableStateOf(false) }
    var scrollToLatestOnNextSnapshot by remember { mutableStateOf(false) }

    var createWorkspaceSlug by remember { mutableStateOf("acme") }
    var createWorkspaceName by remember { mutableStateOf("Acme Product") }
    var createOwnerUserId by remember { mutableStateOf("alice") }
    var createOwnerDisplayName by remember { mutableStateOf("Alice") }

    var joinUserId by remember { mutableStateOf("bob") }
    var joinDisplayName by remember { mutableStateOf("Bob") }

    var channelSlug by remember { mutableStateOf("design") }
    var channelName by remember { mutableStateOf("design") }
    var channelTopic by remember { mutableStateOf("Work in progress and design reviews") }

    var messageBody by remember { mutableStateOf("") }
    var threadMessageBody by remember { mutableStateOf("") }
    var messageLinkPreview by remember { mutableStateOf<LinkPreviewResponse?>(null) }
    var threadLinkPreview by remember { mutableStateOf<LinkPreviewResponse?>(null) }
    var dismissedMessagePreviewUrl by remember { mutableStateOf<String?>(null) }
    var dismissedThreadPreviewUrl by remember { mutableStateOf<String?>(null) }
    var profileDisplayName by remember { mutableStateOf("Bob") }

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
        status = "Choose a workspace or create one."
    }

    suspend fun resolvePreviewForBody(
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

    fun enqueueToast(notification: ToastNotification) {
        if (toastNotifications.none { it.id == notification.id }) {
            toastNotifications.add(0, notification)
            onNotificationEvent(AppNotificationEvent(notification.id, notification.title, notification.body))
        }
        scope.launch {
            delay(4000)
            toastNotifications.removeAll { it.id == notification.id }
        }
    }

    suspend fun refreshNotifications(showToast: Boolean) {
        val member = currentMember ?: return
        val latest = client.listNotifications(member.id, unreadOnly = true)
        val latestAll = client.listNotifications(member.id, unreadOnly = false)
        val previousUnreadIds = unreadNotifications.mapTo(linkedSetOf()) { it.id }
        unreadNotifications.replaceWith(latest)
        allNotifications.replaceWith(latestAll)
        if (showToast) {
            val newUnread = latest.filter { it.id !in previousUnreadIds }
            if (newUnread.isNotEmpty()) {
                newUnread.take(3).forEach { notification ->
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
    }

    suspend fun markNotificationsRead(notificationIds: List<String>) {
        val member = currentMember ?: return
        if (notificationIds.isEmpty()) return
        runCatching {
            client.markNotificationsRead(member.id, notificationIds)
            refreshNotifications(showToast = false)
        }.onFailure {
            status = it.message ?: "Failed to update notifications"
        }
    }

    suspend fun updateCurrentMemberDisplayName(displayName: String) {
        val member = currentMember ?: return
        val updated = client.updateWorkspaceMember(member.id, UpdateWorkspaceMemberRequest(displayName = displayName))
        currentMember = updated
        workspaceMembers.replaceWith(workspaceMembers.map { existing -> if (existing.id == updated.id) updated else existing })
        if (joinUserId == updated.userId) joinDisplayName = updated.displayName
        profileDisplayName = updated.displayName
        status = "Profile updated"
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
        status = "Connecting #${targetChannel.slug}..."

        runCatching {
            val session = client.openChat(targetChannel.id)
            val collector = scope.launch {
                while (true) {
                    when (val event = client.receiveEvent(session)) {
                        is ChatEvent -> {
                            when (event.type) {
                                ChatEventType.SNAPSHOT,
                                ChatEventType.MESSAGE_POSTED,
                                ChatEventType.REPLY_POSTED,
                                ChatEventType.REACTION_UPDATED -> {
                                    if (event.type == ChatEventType.SNAPSHOT) {
                                        hasOlderMessages = event.messages.size >= 50
                                        if (scrollToLatestOnNextSnapshot) {
                                            focusedMessageId = event.messages.lastOrNull()?.id
                                            scrollToLatestOnNextSnapshot = false
                                        }
                                    }
                                    messages.replaceWith(
                                        reduceChatFeed(
                                            current = ChatFeedState(messages.toList()),
                                            event = event
                                        ).messages
                                    )
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
                                ChatEventType.ERROR -> status = event.error?.message ?: "Socket error"
                            }
                        }
                    }
                }
            }
            client.sendCommand(session, ChatCommand(type = ChatCommandType.LOAD_RECENT, limit = 50))
            chatRuntime = ChatRuntime(session, collector)
        }.onSuccess {
            val unreadIds = unreadNotifications.filter { it.channelId == targetChannel.id }.map { it.id }
            markNotificationsRead(unreadIds)
            status = "#${targetChannel.slug} connected"
        }.onFailure {
            status = it.message ?: "Channel connection failed"
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
        status = if (member == null) "Sign in with an existing member or create a new profile." else "Workspace opened"
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
            markNotificationsRead(
                threadNotificationIdsToMarkRead(unreadNotifications, notification.threadRootMessageId)
            )
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
            status = it.message ?: "Failed to load older messages"
        }
        loadingOlderMessages = false
    }

    LaunchedEffect(Unit) {
        runCatching { refreshWorkspaceList() }.onFailure { status = it.message ?: "Failed to load workspaces" }
    }

    DisposableEffect(Unit) {
        onDispose {
            disconnectNotificationStreamAsync()
            scope.launch {
                disconnectChat()
                client.close()
            }
        }
    }

    DisposableEffect(screen, currentMember?.id, workspace?.id) {
        disconnectNotificationStreamAsync()
        if (screen == AppScreen.WORKSPACE && currentMember != null && workspace != null) {
            notificationStreamHandle = NotificationStreamClient.connect(
                baseUrl = PlatformClientConfig.baseUrl,
                memberId = currentMember!!.id,
                onNotificationSignal = {
                    scope.launch {
                        runCatching { refreshNotifications(showToast = true) }
                            .onFailure { status = it.message ?: "Failed to refresh notifications" }
                    }
                },
                onFailure = {
                    scope.launch {
                        status = it.message ?: "Notification stream disconnected"
                    }
                }
            )
        }
        onDispose {
            disconnectNotificationStreamAsync()
        }
    }

    LaunchedEffect(screen, currentMember?.id, workspace?.id, notificationStreamHandle) {
        if (screen != AppScreen.WORKSPACE || currentMember == null || workspace == null) {
            unreadNotifications.clear()
            allNotifications.clear()
            toastNotifications.clear()
            return@LaunchedEffect
        }
        if (notificationStreamHandle != null) {
            runCatching { refreshNotifications(showToast = false) }
                .onFailure { status = it.message ?: "Failed to load notifications" }
            return@LaunchedEffect
        }
        while (true) {
            runCatching { refreshNotifications(showToast = true) }
                .onFailure { status = it.message ?: "Failed to load notifications" }
            delay(5000)
        }
    }

    LaunchedEffect(unreadNotifications.size) {
        onUnreadNotificationCountChange(unreadNotifications.size)
    }

    LaunchedEffect(screen, workspace?.name, channel?.name, currentMember?.displayName, centerView) {
        onWindowTitleChange(
            buildWindowTitle(
                screen = screen,
                workspaceName = workspace?.name,
                channelName = channel?.name,
                memberDisplayName = currentMember?.displayName,
                centerView = centerView
            )
        )
    }

    val darkTheme = isSystemInDarkTheme()
    val palette = rememberCurrentPalette()

    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colors = rememberMaterialColors(darkTheme, palette),
            typography = Typography(defaultFontFamily = appFontFamily)
        ) {
            Surface(modifier = Modifier, color = palette.shell) {
                when (screen) {
                    AppScreen.LANDING -> LandingScreen(
                        workspaces = workspaces,
                        status = status,
                        workspaceSlug = createWorkspaceSlug,
                        workspaceName = createWorkspaceName,
                        ownerUserId = createOwnerUserId,
                        ownerDisplayName = createOwnerDisplayName,
                        onWorkspaceSlugChange = { createWorkspaceSlug = it },
                        onWorkspaceNameChange = { createWorkspaceName = it },
                        onOwnerUserIdChange = { createOwnerUserId = it },
                        onOwnerDisplayNameChange = { createOwnerDisplayName = it },
                        onOpenWorkspace = { target -> scope.launch { openWorkspace(target, member = null) } },
                        onCreateWorkspace = {
                            scope.launch {
                                status = "Creating workspace..."
                                runCatching {
                                    client.createWorkspace(
                                        CreateWorkspaceRequest(
                                            slug = createWorkspaceSlug,
                                            name = createWorkspaceName,
                                            ownerUserId = createOwnerUserId,
                                            ownerDisplayName = createOwnerDisplayName
                                        )
                                    )
                                }.onSuccess { created ->
                                    refreshWorkspaceList()
                                    val owner = client.listWorkspaceMembers(created.id).firstOrNull { it.id == created.ownerMemberId }
                                    openWorkspace(created, owner)
                                    status = "Workspace created with #general"
                                }.onFailure {
                                    status = it.message ?: "Workspace creation failed"
                                }
                            }
                        }
                    )

                    AppScreen.JOIN_WORKSPACE -> JoinWorkspaceScreen(
                        workspace = workspace,
                        existingMembers = workspaceMembers,
                        status = status,
                        userId = joinUserId,
                        displayName = joinDisplayName,
                        onUserIdChange = { joinUserId = it },
                        onDisplayNameChange = { joinDisplayName = it },
                        onBack = {
                            scope.launch {
                                workspace = null
                                currentMember = null
                                workspaceMembers.clear()
                                workspaceChannels.clear()
                                screen = AppScreen.LANDING
                                status = "Choose a workspace or create one."
                            }
                        },
                        onJoin = {
                            val targetWorkspace = workspace ?: return@JoinWorkspaceScreen
                            scope.launch {
                                val joinPlan = planWorkspaceJoin(workspaceMembers, joinUserId, joinDisplayName) ?: return@launch
                                joinPlan.existingMember?.let { member ->
                                    currentMember = member
                                    profileDisplayName = member.displayName
                                    screen = AppScreen.WORKSPACE
                                    status = "Signed in as ${member.displayName}"
                                    workspaceChannels.firstOrNull()?.let { connectChannel(it) }
                                    return@launch
                                }
                                status = "Creating member profile..."
                                runCatching {
                                    client.addWorkspaceMember(targetWorkspace.id, requireNotNull(joinPlan.createRequest))
                                }.onSuccess { member ->
                                    currentMember = member
                                    profileDisplayName = member.displayName
                                    refreshWorkspaceContext(targetWorkspace)
                                    screen = AppScreen.WORKSPACE
                                    status = "Joined as ${member.displayName}"
                                    workspaceChannels.firstOrNull()?.let { connectChannel(it) }
                                }.onFailure {
                                    status = it.message ?: "Join failed"
                                }
                            }
                        },
                        onContinueAsMember = { member ->
                            scope.launch {
                                currentMember = member
                                profileDisplayName = member.displayName
                                screen = AppScreen.WORKSPACE
                                status = "Signed in as ${member.displayName}"
                                workspaceChannels.firstOrNull()?.let { connectChannel(it) }
                            }
                        }
                    )

                    AppScreen.WORKSPACE -> WorkspaceScreen(
                        workspace = workspace,
                        currentMember = currentMember,
                        workspaceMembers = workspaceMembers,
                        status = status,
                        workspaceChannels = workspaceChannels,
                        channelMentionCounts = unreadNotifications.filter { it.kind == NotificationKind.MENTION }.groupingBy { it.channelId }.eachCount(),
                        notificationCount = unreadNotifications.size,
                        centerView = centerView,
                        allNotifications = allNotifications,
                        channel = channel,
                        messages = messages,
                        threadMessages = threadMessages,
                        selectedRootId = selectedRootId,
                        focusedMessageId = focusedMessageId,
                        focusedThreadMessageId = focusedThreadMessageId,
                        channelSlug = channelSlug,
                        channelName = channelName,
                        channelTopic = channelTopic,
                        hasOlderMessages = hasOlderMessages,
                        loadingOlderMessages = loadingOlderMessages,
                        messageBody = messageBody,
                        messageLinkPreview = messageLinkPreview,
                        threadMessageBody = threadMessageBody,
                        threadLinkPreview = threadLinkPreview,
                        profileDisplayName = profileDisplayName,
                        onChannelSlugChange = { channelSlug = it },
                        onChannelNameChange = { channelName = it },
                        onChannelTopicChange = { channelTopic = it },
                        onMessageBodyChange = {
                            messageBody = it
                            scope.launch {
                                resolvePreviewForBody(
                                    body = it,
                                    currentPreview = messageLinkPreview,
                                    dismissedUrl = dismissedMessagePreviewUrl,
                                    onPreviewResolved = { preview -> messageLinkPreview = preview },
                                    onDismissedUrlChange = { dismissedMessagePreviewUrl = it }
                                )
                            }
                        },
                        onDismissMessagePreview = {
                            dismissedMessagePreviewUrl = messageLinkPreview?.url ?: extractFirstUrl(messageBody)
                            messageLinkPreview = null
                        },
                        onThreadMessageBodyChange = {
                            threadMessageBody = it
                            scope.launch {
                                resolvePreviewForBody(
                                    body = it,
                                    currentPreview = threadLinkPreview,
                                    dismissedUrl = dismissedThreadPreviewUrl,
                                    onPreviewResolved = { preview -> threadLinkPreview = preview },
                                    onDismissedUrlChange = { dismissedThreadPreviewUrl = it }
                                )
                            }
                        },
                        onDismissThreadPreview = {
                            dismissedThreadPreviewUrl = threadLinkPreview?.url ?: extractFirstUrl(threadMessageBody)
                            threadLinkPreview = null
                        },
                        onSwitchWorkspace = {
                            resetWorkspaceSession()
                            scope.launch { runCatching { disconnectChat() } }
                            scope.launch {
                                runCatching { refreshWorkspaceList() }
                                    .onFailure { status = it.message ?: "Failed to load workspaces" }
                            }
                        },
                        onOpenChannel = { target -> scope.launch { connectChannel(target) } },
                        onOpenNotifications = {
                            scope.launch {
                                refreshNotifications(showToast = false)
                                centerView = WorkspaceCenterView.NOTIFICATIONS
                            }
                        },
                        onOpenNotificationItem = { notification ->
                            scope.launch {
                                navigateToNotification(
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
                        },
                        onCreateChannel = {
                            val targetWorkspace = workspace ?: return@WorkspaceScreen
                            val member = currentMember ?: return@WorkspaceScreen
                            scope.launch {
                                status = "Creating channel..."
                                runCatching {
                                    client.createChannel(
                                        targetWorkspace.id,
                                        CreateChannelRequest(
                                            slug = channelSlug,
                                            name = channelName,
                                            topic = channelTopic,
                                            visibility = ChannelVisibility.PUBLIC,
                                            createdByMemberId = member.id
                                        )
                                    )
                                }.onSuccess {
                                    refreshWorkspaceContext(targetWorkspace)
                                    connectChannel(it)
                                }.onFailure {
                                    status = it.message ?: "Channel creation failed"
                                }
                            }
                        },
                        onOpenThread = { message ->
                            selectedRootId = message.id
                            threadMessageBody = ""
                            focusedMessageId = message.id
                            focusedThreadMessageId = null
                            scope.launch {
                                loadThread(client, message.id, threadMessages) { status = it }
                                markNotificationsRead(threadNotificationIdsToMarkRead(unreadNotifications, message.id))
                            }
                        },
                        onCloseThread = {
                            selectedRootId = null
                            threadMessages.clear()
                            threadMessageBody = ""
                            focusedThreadMessageId = null
                        },
                        onLoadOlderMessages = { scope.launch { loadOlderMessages() } },
                        onToggleReaction = { message, emoji ->
                            val runtime = chatRuntime ?: return@WorkspaceScreen
                            val member = currentMember ?: return@WorkspaceScreen
                            scope.launch {
                                runCatching {
                                    client.sendCommand(
                                        runtime.session,
                                        ChatCommand(
                                            type = ChatCommandType.TOGGLE_REACTION,
                                            authorMemberId = member.id,
                                            messageId = message.id,
                                            emoji = emoji
                                        )
                                    )
                                }.onFailure { status = it.message ?: "Reaction failed" }
                            }
                        },
                        onSendChannelMessage = {
                            val runtime = chatRuntime ?: return@WorkspaceScreen
                            val member = currentMember ?: return@WorkspaceScreen
                            scope.launch {
                                val command = buildOutgoingChatCommand(
                                    primaryAuthorId = member.id,
                                    body = messageBody,
                                    replyParentMessageId = "",
                                    linkPreview = messageLinkPreview
                                ) ?: return@launch
                                runCatching { client.sendCommand(runtime.session, command) }
                                    .onSuccess {
                                        messageBody = ""
                                        messageLinkPreview = null
                                        dismissedMessagePreviewUrl = null
                                        status = "Message sent"
                                    }.onFailure { status = it.message ?: "Send failed" }
                            }
                        },
                        onSendThreadMessage = {
                            val runtime = chatRuntime ?: return@WorkspaceScreen
                            val member = currentMember ?: return@WorkspaceScreen
                            val rootId = selectedRootId ?: return@WorkspaceScreen
                            scope.launch {
                                val command = buildOutgoingChatCommand(
                                    primaryAuthorId = member.id,
                                    body = threadMessageBody,
                                    replyParentMessageId = rootId,
                                    linkPreview = threadLinkPreview
                                ) ?: return@launch
                                runCatching { client.sendCommand(runtime.session, command) }
                                    .onSuccess {
                                        threadMessageBody = ""
                                        threadLinkPreview = null
                                        dismissedThreadPreviewUrl = null
                                        status = "Reply sent"
                                    }.onFailure { status = it.message ?: "Reply failed" }
                            }
                        },
                        onProfileDisplayNameChange = { profileDisplayName = it },
                        onSaveProfile = {
                            scope.launch {
                                runCatching { updateCurrentMemberDisplayName(profileDisplayName) }
                                    .onFailure { status = it.message ?: "Profile update failed" }
                            }
                        }
                    )
                }

                if (screen == AppScreen.WORKSPACE && toastNotifications.isNotEmpty()) {
                    NotificationOverlay(
                        notifications = toastNotifications,
                        onOpenNotification = { notification -> scope.launch { navigateToNotification(notification) } },
                        onDismiss = { notificationId -> toastNotifications.removeAll { it.id == notificationId } }
                    )
                }
            }
        }
    }
}
