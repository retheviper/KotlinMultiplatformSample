package com.retheviper.chat.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.retheviper.shared.generated.resources.NotoColorEmoji
import com.retheviper.shared.generated.resources.NotoSansJPRegular
import com.retheviper.shared.generated.resources.NotoSansKRRegular
import com.retheviper.shared.generated.resources.Res
import com.retheviper.chat.client.MessagingClient
import com.retheviper.chat.client.PlatformClientConfig
import com.retheviper.chat.contract.AddWorkspaceMemberRequest
import com.retheviper.chat.contract.ChannelResponse
import com.retheviper.chat.contract.ChatCommand
import com.retheviper.chat.contract.ChatCommandType
import com.retheviper.chat.contract.ChatEvent
import com.retheviper.chat.contract.ChatEventType
import com.retheviper.chat.contract.ChannelVisibility
import com.retheviper.chat.contract.CreateChannelRequest
import com.retheviper.chat.contract.CreateWorkspaceRequest
import com.retheviper.chat.contract.LinkPreviewResponse
import com.retheviper.chat.contract.MessageResponse
import com.retheviper.chat.contract.MentionNotificationResponse
import com.retheviper.chat.contract.NotificationKind
import com.retheviper.chat.contract.UpdateWorkspaceMemberRequest
import com.retheviper.chat.contract.WorkspaceMemberResponse
import com.retheviper.chat.contract.WorkspaceResponse
import coil3.compose.AsyncImage
import io.ktor.websocket.close
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font as ResourceFont

private enum class AppScreen {
    LANDING,
    JOIN_WORKSPACE,
    WORKSPACE
}

private enum class WorkspaceCenterView {
    CHANNEL,
    NOTIFICATIONS
}

private val Shell = Color(0xFF1D1624)
private val Sidebar = Color(0xFF3C214B)
private val SidebarCard = Color(0xFF4C2D60)
private val Accent = Color(0xFF7C5CFF)
private val AccentSoft = Color(0xFF8E72FF)
private val MainBg = Color(0xFFF6F4FA)
private val MainCard = Color(0xFFFFFFFF)
private val ThreadBg = Color(0xFFF0ECF7)
private val Border = Color(0xFFE4DDEF)
private val LightText = Color(0xFFF7F2FF)
private val MutedText = Color(0xFFD4C5E2)
private val DarkText = Color(0xFF291F31)
private val DimText = Color(0xFF7E7488)
private val ReactionDefaults = listOf("👍", "❤️", "😂", "🎉", "👀", "🚀")
private val UrlRegex = Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE)

