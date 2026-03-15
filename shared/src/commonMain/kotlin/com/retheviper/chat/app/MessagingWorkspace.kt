package com.retheviper.chat.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.retheviper.chat.client.MessagingClient
import com.retheviper.chat.contract.ChannelResponse
import com.retheviper.chat.contract.LinkPreviewResponse
import com.retheviper.chat.contract.MentionNotificationResponse
import com.retheviper.chat.contract.MessageReactionResponse
import com.retheviper.chat.contract.MessageResponse
import com.retheviper.chat.contract.NotificationKind
import com.retheviper.chat.contract.WorkspaceMemberResponse
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.websocket.close
import kotlinx.coroutines.Job

@Composable
internal fun WorkspaceScreen(
    workspace: com.retheviper.chat.contract.WorkspaceResponse?,
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
    val palette = appPalette()
    val isCompactScreen = rememberIsCompactScreen()
    val mentionUserIds = remember(workspaceMembers) { workspaceMembers.mapTo(linkedSetOf()) { it.userId } }
    var createChannelDialogOpen by remember { mutableStateOf(false) }
    var reactionPickerTarget by remember { mutableStateOf<MessageResponse?>(null) }
    var editProfileDialogOpen by remember { mutableStateOf(false) }
    var compactSidebarOpen by rememberSaveable { mutableStateOf(false) }
    val messageListState = rememberLazyListState()
    val threadListState = rememberLazyListState()

    LaunchedEffect(focusedMessageId, messages.size) {
        val targetId = focusedMessageId ?: return@LaunchedEffect
        val index = messages.indexOfFirst { it.id == targetId }
        if (index >= 0) messageListState.animateScrollToItem(index)
    }

    LaunchedEffect(focusedThreadMessageId, threadMessages.size) {
        val targetId = focusedThreadMessageId ?: return@LaunchedEffect
        val index = threadMessages.indexOfFirst { it.id == targetId }
        if (index >= 0) threadListState.animateScrollToItem(index)
    }

    if (isCompactScreen) {
        Box(modifier = Modifier.fillMaxSize().background(palette.shell)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactTopBar(
                    workspaceName = workspace?.name ?: AppLabels.workspace,
                    subtitle = if (centerView == WorkspaceCenterView.NOTIFICATIONS) AppLabels.notifications else channel?.name ?: AppLabels.chooseChannel,
                    notificationCount = notificationCount,
                    onOpenSidebar = { compactSidebarOpen = true },
                    onOpenNotifications = onOpenNotifications
                )
                Card(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = palette.mainCard,
                    elevation = 0.dp
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        WorkspaceTopSummary(
                            channel = channel,
                            currentMember = currentMember,
                            compact = true
                        )
                        if (centerView == WorkspaceCenterView.NOTIFICATIONS) {
                            NotificationListPanel(notifications = allNotifications, onOpenNotification = onOpenNotificationItem)
                        } else if (channel == null) {
                            EmptyConversationState(AppLabels.openChannel, AppLabels.openChannelBody)
                        } else {
                            LazyColumn(
                                state = messageListState,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (hasOlderMessages) {
                                    item(key = "read-more") {
                                        OutlineActionButton(
                                            text = if (loadingOlderMessages) AppLabels.loading else AppLabels.readMore,
                                            onClick = onLoadOlderMessages,
                                            modifier = Modifier.fillMaxWidth(),
                                            contentColor = palette.accent
                                        )
                                    }
                                }
                                items(messages, key = { it.id }) { item ->
                                    MessageRow(
                                        message = item,
                                        selected = selectedRootId == item.id || focusedMessageId == item.id,
                                        currentMemberId = currentMember?.id,
                                        mentionUserIds = mentionUserIds,
                                        onOpenThread = { onOpenThread(item) },
                                        onToggleReaction = { emoji -> onToggleReaction(item, emoji) },
                                        onOpenReactionPicker = { reactionPickerTarget = item }
                                    )
                                }
                            }
                            AnimatedVisibility(visible = selectedRootId != null) {
                                ThreadPane(
                                    currentMember = currentMember,
                                    mentionUserIds = mentionUserIds,
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
                                    onSend = onSendThreadMessage,
                                    compact = true
                                )
                            }
                            if (selectedRootId == null) {
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
                    }
                }
            }
            AnimatedVisibility(
                visible = compactSidebarOpen,
                enter = slideInHorizontally(initialOffsetX = { -it / 2 }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { -it / 2 }) + fadeOut()
            ) {
                CompactSidebarDrawer(
                    workspaceName = workspace?.name ?: AppLabels.workspace,
                    currentMember = currentMember,
                    workspaceChannels = workspaceChannels,
                    channelMentionCounts = channelMentionCounts,
                    channel = channel,
                    centerView = centerView,
                    notificationCount = notificationCount,
                    onDismiss = { compactSidebarOpen = false },
                    onSwitchWorkspace = {
                        compactSidebarOpen = false
                        onSwitchWorkspace()
                    },
                    onOpenNotifications = {
                        compactSidebarOpen = false
                        onOpenNotifications()
                    },
                    onCreateChannel = {
                        compactSidebarOpen = false
                        createChannelDialogOpen = true
                    },
                    onOpenChannel = {
                        compactSidebarOpen = false
                        onOpenChannel(it)
                    },
                    onEditProfile = {
                        compactSidebarOpen = false
                        editProfileDialogOpen = true
                    }
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxSize().background(palette.shell).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WorkspaceSidebar(
                workspaceName = workspace?.name ?: AppLabels.workspace,
                currentMember = currentMember,
                workspaceChannels = workspaceChannels,
                channelMentionCounts = channelMentionCounts,
                channel = channel,
                centerView = centerView,
                notificationCount = notificationCount,
                onSwitchWorkspace = onSwitchWorkspace,
                onOpenNotifications = onOpenNotifications,
                onCreateChannel = { createChannelDialogOpen = true },
                onOpenChannel = onOpenChannel,
                onEditProfile = { editProfileDialogOpen = true }
            )

            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), backgroundColor = palette.mainCard, elevation = 0.dp) {
                    WorkspaceTopSummary(channel = channel, currentMember = currentMember, compact = false)
                }

                Card(modifier = Modifier.weight(1f).fillMaxWidth(), shape = RoundedCornerShape(24.dp), backgroundColor = palette.mainCard, elevation = 0.dp) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (centerView == WorkspaceCenterView.NOTIFICATIONS) {
                                NotificationListPanel(notifications = allNotifications, onOpenNotification = onOpenNotificationItem)
                            } else if (channel == null) {
                                EmptyConversationState(AppLabels.openChannel, AppLabels.openChannelBody)
                            } else {
                                LazyColumn(state = messageListState, modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (hasOlderMessages) {
                                        item(key = "read-more") {
                                            OutlineActionButton(
                                                text = if (loadingOlderMessages) AppLabels.loading else AppLabels.readMore,
                                                onClick = onLoadOlderMessages,
                                                modifier = Modifier.fillMaxWidth(),
                                                contentColor = palette.accent
                                            )
                                        }
                                    }
                                    items(messages, key = { it.id }) { item ->
                                        MessageRow(
                                            message = item,
                                            selected = selectedRootId == item.id || focusedMessageId == item.id,
                                            currentMemberId = currentMember?.id,
                                            mentionUserIds = mentionUserIds,
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
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                            visible = selectedRootId != null,
                            enter = slideInHorizontally(initialOffsetX = { it / 3 }) + fadeIn(),
                            exit = slideOutHorizontally(targetOffsetX = { it / 3 }) + fadeOut()
                        ) {
                            ThreadPane(
                                currentMember = currentMember,
                                mentionUserIds = mentionUserIds,
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
    }

    if (createChannelDialogOpen) {
        AlertDialog(
            onDismissRequest = { createChannelDialogOpen = false },
            title = { Text(AppLabels.createChannel, color = palette.darkText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField(AppLabels.slug, channelSlug, textColor = palette.darkText, onValueChange = onChannelSlugChange)
                    FormField(AppLabels.name, channelName, textColor = palette.darkText, onValueChange = onChannelNameChange)
                    FormField(AppLabels.topic, channelTopic, textColor = palette.darkText, onValueChange = onChannelTopicChange)
                }
            },
            confirmButton = {
                FilledActionButton(AppLabels.create, onClick = { createChannelDialogOpen = false; onCreateChannel() })
            },
            dismissButton = {
                OutlineActionButton(AppLabels.cancel, onClick = { createChannelDialogOpen = false }, contentColor = palette.darkText)
            },
            shape = RoundedCornerShape(24.dp),
            backgroundColor = palette.mainCard
        )
    }

    if (editProfileDialogOpen) {
        AlertDialog(
            onDismissRequest = { editProfileDialogOpen = false },
            title = { Text(AppLabels.updateProfile, color = palette.darkText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField(AppLabels.displayName, profileDisplayName, textColor = palette.darkText, onValueChange = onProfileDisplayNameChange)
                    currentMember?.userId?.let { userId -> Text("${AppLabels.userId}: $userId", color = palette.dimText) }
                }
            },
            confirmButton = {
                FilledActionButton(AppLabels.save, onClick = { editProfileDialogOpen = false; onSaveProfile() })
            },
            dismissButton = {
                OutlineActionButton(
                    AppLabels.cancel,
                    onClick = {
                        onProfileDisplayNameChange(currentMember?.displayName.orEmpty())
                        editProfileDialogOpen = false
                    },
                    contentColor = palette.darkText
                )
            },
            shape = RoundedCornerShape(24.dp),
            backgroundColor = palette.mainCard
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
internal fun ThreadPane(
    currentMember: WorkspaceMemberResponse?,
    mentionUserIds: Set<String>,
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
    onSend: () -> Unit,
    compact: Boolean = false
) {
    val palette = appPalette()
    Card(
        modifier = if (compact) {
            Modifier.fillMaxWidth().heightIn(min = 360.dp, max = 560.dp)
        } else {
            Modifier.width(420.dp).fillMaxHeight().padding(14.dp)
        },
        shape = RoundedCornerShape(28.dp),
        backgroundColor = palette.threadBg.copy(alpha = 0.98f),
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(AppLabels.thread, style = MaterialTheme.typography.h6, color = palette.darkText)
                Text(AppLabels.close, color = palette.accent, modifier = Modifier.clickable(onClick = onCloseThread))
            }
            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(threadMessages, key = { _, item -> item.id }) { index, item ->
                    val isOwnMessage = currentMember?.id == item.authorMemberId
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(start = if (compact || index == 0) 0.dp else 18.dp),
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = when {
                            focusedThreadMessageId == item.id && isOwnMessage -> palette.ownFocusedMessageBg
                            focusedThreadMessageId == item.id -> palette.otherFocusedMessageBg
                            isOwnMessage -> palette.ownMessageBg
                            else -> palette.mainCard
                        },
                        elevation = 0.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.authorDisplayName, fontWeight = FontWeight.Bold, color = palette.darkText)
                            MessageBodyText(text = item.body, mentionUserIds = mentionUserIds)
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
                sendLabel = AppLabels.replyInThread,
                placeholder = AppLabels.replyToThread
            )
        }
    }
}

internal suspend fun loadThread(
    client: MessagingClient,
    rootId: String?,
    threadMessages: MutableList<MessageResponse>,
    updateStatus: (String) -> Unit
) {
    if (rootId == null) return
    runCatching { client.getThread(rootId) }
        .onSuccess {
            threadMessages.replaceWith(toThreadMessages(it))
            updateStatus(AppStatus.threadLoaded)
        }
        .onFailure {
            updateStatus(it.message ?: AppStatus.failedLoadThread)
        }
}

@Composable
internal fun WorkspaceSidebar(
    workspaceName: String,
    currentMember: WorkspaceMemberResponse?,
    workspaceChannels: List<ChannelResponse>,
    channelMentionCounts: Map<String, Int>,
    channel: ChannelResponse?,
    centerView: WorkspaceCenterView,
    notificationCount: Int,
    showNotificationsShortcut: Boolean = true,
    onSwitchWorkspace: () -> Unit,
    onOpenNotifications: () -> Unit,
    onCreateChannel: () -> Unit,
    onOpenChannel: (ChannelResponse) -> Unit,
    onEditProfile: () -> Unit
) {
    val palette = appPalette()
    Card(
        modifier = Modifier.width(300.dp).fillMaxHeight(),
        shape = RoundedCornerShape(28.dp),
        backgroundColor = palette.sidebar,
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(workspaceName, style = MaterialTheme.typography.h5, color = palette.lightText)
                    Card(
                        modifier = Modifier.clickable(onClick = onSwitchWorkspace),
                        shape = CircleShape,
                        backgroundColor = palette.sidebarCard,
                        elevation = 0.dp
                    ) {
                        Text("<>", modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), color = palette.lightText, fontWeight = FontWeight.Bold)
                    }
                }

                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), backgroundColor = palette.sidebarCard, elevation = 0.dp) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (showNotificationsShortcut) {
                            SidebarMenuItem(
                                label = AppLabels.notifications,
                                badge = notificationCount,
                                selected = centerView == WorkspaceCenterView.NOTIFICATIONS,
                                onClick = onOpenNotifications
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(AppLabels.channels, color = palette.lightText, fontWeight = FontWeight.Bold)
                            Card(
                                modifier = Modifier.clickable(onClick = onCreateChannel),
                                shape = RoundedCornerShape(12.dp),
                                backgroundColor = palette.accent,
                                elevation = 0.dp
                            ) {
                                Text("+", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color.White, fontWeight = FontWeight.Bold)
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

            MemberSummaryCard(
                currentMember = currentMember,
                dark = true,
                onClick = onEditProfile
            )
        }
    }
}

@Composable
internal fun MemberSummaryCard(
    currentMember: WorkspaceMemberResponse?,
    dark: Boolean,
    onClick: () -> Unit
) {
    val palette = appPalette()
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = if (dark) palette.sidebarCard else palette.overlayCard,
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    currentMember?.displayName ?: AppLabels.noMember,
                    color = if (dark) palette.lightText else palette.darkText,
                    fontWeight = FontWeight.Bold
                )
                Text(currentMember?.userId ?: "", color = if (dark) palette.mutedText else palette.dimText)
            }
            Text(AppLabels.edit, color = palette.accentSoft, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun CompactTopBar(
    workspaceName: String,
    subtitle: String,
    notificationCount: Int,
    onOpenSidebar: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    val palette = appPalette()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = palette.sidebar,
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.clickable(onClick = onOpenSidebar),
                shape = CircleShape,
                backgroundColor = palette.sidebarCard,
                elevation = 0.dp
            ) {
                Text("≡", modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = palette.lightText, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(workspaceName, color = palette.lightText, fontWeight = FontWeight.Bold)
                Text(subtitle, color = palette.mutedText)
            }
            Card(
                modifier = Modifier.clickable(onClick = onOpenNotifications),
                shape = CircleShape,
                backgroundColor = palette.sidebarCard,
                elevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("\uD83D\uDD14", color = palette.lightText, fontWeight = FontWeight.Bold)
                    if (notificationCount > 0) {
                        Text("$notificationCount", color = palette.lightText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun CompactSidebarDrawer(
    workspaceName: String,
    currentMember: WorkspaceMemberResponse?,
    workspaceChannels: List<ChannelResponse>,
    channelMentionCounts: Map<String, Int>,
    channel: ChannelResponse?,
    centerView: WorkspaceCenterView,
    notificationCount: Int,
    onDismiss: () -> Unit,
    onSwitchWorkspace: () -> Unit,
    onOpenNotifications: () -> Unit,
    onCreateChannel: () -> Unit,
    onOpenChannel: (ChannelResponse) -> Unit,
    onEditProfile: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.18f))
                .clickable(onClick = onDismiss)
        )
        WorkspaceSidebar(
            workspaceName = workspaceName,
            currentMember = currentMember,
            workspaceChannels = workspaceChannels,
            channelMentionCounts = channelMentionCounts,
            channel = channel,
            centerView = centerView,
            notificationCount = notificationCount,
            showNotificationsShortcut = false,
            onSwitchWorkspace = onSwitchWorkspace,
            onOpenNotifications = onOpenNotifications,
            onCreateChannel = onCreateChannel,
            onOpenChannel = onOpenChannel,
            onEditProfile = onEditProfile
        )
    }
}

@Composable
internal fun WorkspaceTopSummary(
    channel: ChannelResponse?,
    currentMember: WorkspaceMemberResponse?,
    compact: Boolean
) {
    val palette = appPalette()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = if (compact) 4.dp else 20.dp, vertical = if (compact) 4.dp else 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = if (compact) Alignment.Top else Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(channel?.let { "# ${it.slug}" } ?: AppLabels.chooseChannel, style = MaterialTheme.typography.h5, color = palette.darkText)
            Text(channel?.topic ?: AppLabels.selectChannelBody, color = palette.dimText)
        }
        if (!compact) {
            Column {
                Text(currentMember?.displayName ?: AppLabels.noMember, fontWeight = FontWeight.Bold, color = palette.darkText)
                Text(currentMember?.userId ?: "", color = palette.dimText)
            }
        }
    }
}

internal data class ChatRuntime(
    val session: DefaultClientWebSocketSession,
    val collector: Job
) {
    suspend fun close() {
        collector.cancel()
        session.close()
    }
}

@Composable
internal fun SidebarChannelItem(
    channel: ChannelResponse,
    mentionCount: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = appPalette()
    val itemShape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .background(if (selected) palette.accent.copy(alpha = 0.24f) else Color.Transparent, itemShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("# ${channel.slug}", color = palette.lightText, fontWeight = FontWeight.SemiBold)
                Text(channel.topic ?: AppLabels.generalNoTopic, color = palette.mutedText)
            }
            if (mentionCount > 0) {
                Card(shape = RoundedCornerShape(999.dp), backgroundColor = palette.accent, elevation = 0.dp) {
                    Text("$mentionCount", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun SidebarMenuItem(label: String, badge: Int, selected: Boolean, onClick: () -> Unit) {
    val palette = appPalette()
    val itemShape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .background(if (selected) palette.accent.copy(alpha = 0.24f) else Color.Transparent, itemShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = palette.lightText, fontWeight = FontWeight.SemiBold)
        if (badge > 0) {
            Card(shape = RoundedCornerShape(999.dp), backgroundColor = palette.accent, elevation = 0.dp) {
                Text("$badge", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun MessageRow(
    message: MessageResponse,
    selected: Boolean,
    currentMemberId: String?,
    mentionUserIds: Set<String>,
    onOpenThread: () -> Unit,
    onToggleReaction: (String) -> Unit,
    onOpenReactionPicker: () -> Unit
) {
    val palette = appPalette()
    val isOwnMessage = currentMemberId == message.authorMemberId
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenThread),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = when {
            selected && isOwnMessage -> palette.ownFocusedMessageBg
            selected -> palette.otherFocusedMessageBg
            isOwnMessage -> palette.ownMessageBg
            else -> palette.otherMessageBg
        },
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(message.authorDisplayName, fontWeight = FontWeight.Bold, color = palette.darkText)
                Text(shortTimestamp(message.createdAt), color = palette.dimText)
            }
            MessageBodyText(text = message.body, mentionUserIds = mentionUserIds)
            message.linkPreview?.let { LinkPreviewCard(preview = it) }
            ReactionBar(reactions = message.reactions, currentMemberId = currentMemberId, onToggleReaction = onToggleReaction, onOpenReactionPicker = onOpenReactionPicker)
            if (message.threadReplyCount > 0) {
                Card(shape = RoundedCornerShape(999.dp), backgroundColor = palette.overlayCard, elevation = 0.dp) {
                    Text("${message.threadReplyCount} repl${if (message.threadReplyCount == 1) "y" else "ies"}", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = palette.accent, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
internal fun ComposerPanel(
    currentMember: WorkspaceMemberResponse?,
    mentionCandidates: List<WorkspaceMemberResponse>,
    messageBody: String,
    linkPreview: LinkPreviewResponse?,
    onMessageBodyChange: (String) -> Unit,
    onDismissPreview: () -> Unit,
    onFocus: () -> Unit = {},
    onSend: () -> Unit,
    sendLabel: String = AppLabels.send,
    placeholder: String = AppLabels.message
) {
    val palette = appPalette()
    val suggestions = suggestMentionCandidates(mentionCandidates, messageBody)
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), backgroundColor = palette.subtleSurface, elevation = 0.dp) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (suggestions.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), backgroundColor = palette.mainCard, elevation = 0.dp) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        suggestions.forEach { member ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { onMessageBodyChange(applyMentionCandidate(messageBody, member.userId)) }.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(member.displayName, color = palette.darkText, fontWeight = FontWeight.Medium)
                                Text("@${member.userId}", color = palette.dimText)
                            }
                        }
                    }
                }
            }
            linkPreview?.let { ComposerLinkPreviewCard(preview = it, onDismiss = onDismissPreview) }
            FormField(label = placeholder, value = messageBody, minLines = 4, textColor = palette.darkText, onValueChange = onMessageBodyChange, onFocus = onFocus)
            FilledActionButton(sendLabel, onSend, modifier = Modifier.fillMaxWidth(), enabled = currentMember != null && messageBody.isNotBlank())
        }
    }
}

@Composable
internal fun ComposerLinkPreviewCard(preview: LinkPreviewResponse, onDismiss: () -> Unit) {
    val palette = appPalette()
    val imageOnly = isImageOnlyPreview(preview)
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), backgroundColor = palette.mainCard, elevation = 0.dp) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (!imageOnly) Text(preview.siteName ?: AppLabels.linkPreview, color = palette.darkText, fontWeight = FontWeight.SemiBold)
                else Spacer(modifier = Modifier.width(1.dp))
                Text("x", color = palette.accent, modifier = Modifier.clickable(onClick = onDismiss))
            }
            LinkPreviewImage(preview)
            if (!imageOnly) LinkPreviewSummary(preview)
        }
    }
}

