package com.retheviper.chat.app

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.retheviper.chat.contract.WorkspaceMemberResponse
import com.retheviper.chat.contract.WorkspaceResponse

@Composable
internal fun LandingScreen(
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
    val palette = appPalette()
    val isCompactScreen = rememberIsCompactScreen()
    Box(
        modifier = Modifier.fillMaxSize().background(palette.shell).padding(if (isCompactScreen) 14.dp else 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(if (isCompactScreen) 1f else 0.9f)
                .widthIn(max = 1040.dp)
                .heightIn(min = if (isCompactScreen) 0.dp else 720.dp)
                .animateContentSize(),
            shape = RoundedCornerShape(32.dp),
            backgroundColor = palette.sidebar,
            elevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(if (isCompactScreen) 18.dp else 28.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Text(AppLabels.chatWorkspace, style = MaterialTheme.typography.h3, color = palette.lightText)
                Text(status, color = palette.mutedText)
                if (isCompactScreen) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SplitPanelCard(
                            modifier = Modifier.fillMaxWidth(),
                            title = AppLabels.join,
                            subtitle = AppLabels.joinSubtitle,
                            dark = true
                        ) {
                            if (workspaces.isEmpty()) {
                                EmptyConversationState(AppLabels.noWorkspaces, AppLabels.noWorkspacesBody)
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(workspaces, key = { it.id }) { item ->
                                        WorkspaceListItem(item, onClick = { onOpenWorkspace(item) })
                                    }
                                }
                            }
                        }

                        SplitPanelCard(
                            modifier = Modifier.fillMaxWidth(),
                            title = AppLabels.create,
                            subtitle = AppLabels.createWorkspaceSubtitle,
                            dark = false
                        ) {
                            FormField(AppLabels.workspaceSlugField, workspaceSlug, textColor = palette.darkText, onValueChange = onWorkspaceSlugChange)
                            FormField(AppLabels.workspaceNameField, workspaceName, textColor = palette.darkText, onValueChange = onWorkspaceNameChange)
                            FormField(AppLabels.ownerUserIdField, ownerUserId, textColor = palette.darkText, onValueChange = onOwnerUserIdChange)
                            FormField(AppLabels.ownerDisplayNameField, ownerDisplayName, textColor = palette.darkText, onValueChange = onOwnerDisplayNameChange)
                            FilledActionButton(AppLabels.createWorkspace, onCreateWorkspace, modifier = Modifier.fillMaxWidth())
                        }
                    }
                } else Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    SplitPanelCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        title = AppLabels.join,
                        subtitle = AppLabels.joinSubtitle,
                        dark = true
                    ) {
                        if (workspaces.isEmpty()) {
                            EmptyConversationState(AppLabels.noWorkspaces, AppLabels.noWorkspacesBody)
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
                            .background(palette.mutedText.copy(alpha = 0.22f))
                    )

                    SplitPanelCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        title = AppLabels.create,
                        subtitle = AppLabels.createWorkspaceSubtitle,
                        dark = false
                    ) {
                        FormField(AppLabels.workspaceSlugField, workspaceSlug, textColor = palette.darkText, onValueChange = onWorkspaceSlugChange)
                        FormField(AppLabels.workspaceNameField, workspaceName, textColor = palette.darkText, onValueChange = onWorkspaceNameChange)
                        FormField(AppLabels.ownerUserIdField, ownerUserId, textColor = palette.darkText, onValueChange = onOwnerUserIdChange)
                        FormField(AppLabels.ownerDisplayNameField, ownerDisplayName, textColor = palette.darkText, onValueChange = onOwnerDisplayNameChange)
                        Spacer(modifier = Modifier.weight(1f))
                        FilledActionButton(AppLabels.createWorkspace, onCreateWorkspace, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
internal fun JoinWorkspaceScreen(
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
    val palette = appPalette()
    val isCompactScreen = rememberIsCompactScreen()
    Box(
        modifier = Modifier.fillMaxSize().background(palette.shell).padding(if (isCompactScreen) 14.dp else 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(if (isCompactScreen) 1f else 0.9f)
                .widthIn(max = 980.dp)
                .heightIn(min = if (isCompactScreen) 0.dp else 680.dp)
                .animateContentSize(),
            shape = RoundedCornerShape(32.dp),
            backgroundColor = palette.sidebar,
            elevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(if (isCompactScreen) 18.dp else 28.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                SplitPanelHeader(
                    title = AppStatus.joinWorkspaceTitle(workspace?.name),
                    subtitle = status,
                    dark = false,
                    backLabel = AppLabels.back,
                    onBack = onBack
                )
                if (isCompactScreen) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SplitPanelCard(
                            modifier = Modifier.fillMaxWidth(),
                            title = AppLabels.select,
                            subtitle = AppLabels.selectMemberSubtitle,
                            dark = true
                        ) {
                            if (existingMembers.isEmpty()) {
                                EmptyConversationState(AppLabels.noMembers, AppLabels.noMembersBody)
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(existingMembers, key = { it.id }) { member ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().clickable { onContinueAsMember(member) },
                                            shape = RoundedCornerShape(16.dp),
                                            backgroundColor = palette.sidebar,
                                            elevation = 0.dp
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(member.displayName, color = palette.lightText, fontWeight = FontWeight.Bold)
                                                    Text("@${member.userId}", color = palette.mutedText)
                                                }
                                                Text(AppLabels.continueLabel, color = palette.accentSoft, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        SplitPanelCard(
                            modifier = Modifier.fillMaxWidth(),
                            title = AppLabels.create,
                            subtitle = AppLabels.createMemberSubtitle,
                            dark = false
                        ) {
                            FormField(AppLabels.userIdField, userId, textColor = palette.darkText, onValueChange = onUserIdChange)
                            FormField(AppLabels.displayNameField, displayName, textColor = palette.darkText, onValueChange = onDisplayNameChange)
                            FilledActionButton(AppLabels.continueLabel, onJoin, modifier = Modifier.fillMaxWidth())
                        }
                    }
                } else Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    SplitPanelCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        title = AppLabels.select,
                        subtitle = AppLabels.selectMemberSubtitle,
                        dark = true
                    ) {
                        if (existingMembers.isEmpty()) {
                            EmptyConversationState(AppLabels.noMembers, AppLabels.noMembersBody)
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(existingMembers, key = { it.id }) { member ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable { onContinueAsMember(member) },
                                        shape = RoundedCornerShape(16.dp),
                                        backgroundColor = palette.sidebar,
                                        elevation = 0.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(member.displayName, color = palette.lightText, fontWeight = FontWeight.Bold)
                                                Text("@${member.userId}", color = palette.mutedText)
                                            }
                                            Text(AppLabels.continueLabel, color = palette.accentSoft, fontWeight = FontWeight.Medium)
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
                            .background(palette.mutedText.copy(alpha = 0.22f))
                    )

                    SplitPanelCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        title = AppLabels.create,
                        subtitle = AppLabels.createMemberSubtitle,
                        dark = false
                    ) {
                        FormField(AppLabels.userIdField, userId, textColor = palette.darkText, onValueChange = onUserIdChange)
                        FormField(AppLabels.displayNameField, displayName, textColor = palette.darkText, onValueChange = onDisplayNameChange)
                        Spacer(modifier = Modifier.weight(1f))
                        FilledActionButton(AppLabels.continueLabel, onJoin, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
internal fun WorkspaceListItem(workspace: WorkspaceResponse, onClick: () -> Unit) {
    val palette = appPalette()
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = palette.sidebarCard,
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(workspace.name, color = palette.lightText, fontWeight = FontWeight.Bold)
                Text(workspace.slug, color = palette.mutedText)
            }
            Text(AppLabels.open, color = palette.accentSoft, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
internal fun SplitPanelCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    dark: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = appPalette()
    Card(
        modifier = modifier.widthIn(min = 0.dp),
        shape = RoundedCornerShape(26.dp),
        backgroundColor = if (dark) palette.sidebarCard else palette.mainBg,
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = {
                Text(title, style = MaterialTheme.typography.h5, color = if (dark) palette.lightText else palette.darkText)
                Text(subtitle, color = if (dark) palette.mutedText else palette.dimText)
                content()
            }
        )
    }
}

@Composable
internal fun SplitPanelHeader(
    title: String,
    subtitle: String,
    dark: Boolean,
    backLabel: String,
    onBack: () -> Unit
) {
    val palette = appPalette()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.h4, color = if (dark) palette.darkText else palette.lightText)
            Text(subtitle, color = if (dark) palette.dimText else palette.mutedText)
        }
        Card(
            modifier = Modifier.clickable(onClick = onBack),
            shape = RoundedCornerShape(14.dp),
            backgroundColor = if (dark) palette.overlayCard else palette.sidebarCard,
            elevation = 0.dp
        ) {
            Text(
                backLabel,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = if (dark) palette.darkText else palette.lightText
            )
        }
    }
}