private data class ToastNotification(
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
    val messages = remember { mutableStateListOf<MessageResponse>() }
    val threadMessages = remember { mutableStateListOf<MessageResponse>() }
    val unreadNotifications = remember { mutableStateListOf<MentionNotificationResponse>() }
    val allNotifications = remember { mutableStateListOf<MentionNotificationResponse>() }
    val toastNotifications = remember { mutableStateListOf<ToastNotification>() }
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
        chatRuntime?.close()
        chatRuntime = null
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

        if (currentPreview?.url == url) {
            return
        }

        onDismissedUrlChange(null)
        val preview = runCatching { client.resolveLinkPreview(url).preview }.getOrNull()
        onPreviewResolved(preview)
    }

    fun enqueueToast(notification: ToastNotification) {
        if (toastNotifications.none { it.id == notification.id }) {
            toastNotifications.add(0, notification)
            onNotificationEvent(
                AppNotificationEvent(
                    id = notification.id,
                    title = notification.title,
                    body = notification.body
                )
            )
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
        client.markNotificationsRead(member.id, notificationIds)
        refreshNotifications(showToast = false)
    }

    suspend fun updateCurrentMemberDisplayName(displayName: String) {
        val member = currentMember ?: return
        val updated = client.updateWorkspaceMember(
            memberId = member.id,
            request = UpdateWorkspaceMemberRequest(displayName = displayName)
        )
        currentMember = updated
        workspaceMembers.replaceWith(
            workspaceMembers.map { existing ->
                if (existing.id == updated.id) updated else existing
            }
        )
        if (joinUserId == updated.userId) {
            joinDisplayName = updated.displayName
        }
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

                                ChatEventType.ERROR -> {
                                    status = event.error?.message ?: "Socket error"
                                }
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
        status = if (member == null) {
            "Sign in with an existing member or create a new profile."
        } else {
            "Workspace opened"
        }
        if (member != null) {
            refreshNotifications(showToast = false)
            workspaceChannels.firstOrNull()?.let { connectChannel(it) }
        }
    }

    suspend fun navigateToNotification(notification: ToastNotification) {
        val targetChannel = workspaceChannels.firstOrNull { it.id == notification.channelId } ?: return
        if (channel?.id != targetChannel.id) {
            connectChannel(targetChannel)
        }

        if (notification.threadRootMessageId != null) {
            selectedRootId = notification.threadRootMessageId
            focusedMessageId = notification.threadRootMessageId
            focusedThreadMessageId = notification.messageId
            loadThread(client, notification.threadRootMessageId, threadMessages) { status = it }
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
            client.listChannelMessages(
                channelId = targetChannel.id,
                limit = 50,
                beforeMessageId = oldestMessageId
            ).messages
        }.onSuccess { olderMessages ->
            if (olderMessages.isNotEmpty()) {
                messages.replaceWith(olderMessages + messages)
            }
            hasOlderMessages = olderMessages.size >= 50
        }.onFailure {
            status = it.message ?: "Failed to load older messages"
        }
        loadingOlderMessages = false
    }

    LaunchedEffect(Unit) {
        runCatching { refreshWorkspaceList() }
            .onFailure { status = it.message ?: "Failed to load workspaces" }
    }

    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                disconnectChat()
                client.close()
            }
        }
    }

    LaunchedEffect(screen, currentMember?.id, workspace?.id) {
        if (screen != AppScreen.WORKSPACE || currentMember == null || workspace == null) {
            unreadNotifications.clear()
            allNotifications.clear()
            toastNotifications.clear()
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
            buildDesktopWindowTitle(
                screen = screen,
                workspaceName = workspace?.name,
                channelName = channel?.name,
                memberDisplayName = currentMember?.displayName,
                centerView = centerView
            )
        )
    }

    MaterialTheme(typography = Typography(defaultFontFamily = appFontFamily)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Shell) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (screen) {
                    AppScreen.LANDING -> {
                        LandingScreen(
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
                            onOpenWorkspace = { target ->
                                scope.launch { openWorkspace(target, member = null) }
                            },
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
                    }

                    AppScreen.JOIN_WORKSPACE -> {
                        JoinWorkspaceScreen(
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
                                val joinPlan = planWorkspaceJoin(
                                    members = workspaceMembers,
                                    userId = joinUserId,
                                    displayName = joinDisplayName
                                ) ?: return@launch

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
                                    client.addWorkspaceMember(
                                        targetWorkspace.id,
                                        requireNotNull(joinPlan.createRequest)
                                    )
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
                    }

                    AppScreen.WORKSPACE -> {
                        WorkspaceScreen(
                        workspace = workspace,
                        currentMember = currentMember,
                        workspaceMembers = workspaceMembers,
                        status = status,
                        workspaceChannels = workspaceChannels,
                        channelMentionCounts = unreadNotifications
                            .filter { it.kind == NotificationKind.MENTION }
                            .groupingBy { it.channelId }
                            .eachCount(),
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
                        onSwitchWorkspace = {
                            scope.launch {
                                disconnectChat()
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
                                messageLinkPreview = null
                                threadLinkPreview = null
                                dismissedMessagePreviewUrl = null
                                dismissedThreadPreviewUrl = null
                                status = "Choose a workspace or create one."
                                refreshWorkspaceList()
                            }
                        },
                        onOpenChannel = { target ->
                            scope.launch { connectChannel(target) }
                        },
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
                            }
                        },
                        onCloseThread = {
                            selectedRootId = null
                            threadMessages.clear()
                            threadMessageBody = ""
                            focusedThreadMessageId = null
                        },
                        onLoadOlderMessages = {
                            scope.launch { loadOlderMessages() }
                        },
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
                                }.onFailure {
                                    status = it.message ?: "Reaction failed"
                                }
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
                                    }.onFailure {
                                        status = it.message ?: "Send failed"
                                    }
                            }
                        },
                        threadMessageBody = threadMessageBody,
                        threadLinkPreview = threadLinkPreview,
                        profileDisplayName = profileDisplayName,
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
                                    }.onFailure {
                                        status = it.message ?: "Reply failed"
                                    }
                            }
                        },
                        onProfileDisplayNameChange = { profileDisplayName = it },
                        onSaveProfile = {
                            scope.launch {
                                runCatching {
                                    updateCurrentMemberDisplayName(profileDisplayName)
                                }.onFailure {
                                    status = it.message ?: "Profile update failed"
                                }
                            }
                        }
                    )
                }
                }

                if (screen == AppScreen.WORKSPACE && toastNotifications.isNotEmpty()) {
                    NotificationOverlay(
                        notifications = toastNotifications,
                        onOpenNotification = { notification ->
                            scope.launch { navigateToNotification(notification) }
                        },
                        onDismiss = { notificationId ->
                            toastNotifications.removeAll { it.id == notificationId }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LandingScreen(
    workspaces: List<WorkspaceResponse>,
    status: String,
    workspaceSlug: String,
    workspaceName: String,
    ownerUserId: String,
    ownerDisplayName: String,
    onWorkspaceSlugChange: (String) -> Unit,
    onWorkspaceNameChange: (String) -> Unit,
    onOwnerUserIdChange: (String) -> Unit,
    onOwnerDisplayNameChange: (String) -> Unit,
    onOpenWorkspace: (WorkspaceResponse) -> Unit,
    onCreateWorkspace: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 1040.dp)
                .height(720.dp)
                .animateContentSize(),
            shape = RoundedCornerShape(32.dp),
            backgroundColor = Sidebar,
            elevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Text("Chat Workspace", style = MaterialTheme.typography.h3, color = LightText)
                Text(status, color = MutedText)
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    SplitPanelCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        title = "Join",
                        subtitle = "Choose an existing workspace",
                        dark = true
                    ) {
                        if (workspaces.isEmpty()) {
                            EmptyConversationState("No workspaces yet", "Create the first one to get started.")
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(workspaces, key = { it.id }) { item ->
                                    WorkspaceListItem(item, onClick = { onOpenWorkspace(item) })
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MutedText.copy(alpha = 0.22f))
                    )

                    SplitPanelCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        title = "Create",
                        subtitle = "A #general channel is created automatically.",
                        dark = false
                    ) {
                        FormField("Workspace slug", workspaceSlug, textColor = DarkText, onValueChange = onWorkspaceSlugChange)
                        FormField("Workspace name", workspaceName, textColor = DarkText, onValueChange = onWorkspaceNameChange)
                        FormField("Owner user id", ownerUserId, textColor = DarkText, onValueChange = onOwnerUserIdChange)
                        FormField("Owner display name", ownerDisplayName, textColor = DarkText, onValueChange = onOwnerDisplayNameChange)
                        Spacer(modifier = Modifier.weight(1f))
                        FilledActionButton("Create workspace", onCreateWorkspace, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitPanelCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    dark: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.widthIn(min = 0.dp),
        shape = RoundedCornerShape(26.dp),
        backgroundColor = if (dark) SidebarCard else MainBg,
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = {
                Text(title, style = MaterialTheme.typography.h5, color = if (dark) LightText else DarkText)
                Text(subtitle, color = if (dark) MutedText else DimText)
                content()
            }
        )
    }
}

@Composable
private fun SplitPanelHeader(
    title: String,
    subtitle: String,
    dark: Boolean,
    backLabel: String,
    onBack: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.h4, color = if (dark) DarkText else LightText)
            Text(subtitle, color = if (dark) DimText else MutedText)
        }
        Card(
            modifier = Modifier.clickable(onClick = onBack),
            shape = RoundedCornerShape(14.dp),
            backgroundColor = if (dark) Color(0xFFEAE3F7) else SidebarCard,
            elevation = 0.dp
        ) {
            Text(
                backLabel,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = if (dark) DarkText else LightText
            )
        }
    }
}