@Composable
internal fun LinkPreviewCard(preview: LinkPreviewResponse) {
    val palette = appPalette()
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), backgroundColor = palette.overlayCard, elevation = 0.dp) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LinkPreviewImage(preview)
            if (!isImageOnlyPreview(preview)) LinkPreviewSummary(preview)
        }
    }
}

@Composable
internal fun LinkPreviewImage(preview: LinkPreviewResponse) {
    val palette = appPalette()
    val environment = rememberMessagingAppEnvironment()
    val imageUrl = preview.imageUrl?.takeIf { it.isNotBlank() } ?: return
    var dialogOpen by remember(imageUrl) { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().clickable { dialogOpen = true }, shape = RoundedCornerShape(12.dp), backgroundColor = palette.subtleSurface, elevation = 0.dp) {
        AsyncImage(
            model = environment.previewImageUrl(imageUrl),
            contentDescription = preview.title ?: preview.siteName ?: AppLabels.linkPreviewImage,
            modifier = Modifier.fillMaxWidth().height(180.dp),
            contentScale = ContentScale.Crop
        )
    }
    if (dialogOpen) {
        ImageAssetDialog(imageUrl = imageUrl, title = preview.title, onDismiss = { dialogOpen = false })
    }
}

@Composable
internal fun LinkPreviewSummary(preview: LinkPreviewResponse) {
    val palette = appPalette()
    if (!preview.siteName.isNullOrBlank()) Text(preview.siteName, color = palette.accent, fontWeight = FontWeight.SemiBold)
    if (!preview.title.isNullOrBlank()) Text(preview.title, color = palette.darkText, fontWeight = FontWeight.Bold)
    if (!preview.description.isNullOrBlank()) Text(preview.description, color = palette.dimText)
}

