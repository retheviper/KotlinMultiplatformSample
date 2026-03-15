package com.retheviper.chat.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.retheviper.chat.contract.ChannelVisibility
import com.retheviper.chat.contract.CreateChannelRequest
import com.retheviper.chat.contract.CreateWorkspaceRequest
import com.retheviper.chat.contract.NotificationKind
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
    environment: MessagingAppEnvironment = rememberMessagingAppEnvironment(),
    onUnreadNotificationCountChange: (Int) -> Unit = {},
    onNotificationEvent: (AppNotificationEvent) -> Unit = {},
    onWindowTitleChange: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val state = rememberMessagingAppState(environment, scope)
    val appFontFamily = rememberAppFontFamily()
    val darkTheme = isSystemInDarkTheme()
    val palette = rememberCurrentPalette()

    LaunchedEffect(Unit) {
        runCatching { state.refreshWorkspaceList() }
            .onFailure { state.status = it.message ?: AppStatus.failedLoadWorkspaces }
    }

    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                runCatching { state.dispose() }
            }
        }
    }

    DisposableEffect(state.screen, state.currentMember?.id, state.workspace?.id) {
        state.startNotificationStreamIfNeeded()
        onDispose {
            state.disconnectNotificationStreamAsync()
        }
    }

    LaunchedEffect(state.screen, state.currentMember?.id, state.workspace?.id, state.notificationStreamHandle) {
        state.refreshOrPollNotifications()
    }

    LaunchedEffect(state.unreadNotifications.size) {
        onUnreadNotificationCountChange(state.unreadNotifications.size)
    }

    LaunchedEffect(state.toastNotifications.toList()) {
        state.toastNotifications.forEach { notification ->
            onNotificationEvent(AppNotificationEvent(notification.id, notification.title, notification.body))
        }
    }

    LaunchedEffect(state.screen, state.workspace?.name, state.channel?.name, state.currentMember?.displayName, state.centerView) {
        onWindowTitleChange(
            buildWindowTitle(
                screen = state.screen,
                workspaceName = state.workspace?.name,
                channelName = state.channel?.name,
                memberDisplayName = state.currentMember?.displayName,
                centerView = state.centerView
            )
        )
    }

    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colors = rememberMaterialColors(darkTheme, palette),
            typography = Typography(defaultFontFamily = appFontFamily)
        ) {
            Surface(modifier = Modifier, color = palette.shell) {
                when (state.screen) {
                    AppScreen.LANDING -> LandingScreen(
                        workspaces = state.workspaces,
                        status = state.status,
                        workspaceSlug = state.createWorkspaceSlug,
                        workspaceName = state.createWorkspaceName,
                        ownerUserId = state.createOwnerUserId,
                        ownerDisplayName = state.createOwnerDisplayName,
                        onWorkspaceSlugChange = { state.createWorkspaceSlug = it },
                        onWorkspaceNameChange = { state.createWorkspaceName = it },
                        onOwnerUserIdChange = { state.createOwnerUserId = it },
                        onOwnerDisplayNameChange = { state.createOwnerDisplayName = it },
                        onOpenWorkspace = { target -> scope.launch { state.openWorkspace(target, member = null) } },
                        onCreateWorkspace = {
                            scope.launch {
                                state.status = AppStatus.creatingWorkspace
                                runCatching {
                                    state.client.createWorkspace(
                                        CreateWorkspaceRequest(
                                            slug = state.createWorkspaceSlug,
                                            name = state.createWorkspaceName,
                                            ownerUserId = state.createOwnerUserId,
                                            ownerDisplayName = state.createOwnerDisplayName
                                        )
                                    )
                                }.onSuccess { created ->
                                    state.refreshWorkspaceList()
                                    val owner = state.client.listWorkspaceMembers(created.id).firstOrNull { it.id == created.ownerMemberId }
                                    state.openWorkspace(created, owner)
                                    state.status = AppStatus.workspaceCreated
                                }.onFailure {
                                    state.status = it.message ?: AppStatus.workspaceCreateFailed
                                }
                            }
                        }
                    )

                    AppScreen.JOIN_WORKSPACE -> JoinWorkspaceScreen(
                        workspace = state.workspace,
                        existingMembers = state.workspaceMembers,
                        status = state.status,
                        userId = state.joinUserId,
                        displayName = state.joinDisplayName,
                        onUserIdChange = { state.joinUserId = it },
                        onDisplayNameChange = { state.joinDisplayName = it },
                        onBack = {
                            state.workspace = null
                            state.currentMember = null
                            state.workspaceMembers.clear()
                            state.workspaceChannels.clear()
                            state.screen = AppScreen.LANDING
                            state.status = AppDefaults.initialStatus
                        },
                        onJoin = {
                            val targetWorkspace = state.workspace ?: return@JoinWorkspaceScreen
                            scope.launch {
                                val joinPlan = planWorkspaceJoin(state.workspaceMembers, state.joinUserId, state.joinDisplayName) ?: return@launch
                                joinPlan.existingMember?.let { member ->
                                    state.currentMember = member
                                    state.profileDisplayName = member.displayName
                                    state.screen = AppScreen.WORKSPACE
                                    state.status = AppStatus.signedInAs(member.displayName)
                                    state.workspaceChannels.firstOrNull()?.let { state.connectChannel(it) }
                                    return@launch
                                }
                                state.status = AppStatus.creatingMemberProfile
                                runCatching {
                                    state.client.addWorkspaceMember(targetWorkspace.id, requireNotNull(joinPlan.createRequest))
                                }.onSuccess { member ->
                                    state.currentMember = member
                                    state.profileDisplayName = member.displayName
                                    state.refreshWorkspaceContext(targetWorkspace)
                                    state.screen = AppScreen.WORKSPACE
                                    state.status = AppStatus.joinedAs(member.displayName)
                                    state.workspaceChannels.firstOrNull()?.let { state.connectChannel(it) }
                                }.onFailure {
                                    state.status = it.message ?: AppStatus.joinFailed
                                }
                            }
                        },
                        onContinueAsMember = { member ->
                            scope.launch {
                                state.currentMember = member
                                state.profileDisplayName = member.displayName
                                state.screen = AppScreen.WORKSPACE
                                state.status = AppStatus.signedInAs(member.displayName)
                                state.workspaceChannels.firstOrNull()?.let { state.connectChannel(it) }
                            }
                        }
                    )

                    AppScreen.WORKSPACE -> WorkspaceScreen(
                        workspace = state.workspace,
                        currentMember = state.currentMember,
                        workspaceMembers = state.workspaceMembers,
                        status = state.status,
                        workspaceChannels = state.workspaceChannels,
                        channelMentionCounts = state.unreadNotifications.filter { it.kind == NotificationKind.MENTION }.groupingBy { it.channelId }.eachCount(),
                        notificationCount = state.unreadNotifications.size,
                        centerView = state.centerView,
                        allNotifications = state.allNotifications,
                        channel = state.channel,
                        messages = state.messages,
                        threadMessages = state.threadMessages,
                        selectedRootId = state.selectedRootId,
                        focusedMessageId = state.focusedMessageId,
                        focusedThreadMessageId = state.focusedThreadMessageId,
                        channelSlug = state.channelSlug,
                        channelName = state.channelName,
                        channelTopic = state.channelTopic,
                        hasOlderMessages = state.hasOlderMessages,
                        loadingOlderMessages = state.loadingOlderMessages,
                        messageBody = state.messageBody,
                        messageLinkPreview = state.messageLinkPreview,
                        threadMessageBody = state.threadMessageBody,
                        threadLinkPreview = state.threadLinkPreview,
                        profileDisplayName = state.profileDisplayName,
                        onChannelSlugChange = { state.channelSlug = it },
                        onChannelNameChange = { state.channelName = it },
                        onChannelTopicChange = { state.channelTopic = it },
                        onMessageBodyChange = { value -> scope.launch { state.updateMessageBody(value) } },
                        onDismissMessagePreview = { state.dismissMessagePreview() },
                        onThreadMessageBodyChange = { value -> scope.launch { state.updateMessageBody(value, thread = true) } },
                        onDismissThreadPreview = { state.dismissMessagePreview(thread = true) },
                        onSwitchWorkspace = {
                            state.resetWorkspaceSession()
                            scope.launch {
                                runCatching { state.refreshWorkspaceList() }
                                    .onFailure { state.status = it.message ?: AppStatus.failedLoadWorkspaces }
                            }
                        },
                        onOpenChannel = { target -> scope.launch { state.connectChannel(target) } },
                        onOpenNotifications = {
                            scope.launch {
                                state.refreshNotifications(showToast = false)
                                state.centerView = WorkspaceCenterView.NOTIFICATIONS
                            }
                        },
                        onOpenNotificationItem = { notification ->
                            scope.launch {
                                state.navigateToNotification(
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
                            val targetWorkspace = state.workspace ?: return@WorkspaceScreen
                            val member = state.currentMember ?: return@WorkspaceScreen
                            scope.launch {
                                state.status = AppStatus.creatingChannel
                                runCatching {
                                    state.client.createChannel(
                                        targetWorkspace.id,
                                        CreateChannelRequest(
                                            slug = state.channelSlug,
                                            name = state.channelName,
                                            topic = state.channelTopic,
                                            visibility = ChannelVisibility.PUBLIC,
                                            createdByMemberId = member.id
                                        )
                                    )
                                }.onSuccess {
                                    state.refreshWorkspaceContext(targetWorkspace)
                                    state.connectChannel(it)
                                }.onFailure {
                                    state.status = it.message ?: AppStatus.channelCreateFailed
                                }
                            }
                        },
                        onOpenThread = { message ->
                            state.selectedRootId = message.id
                            state.threadMessageBody = ""
                            state.focusedMessageId = message.id
                            state.focusedThreadMessageId = null
                            scope.launch {
                                loadThread(state.client, message.id, state.threadMessages) { state.status = it }
                                state.markNotificationsRead(threadNotificationIdsToMarkRead(state.unreadNotifications, message.id))
                            }
                        },
                        onCloseThread = {
                            state.selectedRootId = null
                            state.threadMessages.clear()
                            state.threadMessageBody = ""
                            state.focusedThreadMessageId = null
                        },
                        onLoadOlderMessages = { scope.launch { state.loadOlderMessages() } },
                        onToggleReaction = { message, emoji -> scope.launch { state.toggleReaction(message, emoji) } },
                        onSendChannelMessage = {
                            val runtime = state.chatRuntime ?: return@WorkspaceScreen
                            val member = state.currentMember ?: return@WorkspaceScreen
                            scope.launch {
                                val command = buildOutgoingChatCommand(
                                    primaryAuthorId = member.id,
                                    body = state.messageBody,
                                    replyParentMessageId = "",
                                    linkPreview = state.messageLinkPreview
                                ) ?: return@launch
                                runCatching { state.client.sendCommand(runtime.session, command) }
                                    .onSuccess {
                                        state.messageBody = ""
                                        state.messageLinkPreview = null
                                        state.dismissedMessagePreviewUrl = null
                                        state.status = AppStatus.messageSent
                                    }.onFailure { state.status = it.message ?: AppStatus.sendFailed }
                            }
                        },
                        onSendThreadMessage = {
                            val runtime = state.chatRuntime ?: return@WorkspaceScreen
                            val member = state.currentMember ?: return@WorkspaceScreen
                            val rootId = state.selectedRootId ?: return@WorkspaceScreen
                            scope.launch {
                                val command = buildOutgoingChatCommand(
                                    primaryAuthorId = member.id,
                                    body = state.threadMessageBody,
                                    replyParentMessageId = rootId,
                                    linkPreview = state.threadLinkPreview
                                ) ?: return@launch
                                runCatching { state.client.sendCommand(runtime.session, command) }
                                    .onSuccess {
                                        state.threadMessageBody = ""
                                        state.threadLinkPreview = null
                                        state.dismissedThreadPreviewUrl = null
                                        state.status = AppStatus.replySent
                                    }.onFailure { state.status = it.message ?: AppStatus.replyFailed }
                            }
                        },
                        onProfileDisplayNameChange = { state.profileDisplayName = it },
                        onSaveProfile = {
                            scope.launch {
                                runCatching { state.updateCurrentMemberDisplayName(state.profileDisplayName) }
                                    .onFailure { state.status = it.message ?: AppStatus.profileUpdateFailed }
                            }
                        }
                    )
                }

                if (state.screen == AppScreen.WORKSPACE && state.toastNotifications.isNotEmpty()) {
                    NotificationOverlay(
                        notifications = state.toastNotifications,
                        onOpenNotification = { notification -> scope.launch { state.navigateToNotification(notification) } },
                        onDismiss = { notificationId -> state.toastNotifications.removeAll { it.id == notificationId } }
                    )
                }
            }
        }
    }
}