@Composable
private fun JoinWorkspaceScreen(
    workspace: WorkspaceResponse?,
    existingMembers: List<WorkspaceMemberResponse>,
    status: String,
    userId: String,
    displayName: String,
    onUserIdChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onJoin: () -> Unit,
    onContinueAsMember: (WorkspaceMemberResponse) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 980.dp)
                .height(680.dp)
                .animateContentSize(),
            shape = RoundedCornerShape(32.dp),
            backgroundColor = Sidebar,
            elevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                SplitPanelHeader(
                    title = "Join ${workspace?.name ?: "workspace"}",
                    subtitle = status,
                    dark = false,
                    backLabel = "Back",
                    onBack = onBack
                )
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    SplitPanelCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        title = "Select",
                        subtitle = "Continue as an existing member",
                        dark = true
                    ) {
                        if (existingMembers.isEmpty()) {
                            EmptyConversationState("No members yet", "Create the first member profile on the right.")
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(existingMembers, key = { it.id }) { member ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable { onContinueAsMember(member) },
                                        shape = RoundedCornerShape(16.dp),
                                        backgroundColor = Sidebar,
                                        elevation = 0.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(member.displayName, color = LightText, fontWeight = FontWeight.Bold)
                                                Text("@${member.userId}", color = MutedText)
                                            }
                                            Text("Continue", color = AccentSoft, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MutedText.copy(alpha = 0.22f))
                    )

                    SplitPanelCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        title = "Create",
                        subtitle = "Pick the identity you will use in this workspace.",
                        dark = false
                    ) {
                        FormField("User id", userId, textColor = DarkText, onValueChange = onUserIdChange)
                        FormField("Display name", displayName, textColor = DarkText, onValueChange = onDisplayNameChange)
                        Spacer(modifier = Modifier.weight(1f))
                        FilledActionButton("Continue", onJoin, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceScreen(
    workspace: WorkspaceResponse?,
    currentMember: WorkspaceMemberResponse?,
    workspaceMembers: List<WorkspaceMemberResponse>,
    status: String,
    workspaceChannels: List<ChannelResponse>,
    channelMentionCounts: Map<String, Int>,
    notificationCount: Int,
    centerView: WorkspaceCenterView,
    allNotifications: List<MentionNotificationResponse>,
    channel: ChannelResponse?,
    messages: List<MessageResponse>,
    threadMessages: List<MessageResponse>,
    selectedRootId: String?,
    focusedMessageId: String?,
    focusedThreadMessageId: String?,
    channelSlug: String,
    channelName: String,
    channelTopic: String,
    hasOlderMessages: Boolean,
    loadingOlderMessages: Boolean,
    messageBody: String,
    messageLinkPreview: LinkPreviewResponse?,
    threadMessageBody: String,
    threadLinkPreview: LinkPreviewResponse?,
    profileDisplayName: String,
    onChannelSlugChange: (String) -> Unit,
    onChannelNameChange: (String) -> Unit,
    onChannelTopicChange: (String) -> Unit,
    onMessageBodyChange: (String) -> Unit,
    onDismissMessagePreview: () -> Unit,
    onThreadMessageBodyChange: (String) -> Unit,
    onDismissThreadPreview: () -> Unit,
    onSwitchWorkspace: () -> Unit,
    onOpenChannel: (ChannelResponse) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenNotificationItem: (MentionNotificationResponse) -> Unit,
    onCreateChannel: () -> Unit,
    onOpenThread: (MessageResponse) -> Unit,
    onCloseThread: () -> Unit,
    onLoadOlderMessages: () -> Unit,
    onToggleReaction: (MessageResponse, String) -> Unit,
    onSendChannelMessage: () -> Unit,
    onSendThreadMessage: () -> Unit,
    onProfileDisplayNameChange: (String) -> Unit,
    onSaveProfile: () -> Unit
) {
    var createChannelDialogOpen by remember { mutableStateOf(false) }
    var reactionPickerTarget by remember { mutableStateOf<MessageResponse?>(null) }
    var editProfileDialogOpen by remember { mutableStateOf(false) }
    val messageListState = rememberLazyListState()
    val threadListState = rememberLazyListState()

    LaunchedEffect(focusedMessageId, messages.size) {
        val targetId = focusedMessageId ?: return@LaunchedEffect
        val index = messages.indexOfFirst { it.id == targetId }
        if (index >= 0) {
            messageListState.animateScrollToItem(index)
        }
    }

    LaunchedEffect(focusedThreadMessageId, threadMessages.size) {
        val targetId = focusedThreadMessageId ?: return@LaunchedEffect
        val index = threadMessages.indexOfFirst { it.id == targetId }
        if (index >= 0) {
            threadListState.animateScrollToItem(index)
        }
    }

    Row(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.width(300.dp).fillMaxHeight(),
            shape = RoundedCornerShape(28.dp),
            backgroundColor = Sidebar,
            elevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(workspace?.name ?: "Workspace", style = MaterialTheme.typography.h5, color = LightText)
                        Card(
                            modifier = Modifier.clickable(onClick = onSwitchWorkspace),
                            shape = CircleShape,
                            backgroundColor = SidebarCard,
                            elevation = 0.dp
                        ) {
                            Text(
                                "<>",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                color = LightText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = SidebarCard,
                        elevation = 0.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SidebarMenuItem(
                                label = "Notifications",
                                badge = notificationCount,
                                selected = centerView == WorkspaceCenterView.NOTIFICATIONS,
                                onClick = onOpenNotifications
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Channels", color = LightText, fontWeight = FontWeight.Bold)
                                Card(
                                    modifier = Modifier.clickable { createChannelDialogOpen = true },
                                    shape = RoundedCornerShape(12.dp),
                                    backgroundColor = Accent,
                                    elevation = 0.dp
                                ) {
                                    Text(
                                        "+",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            workspaceChannels.forEach { item ->
                                SidebarChannelItem(
                                    channel = item,
                                    mentionCount = channelMentionCounts[item.id] ?: 0,
                                    selected = item.id == channel?.id,
                                    onClick = { onOpenChannel(item) }
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { editProfileDialogOpen = true },
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = SidebarCard,
                    elevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(currentMember?.displayName ?: "No member", color = LightText, fontWeight = FontWeight.Bold)
                            Text(currentMember?.userId ?: "", color = MutedText)
                        }
                        Text("Edit", color = AccentSoft, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = MainCard,
                elevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(channel?.let { "# ${it.slug}" } ?: "Choose a channel", style = MaterialTheme.typography.h5, color = DarkText)
                        Text(channel?.topic ?: "Select a channel to start chatting.", color = DimText)
                    }
                    Column {
                        Text(currentMember?.displayName ?: "No member", fontWeight = FontWeight.Bold, color = DarkText)
                        Text(currentMember?.userId ?: "", color = DimText)
                    }
                }
            }

            Card(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = MainCard,
                elevation = 0.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (centerView == WorkspaceCenterView.NOTIFICATIONS) {
                            NotificationListPanel(
                                notifications = allNotifications,
                                onOpenNotification = onOpenNotificationItem
                            )
                        } else if (channel == null) {
                            EmptyConversationState("Open a channel", "Pick #general or another channel from the sidebar.")
                        } else {
                            LazyColumn(
                                state = messageListState,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (hasOlderMessages) {
                                    item(key = "read-more") {
                                        OutlineActionButton(
                                            text = if (loadingOlderMessages) "Loading..." else "Read more",
                                            onClick = onLoadOlderMessages,
                                            modifier = Modifier.fillMaxWidth(),
                                            contentColor = Accent
                                        )
                                    }
                                }
                                items(messages, key = { it.id }) { item ->
                                    MessageRow(
                                        message = item,
                                        selected = selectedRootId == item.id || focusedMessageId == item.id,
                                        currentMemberId = currentMember?.id,
                                        mentionUserIds = workspaceMembers.mapTo(linkedSetOf()) { it.userId },
                                        onOpenThread = { onOpenThread(item) },
                                        onToggleReaction = { emoji -> onToggleReaction(item, emoji) },
                                        onOpenReactionPicker = { reactionPickerTarget = item }
                                    )
                                }
                            }
                            ComposerPanel(
                                currentMember = currentMember,
                                mentionCandidates = workspaceMembers,
                                messageBody = messageBody,
                                linkPreview = messageLinkPreview,
                                onMessageBodyChange = onMessageBodyChange,
                                onDismissPreview = onDismissMessagePreview,
                                onFocus = onCloseThread,
                                onSend = onSendChannelMessage
                            )
                        }
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd).fillMaxHeight(),
                        visible = selectedRootId != null,
                        enter = slideInHorizontally(initialOffsetX = { it / 3 }) + fadeIn(),
                        exit = slideOutHorizontally(targetOffsetX = { it / 3 }) + fadeOut()
                    ) {
                        ThreadPane(
                            currentMember = currentMember,
                            workspaceMembers = workspaceMembers,
                            threadMessages = threadMessages,
                            listState = threadListState,
                            focusedThreadMessageId = focusedThreadMessageId,
                            messageBody = threadMessageBody,
                            linkPreview = threadLinkPreview,
                            onMessageBodyChange = onThreadMessageBodyChange,
                            onDismissPreview = onDismissThreadPreview,
                            onCloseThread = onCloseThread,
                            onToggleReaction = onToggleReaction,
                            onOpenReactionPicker = { reactionPickerTarget = it },
                            onSend = onSendThreadMessage
                        )
                    }
                }
            }
        }
    }

    if (createChannelDialogOpen) {
        AlertDialog(
            onDismissRequest = { createChannelDialogOpen = false },
            title = {
                Text("Create a channel", color = DarkText)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField("Slug", channelSlug, textColor = DarkText, onValueChange = onChannelSlugChange)
                    FormField("Name", channelName, textColor = DarkText, onValueChange = onChannelNameChange)
                    FormField("Topic", channelTopic, textColor = DarkText, onValueChange = onChannelTopicChange)
                }
            },
            confirmButton = {
                FilledActionButton(
                    "Create",
                    onClick = {
                        createChannelDialogOpen = false
                        onCreateChannel()
                    }
                )
            },
            dismissButton = {
                OutlineActionButton(
                    "Cancel",
                    onClick = { createChannelDialogOpen = false },
                    contentColor = DarkText
                )
            },
            shape = RoundedCornerShape(24.dp),
            backgroundColor = MainCard
        )
    }

    if (editProfileDialogOpen) {
        AlertDialog(
            onDismissRequest = { editProfileDialogOpen = false },
            title = {
                Text("Update profile", color = DarkText)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField("Display name", profileDisplayName, textColor = DarkText, onValueChange = onProfileDisplayNameChange)
                    currentMember?.userId?.let { userId ->
                        Text("User ID: $userId", color = DimText)
                    }
                }
            },
            confirmButton = {
                FilledActionButton(
                    "Save",
                    onClick = {
                        editProfileDialogOpen = false
                        onSaveProfile()
                    }
                )
            },
            dismissButton = {
                OutlineActionButton(
                    "Cancel",
                    onClick = {
                        onProfileDisplayNameChange(currentMember?.displayName.orEmpty())
                        editProfileDialogOpen = false
                    },
                    contentColor = DarkText
                )
            },
            shape = RoundedCornerShape(24.dp),
            backgroundColor = MainCard
        )
    }

    reactionPickerTarget?.let { message ->
        EmojiPickerDialog(
            onDismiss = { reactionPickerTarget = null },
            onSelect = { emoji ->
                reactionPickerTarget = null
                onToggleReaction(message, emoji)
            }
        )
    }
}