@Composable
internal fun ImageAssetDialog(imageUrl: String, title: String?, onDismiss: () -> Unit) {
    val palette = appPalette()
    val environment = rememberMessagingAppEnvironment()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title ?: AppDefaults.imageName, color = palette.darkText, fontWeight = FontWeight.Bold) },
        text = {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), backgroundColor = palette.overlayCard, elevation = 0.dp) {
                AsyncImage(
                    model = environment.previewImageUrl(imageUrl),
                    contentDescription = title ?: AppLabels.imagePreview,
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    contentScale = ContentScale.Fit
                )
            }
        },
        buttons = {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                Button(
                    onClick = { LinkAssetActions.copyText(imageUrl) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = palette.mainCard, contentColor = palette.darkText),
                    elevation = null
                ) { Text(AppLabels.copyUrl) }
                Button(
                    onClick = { LinkAssetActions.saveRemoteFile(imageUrl, suggestedName = imageUrl.suggestedDownloadName()) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = palette.accent, contentColor = Color.White),
                    elevation = null
                ) { Text(AppLabels.download) }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(backgroundColor = palette.mainCard, contentColor = palette.darkText),
                    elevation = null
                ) { Text(AppLabels.close) }
            }
        }
    )
}

@Composable
internal fun MessageBodyText(text: String, mentionUserIds: Set<String>) {
    val palette = appPalette()
    val annotated = buildMessageAnnotatedText(text, mentionUserIds, palette)
    Text(text = annotated, style = MaterialTheme.typography.body1.copy(color = palette.softText))
}