@Composable
private fun ThreadPane(
    currentMember: WorkspaceMemberResponse?,
    workspaceMembers: List<WorkspaceMemberResponse>,
    threadMessages: List<MessageResponse>,
    listState: LazyListState,
    focusedThreadMessageId: String?,
    messageBody: String,
    linkPreview: LinkPreviewResponse?,
    onMessageBodyChange: (String) -> Unit,
    onDismissPreview: () -> Unit,
    onCloseThread: () -> Unit,
    onToggleReaction: (MessageResponse, String) -> Unit,
    onOpenReactionPicker: (MessageResponse) -> Unit,
    onSend: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(420.dp)
            .fillMaxHeight()
            .padding(14.dp),
        shape = RoundedCornerShape(28.dp),
        backgroundColor = ThreadBg.copy(alpha = 0.98f),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Thread", style = MaterialTheme.typography.h6, color = DarkText)
                Text("Close", color = Accent, modifier = Modifier.clickable(onClick = onCloseThread))
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(threadMessages, key = { _, item -> item.id }) { index, item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = if (index == 0) 0.dp else 18.dp),
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = if (focusedThreadMessageId == item.id) Color(0xFFEDE7FB) else Color.White,
                        elevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(item.authorDisplayName, fontWeight = FontWeight.Bold, color = DarkText)
                            MessageBodyText(
                                text = item.body,
                                mentionUserIds = workspaceMembers.mapTo(linkedSetOf()) { it.userId }
                            )
                            item.linkPreview?.let { LinkPreviewCard(preview = it) }
                            ReactionBar(
                                reactions = item.reactions,
                                currentMemberId = currentMember?.id,
                                onToggleReaction = { emoji -> onToggleReaction(item, emoji) },
                                onOpenReactionPicker = { onOpenReactionPicker(item) }
                            )
                        }
                    }
                }
            }
            ComposerPanel(
                currentMember = currentMember,
                mentionCandidates = workspaceMembers,
                messageBody = messageBody,
                linkPreview = linkPreview,
                onMessageBodyChange = onMessageBodyChange,
                onDismissPreview = onDismissPreview,
                onSend = onSend,
                sendLabel = "Reply in thread",
                placeholder = "Reply to this thread"
            )
        }
    }
}

private suspend fun loadThread(
    client: MessagingClient,
    rootId: String?,
    threadMessages: MutableList<MessageResponse>,
    updateStatus: (String) -> Unit
) {
    if (rootId == null) return
    runCatching { client.getThread(rootId) }
        .onSuccess {
            threadMessages.replaceWith(toThreadMessages(it))
            updateStatus("Thread loaded")
        }
        .onFailure {
            updateStatus(it.message ?: "Failed to load thread")
        }
}

private data class ChatRuntime(
    val session: io.ktor.client.plugins.websocket.DefaultClientWebSocketSession,
    val collector: Job
) {
    suspend fun close() {
        collector.cancel()
        session.close()
    }
}

@Composable
private fun WorkspaceListItem(workspace: WorkspaceResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Sidebar,
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(workspace.name, color = LightText, fontWeight = FontWeight.Bold)
                Text(workspace.slug, color = MutedText)
            }
            Text("Open", color = AccentSoft, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SidebarPanelCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = SidebarCard,
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = LightText, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun SidebarChannelItem(
    channel: ChannelResponse,
    mentionCount: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) AccentSoft else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("# ${channel.slug}", color = LightText, fontWeight = FontWeight.SemiBold)
                Text(channel.topic ?: "No topic", color = MutedText)
            }
            if (mentionCount > 0) {
                Text("[${mentionCount}]", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SidebarMenuItem(
    label: String,
    badge: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) AccentSoft else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = LightText, fontWeight = FontWeight.SemiBold)
        if (badge > 0) {
            Text("[${badge}]", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MessageRow(
    message: MessageResponse,
    selected: Boolean,
    currentMemberId: String?,
    mentionUserIds: Set<String>,
    onOpenThread: () -> Unit,
    onToggleReaction: (String) -> Unit,
    onOpenReactionPicker: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenThread),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = if (selected) Color(0xFFEEE8FB) else Color(0xFFF9F7FC),
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(message.authorDisplayName, fontWeight = FontWeight.Bold, color = DarkText)
                Text(shortTimestamp(message.createdAt), color = DimText)
            }
            MessageBodyText(text = message.body, mentionUserIds = mentionUserIds)
            message.linkPreview?.let { LinkPreviewCard(preview = it) }
            ReactionBar(
                reactions = message.reactions,
                currentMemberId = currentMemberId,
                onToggleReaction = onToggleReaction,
                onOpenReactionPicker = onOpenReactionPicker
            )
            if (message.threadReplyCount > 0) {
                Text(
                    "${message.threadReplyCount} repl${if (message.threadReplyCount == 1) "y" else "ies"}",
                    color = Accent,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ComposerPanel(
    currentMember: WorkspaceMemberResponse?,
    mentionCandidates: List<WorkspaceMemberResponse>,
    messageBody: String,
    linkPreview: LinkPreviewResponse?,
    onMessageBodyChange: (String) -> Unit,
    onDismissPreview: () -> Unit,
    onFocus: () -> Unit = {},
    onSend: () -> Unit,
    sendLabel: String = "Send",
    placeholder: String = "Message"
) {
    val suggestions = suggestMentionCandidates(mentionCandidates, messageBody)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = Color(0xFFF4F0FA),
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (suggestions.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    backgroundColor = Color.White,
                    elevation = 0.dp
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        suggestions.forEach { member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onMessageBodyChange(applyMentionCandidate(messageBody, member.userId)) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(member.displayName, color = DarkText, fontWeight = FontWeight.Medium)
                                Text("@${member.userId}", color = DimText)
                            }
                        }
                    }
                }
            }
            linkPreview?.let { preview ->
                ComposerLinkPreviewCard(preview = preview, onDismiss = onDismissPreview)
            }
            FormField(
                label = placeholder,
                value = messageBody,
                minLines = 4,
                textColor = DarkText,
                onValueChange = onMessageBodyChange,
                onFocus = onFocus
            )
            FilledActionButton(sendLabel, onSend, modifier = Modifier.fillMaxWidth(), enabled = currentMember != null && messageBody.isNotBlank())
        }
    }
}