@Composable
internal fun ReactionBar(
    reactions: List<MessageReactionResponse>,
    currentMemberId: String?,
    onToggleReaction: (String) -> Unit,
    onOpenReactionPicker: () -> Unit
) {
    val palette = appPalette()
    val emojiFontFamily = rememberEmojiFontFamily()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        reactions.forEach { reaction ->
            val selected = currentMemberId != null && currentMemberId in reaction.memberIds
            Card(
                modifier = Modifier.clickable { onToggleReaction(reaction.emoji) },
                shape = RoundedCornerShape(999.dp),
                backgroundColor = if (selected) palette.overlayCard else palette.subtleSurface,
                elevation = 0.dp
            ) {
                Text(
                    "${reactionDisplayLabel(reaction.emoji)} ${reaction.count}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = if (selected) palette.accent else palette.darkText,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = emojiFontFamily
                )
            }
        }
        Card(modifier = Modifier.clickable(onClick = onOpenReactionPicker), shape = RoundedCornerShape(999.dp), backgroundColor = palette.subtleSurface, elevation = 0.dp) {
            Text("+", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = palette.accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun EmojiPickerDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val palette = appPalette()
    val emojiFontFamily = rememberEmojiFontFamily()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppLabels.addReaction, color = palette.darkText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ReactionDefaults.chunked(3).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { emoji ->
                            Card(
                                modifier = Modifier.weight(1f).clickable { onSelect(emoji) },
                                shape = RoundedCornerShape(16.dp),
                                backgroundColor = palette.subtleSurface,
                                elevation = 0.dp
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                                    Text(reactionDisplayLabel(emoji), color = palette.darkText, style = MaterialTheme.typography.h6, fontFamily = emojiFontFamily)
                                }
                            }
                        }
                        repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { OutlineActionButton(AppLabels.close, onClick = onDismiss, contentColor = palette.darkText) },
        shape = RoundedCornerShape(24.dp),
        backgroundColor = palette.mainCard
    )
}