@Composable
private fun ComposerLinkPreviewCard(
    preview: LinkPreviewResponse,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color.White,
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(preview.siteName ?: "Link preview", color = DarkText, fontWeight = FontWeight.SemiBold)
                Text("x", color = Accent, modifier = Modifier.clickable(onClick = onDismiss))
            }
            LinkPreviewImage(preview)
            LinkPreviewSummary(preview)
        }
    }
}

@Composable
private fun LinkPreviewCard(preview: LinkPreviewResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color(0xFFF3F0FA),
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LinkPreviewImage(preview)
            LinkPreviewSummary(preview)
        }
    }
}

@Composable
private fun LinkPreviewImage(preview: LinkPreviewResponse) {
    val imageUrl = preview.imageUrl?.takeIf { it.isNotBlank() } ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color(0xFFEAE4F6),
        elevation = 0.dp
    ) {
        AsyncImage(
            model = previewImageProxyUrl(imageUrl),
            contentDescription = preview.title ?: preview.siteName ?: "Link preview image",
            modifier = Modifier.fillMaxWidth().height(180.dp),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun LinkPreviewSummary(preview: LinkPreviewResponse) {
    if (!preview.siteName.isNullOrBlank()) {
        Text(preview.siteName, color = Accent, fontWeight = FontWeight.SemiBold)
    }
    if (!preview.title.isNullOrBlank()) {
        Text(preview.title, color = DarkText, fontWeight = FontWeight.Bold)
    }
    if (!preview.description.isNullOrBlank()) {
        Text(preview.description, color = DimText)
    }
}

@Composable
private fun MessageBodyText(
    text: String,
    mentionUserIds: Set<String>
) {
    val annotated = buildMessageAnnotatedText(text, mentionUserIds)
    Text(
        text = annotated,
        style = MaterialTheme.typography.body1.copy(color = Color(0xFF463B4F))
    )
}

@Composable
private fun ReactionBar(
    reactions: List<com.retheviper.chat.contract.MessageReactionResponse>,
    currentMemberId: String?,
    onToggleReaction: (String) -> Unit,
    onOpenReactionPicker: () -> Unit
) {
    val emojiFontFamily = rememberEmojiFontFamily()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        reactions.forEach { reaction ->
            val selected = currentMemberId != null && currentMemberId in reaction.memberIds
            Card(
                modifier = Modifier.clickable { onToggleReaction(reaction.emoji) },
                shape = RoundedCornerShape(999.dp),
                backgroundColor = if (selected) Color(0xFFE9DEFF) else Color(0xFFF1ECF8),
                elevation = 0.dp
            ) {
                Text(
                    "${reactionDisplayLabel(reaction.emoji)} ${reaction.count}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = if (selected) Accent else DarkText,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = emojiFontFamily
                )
            }
        }
        Card(
            modifier = Modifier.clickable(onClick = onOpenReactionPicker),
            shape = RoundedCornerShape(999.dp),
            backgroundColor = Color(0xFFF1ECF8),
            elevation = 0.dp
        ) {
            Text(
                "+",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                color = Accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmojiPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val emojiFontFamily = rememberEmojiFontFamily()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add reaction", color = DarkText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ReactionDefaults.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { emoji ->
                            Card(
                                modifier = Modifier.weight(1f).clickable { onSelect(emoji) },
                                shape = RoundedCornerShape(16.dp),
                                backgroundColor = Color(0xFFF6F3FB),
                                elevation = 0.dp
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                        Text(
                                            reactionDisplayLabel(emoji),
                                            color = DarkText,
                                            style = MaterialTheme.typography.h6,
                                            fontFamily = emojiFontFamily
                                        )
                                    }
                                }
                        }
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlineActionButton(
                "Close",
                onClick = onDismiss,
                contentColor = DarkText
            )
        },
        shape = RoundedCornerShape(24.dp),
        backgroundColor = MainCard
    )
}