@Composable
internal fun NotificationListPanel(
    notifications: List<MentionNotificationResponse>,
    onOpenNotification: (MentionNotificationResponse) -> Unit
) {
    val palette = appPalette()
    if (notifications.isEmpty()) {
        EmptyConversationState(AppLabels.noNotifications, AppLabels.noNotificationsBody)
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(notifications, key = { it.id }) { notification ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onOpenNotification(notification) },
                shape = RoundedCornerShape(16.dp),
                backgroundColor = if (notification.readAt == null) palette.overlayCard else palette.subtleSurface,
                elevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(notificationTitle(notification), color = palette.darkText, fontWeight = FontWeight.Bold)
                    Text(notification.messagePreview, color = palette.softText)
                    Text(shortTimestamp(notification.createdAt), color = palette.dimText)
                }
            }
        }
    }
}

internal fun notificationTitle(notification: MentionNotificationResponse): String =
    when (notification.kind) {
        NotificationKind.MENTION -> notification.authorDisplayName
        NotificationKind.THREAD_ACTIVITY -> AppStatus.notificationTitleThread(notification.authorDisplayName)
    }

internal fun buildMessageAnnotatedText(text: String, mentionUserIds: Set<String>, palette: AppPalette) = buildAnnotatedString {
    val tokenRegex = Regex("""https?://[^\s]+|@([a-zA-Z0-9._-]+)""", RegexOption.IGNORE_CASE)
    var currentIndex = 0
    tokenRegex.findAll(text).forEach { match ->
        val range = match.range
        if (range.first > currentIndex) append(text.substring(currentIndex, range.first))
        val token = match.value
        if (token.startsWith("http://", true) || token.startsWith("https://", true)) {
            val normalizedUrl = token.trimEnd('.', ',', ';', ':', ')', ']', '!')
            pushLink(
                LinkAnnotation.Url(
                    url = normalizedUrl,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = palette.accent,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                )
            )
            append(normalizedUrl)
            pop()
            if (normalizedUrl.length != token.length) append(token.removePrefix(normalizedUrl))
        } else {
            val userId = match.groupValues.getOrNull(1).orEmpty()
            if (userId in mentionUserIds) {
                pushStyle(SpanStyle(color = palette.accent, background = palette.overlayCard, fontWeight = FontWeight.SemiBold))
                append(token)
                pop()
            } else {
                append(token)
            }
        }
        currentIndex = range.last + 1
    }
    if (currentIndex < text.length) append(text.substring(currentIndex))
}

internal fun extractFirstUrl(text: String): String? {
    return UrlRegex.find(text)?.value?.trimEnd('.', ',', ';', ':', ')', ']', '!')?.takeIf { it.isNotBlank() }
}

internal fun reactionDisplayLabel(value: String): String {
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

internal fun isImageOnlyPreview(preview: LinkPreviewResponse): Boolean {
    return !preview.imageUrl.isNullOrBlank() &&
        preview.imageUrl == preview.url &&
        preview.title.isNullOrBlank() &&
        preview.description.isNullOrBlank() &&
        preview.siteName.isNullOrBlank()
}

internal fun String.suggestedDownloadName(): String {
    val withoutQuery = substringBefore('?').substringBefore('#')
    val fileName = withoutQuery.substringAfterLast('/')
    return fileName.ifBlank { AppDefaults.imageFallbackFileName }
}

@Composable
internal fun NotificationOverlay(
    notifications: List<ToastNotification>,
    onOpenNotification: (ToastNotification) -> Unit,
    onDismiss: (String) -> Unit
) {
    val palette = appPalette()
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, end = 18.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(modifier = Modifier.widthIn(max = 360.dp), shape = RoundedCornerShape(20.dp), backgroundColor = palette.mainCard, elevation = 0.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                notifications.forEach { notification ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenNotification(notification) },
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = palette.subtleSurface,
                        elevation = 0.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(notification.title, color = palette.darkText, fontWeight = FontWeight.SemiBold)
                                Text("x", color = palette.accent, modifier = Modifier.clickable { onDismiss(notification.id) })
                            }
                            Text(notification.body, color = palette.softText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun EmptyConversationState(title: String, body: String) {
    val palette = appPalette()
    Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.h6, color = palette.darkText)
        Text(body, color = palette.dimText)
    }
}

@Composable
internal fun FilledActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val palette = appPalette()
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = palette.accent,
            contentColor = Color.White,
            disabledBackgroundColor = palette.border,
            disabledContentColor = palette.dimText
        ),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp)
    ) {
        Text(text)
    }
}