@Composable
private fun NotificationListPanel(
    notifications: List<MentionNotificationResponse>,
    onOpenNotification: (MentionNotificationResponse) -> Unit
) {
    if (notifications.isEmpty()) {
        EmptyConversationState("No notifications", "Mentions and thread replies will show up here.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(notifications, key = { it.id }) { notification ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onOpenNotification(notification) },
                shape = RoundedCornerShape(16.dp),
                backgroundColor = if (notification.readAt == null) Color(0xFFF4EEFF) else Color(0xFFF9F7FC),
                elevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(notificationTitle(notification), color = DarkText, fontWeight = FontWeight.Bold)
                    Text(notification.messagePreview, color = Color(0xFF463B4F))
                    Text(shortTimestamp(notification.createdAt), color = DimText)
                }
            }
        }
    }
}

private fun notificationTitle(notification: MentionNotificationResponse): String {
    return when (notification.kind) {
        NotificationKind.MENTION -> notification.authorDisplayName
        NotificationKind.THREAD_ACTIVITY -> "${notification.authorDisplayName} replied in a thread"
    }
}

private fun buildMessageAnnotatedText(text: String, mentionUserIds: Set<String>) = buildAnnotatedString {
    val tokenRegex = Regex("""https?://[^\s]+|@([a-zA-Z0-9._-]+)""", RegexOption.IGNORE_CASE)
    var currentIndex = 0

    tokenRegex.findAll(text).forEach { match ->
        val range = match.range
        if (range.first > currentIndex) {
            append(text.substring(currentIndex, range.first))
        }

        val token = match.value
        if (token.startsWith("http://", ignoreCase = true) || token.startsWith("https://", ignoreCase = true)) {
            val normalizedUrl = token.trimEnd('.', ',', ';', ':', ')', ']', '!')
            pushLink(
                LinkAnnotation.Url(
                    url = normalizedUrl,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = Accent,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                )
            )
            append(normalizedUrl)
            pop()
            if (normalizedUrl.length != token.length) {
                append(token.removePrefix(normalizedUrl))
            }
        } else {
            val userId = match.groupValues.getOrNull(1).orEmpty()
            if (userId in mentionUserIds) {
                pushStyle(
                    SpanStyle(
                        color = Accent,
                        background = Color(0xFFE8DEFF),
                        fontWeight = FontWeight.SemiBold
                    )
                )
                append(token)
                pop()
            } else {
                append(token)
            }
        }

        currentIndex = range.last + 1
    }

    if (currentIndex < text.length) {
        append(text.substring(currentIndex))
    }
}