@Composable
internal fun OutlineActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = LightPalette.lightText
) {
    val palette = appPalette()
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color.Transparent,
            contentColor = if (contentColor == LightPalette.lightText) palette.lightText else contentColor
        ),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp)
    ) {
        Text(text)
    }
}

@Composable
internal fun FormField(
    label: String,
    value: String,
    minLines: Int = 1,
    textColor: Color = LightPalette.darkText,
    labelColor: Color = LightPalette.dimText,
    onValueChange: (String) -> Unit,
    onFocus: () -> Unit = {}
) {
    val palette = appPalette()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) onFocus() },
        label = { Text(label, color = if (labelColor == LightPalette.dimText) palette.dimText else labelColor) },
        minLines = minLines,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = if (textColor == LightPalette.darkText) palette.darkText else textColor,
            cursorColor = palette.accent,
            focusedBorderColor = palette.accent,
            unfocusedBorderColor = palette.border,
            focusedLabelColor = palette.accent,
            unfocusedLabelColor = palette.dimText,
            backgroundColor = palette.mainCard
        ),
        textStyle = MaterialTheme.typography.body1.copy(color = if (textColor == LightPalette.darkText) palette.darkText else textColor)
    )
}

internal fun shortTimestamp(createdAt: String): String = createdAt.drop(11).take(5).ifBlank { createdAt }

internal fun <T> MutableList<T>.replaceWith(items: List<T>) {
    clear()
    addAll(items)
}