private fun extractFirstUrl(text: String): String? {
    return UrlRegex.find(text)
        ?.value
        ?.trimEnd('.', ',', ';', ':', ')', ']', '!')
        ?.takeIf { it.isNotBlank() }
}

private fun reactionDisplayLabel(value: String): String {
    return when (value) {
        ":+1:" -> "👍"
        ":heart:" -> "❤️"
        ":joy:" -> "😂"
        ":tada:" -> "🎉"
        ":eyes:" -> "👀"
        ":rocket:" -> "🚀"
        else -> value
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun rememberEmojiFontFamily(): FontFamily {
    return FontFamily(ResourceFont(Res.font.NotoColorEmoji))
}

@Composable
private fun rememberAppFontFamily(): FontFamily {
    return FontFamily(
        ResourceFont(Res.font.NotoSansJPRegular),
        ResourceFont(Res.font.NotoSansKRRegular)
    )
}

private fun previewImageProxyUrl(imageUrl: String): String {
    return "${PlatformClientConfig.baseUrl}/api/v1/link-preview/image?url=${imageUrl.encodeURLParameter()}"
}

private fun buildDesktopWindowTitle(
    screen: AppScreen,
    workspaceName: String?,
    channelName: String?,
    memberDisplayName: String?,
    centerView: WorkspaceCenterView
): String {
    return when (screen) {
        AppScreen.LANDING -> "Chat Desktop"
        AppScreen.JOIN_WORKSPACE -> listOfNotNull(workspaceName, "Join").joinToString(" • ").ifBlank { "Chat Desktop" }
        AppScreen.WORKSPACE -> when (centerView) {
            WorkspaceCenterView.CHANNEL -> listOfNotNull(
                channelName?.let { "#$it" },
                workspaceName,
                memberDisplayName
            ).joinToString(" • ").ifBlank { "Chat Desktop" }
            WorkspaceCenterView.NOTIFICATIONS -> listOfNotNull(
                "Notifications",
                workspaceName,
                memberDisplayName
            ).joinToString(" • ").ifBlank { "Chat Desktop" }
        }
    }
}

@Composable
private fun NotificationOverlay(
    notifications: List<ToastNotification>,
    onOpenNotification: (ToastNotification) -> Unit,
    onDismiss: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, end = 18.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.widthIn(max = 360.dp),
            shape = RoundedCornerShape(20.dp),
            backgroundColor = MainCard,
            elevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                notifications.forEach { notification ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenNotification(notification) },
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = Color(0xFFF6F3FB),
                        elevation = 0.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(notification.title, color = DarkText, fontWeight = FontWeight.SemiBold)
                                Text("x", color = Accent, modifier = Modifier.clickable { onDismiss(notification.id) })
                            }
                            Text(notification.body, color = DimText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyConversationState(title: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.h6, color = DarkText)
        Text(body, color = DimText)
    }
}

@Composable
private fun FilledActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Accent,
            contentColor = Color.White,
            disabledBackgroundColor = Border,
            disabledContentColor = DimText
        ),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp)
    ) {
        Text(text)
    }
}

@Composable
private fun OutlineActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = LightText
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color.Transparent,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp)
    ) {
        Text(text)
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    minLines: Int = 1,
    textColor: Color = DarkText,
    labelColor: Color = DimText,
    onValueChange: (String) -> Unit,
    onFocus: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) onFocus() },
        label = { Text(label, color = labelColor) },
        minLines = minLines,
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.body1.copy(color = textColor)
    )
}

private fun shortTimestamp(createdAt: String): String {
    return createdAt.drop(11).take(5).ifBlank { createdAt }
}

private fun <T> MutableList<T>.replaceWith(items: List<T>) {
    clear()
    addAll(items)
}
