package com.retheviper.chat.messaging.application

import com.retheviper.chat.contract.AddWorkspaceMemberRequest
import com.retheviper.chat.contract.ChannelVisibility
import com.retheviper.chat.contract.CreateChannelRequest
import com.retheviper.chat.contract.CreateWorkspaceRequest
import com.retheviper.chat.contract.LinkPreviewResponse
import com.retheviper.chat.contract.PostMessageRequest
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class MessagingSampleDataBootstrapper(
    private val commandService: MessagingCommandService,
    private val queryService: MessagingQueryService
) {
    suspend fun seedIfNeeded() {
        if (queryService.listWorkspaces().isNotEmpty()) {
            return
        }

        seedAcmeWorkspace()
        seedGlobexWorkspace()
    }

    private suspend fun seedAcmeWorkspace() {
        val workspace = commandService.createWorkspace(
            CreateWorkspaceRequest(
                slug = "acme",
                name = "Acme Product",
                ownerUserId = "u-alice",
                ownerDisplayName = "Alice"
            )
        )
        val bob = commandService.addMember(
            workspace.id,
            AddWorkspaceMemberRequest(
                userId = "u-bob",
                displayName = "Bob"
            )
        )
        val carol = commandService.addMember(
            workspace.id,
            AddWorkspaceMemberRequest(
                userId = "u-carol",
                displayName = "Carol"
            )
        )
        val general = queryService.listChannels(workspace.id).single { it.slug == "general" }
        val design = commandService.createChannel(
            workspace.id,
            CreateChannelRequest(
                slug = "design",
                name = "design",
                topic = "Product design reviews and launch prep",
                visibility = ChannelVisibility.PUBLIC,
                createdByMemberId = workspace.ownerMemberId.toString()
            )
        )
        commandService.createChannel(
            workspace.id,
            CreateChannelRequest(
                slug = "support-desk",
                name = "support-desk",
                topic = "Customer issues and incident handoff",
                visibility = ChannelVisibility.PUBLIC,
                createdByMemberId = workspace.ownerMemberId.toString()
            )
        )

        val kickoff = commandService.postChannelMessage(
            general.id,
            PostMessageRequest(
                authorMemberId = workspace.ownerMemberId.toString(),
                body = "Morning sync is live. @u-bob please share the rollout note https://example.com/launch-plan",
                linkPreview = LinkPreviewResponse(
                    url = "https://example.com/launch-plan",
                    title = "Launch plan",
                    description = "Checklist for the public beta rollout",
                    siteName = "Example"
                )
            )
        )
        commandService.replyToMessage(
            kickoff.id,
            PostMessageRequest(
                authorMemberId = bob.id.toString(),
                body = "On it. Draft is ready for review."
            )
        )
        commandService.replyToMessage(
            kickoff.id,
            PostMessageRequest(
                authorMemberId = carol.id.toString(),
                body = "I've added the final design callouts."
            )
        )
        commandService.toggleReaction(kickoff.id, bob.id, "👍")
        commandService.toggleReaction(kickoff.id, carol.id, "🚀")

        val reviewThread = commandService.postChannelMessage(
            design.id,
            PostMessageRequest(
                authorMemberId = carol.id.toString(),
                body = "Latest mock exported. @u-alice can you confirm the navigation states?"
            )
        )
        commandService.replyToMessage(
            reviewThread.id,
            PostMessageRequest(
                authorMemberId = workspace.ownerMemberId.toString(),
                body = "Looks good. Ship the revised desktop header."
            )
        )
    }

    private suspend fun seedGlobexWorkspace() {
        val workspace = commandService.createWorkspace(
            CreateWorkspaceRequest(
                slug = "globex",
                name = "Globex Ops",
                ownerUserId = "u-dana",
                ownerDisplayName = "Dana"
            )
        )
        val erin = commandService.addMember(
            workspace.id,
            AddWorkspaceMemberRequest(
                userId = "u-erin",
                displayName = "Erin"
            )
        )
        val general = queryService.listChannels(workspace.id).single { it.slug == "general" }
        val incidents = commandService.createChannel(
            workspace.id,
            CreateChannelRequest(
                slug = "incidents",
                name = "incidents",
                topic = "Operational alerts and response tracking",
                visibility = ChannelVisibility.PUBLIC,
                createdByMemberId = workspace.ownerMemberId.toString()
            )
        )

        commandService.postChannelMessage(
            general.id,
            PostMessageRequest(
                authorMemberId = workspace.ownerMemberId.toString(),
                body = "Welcome to the ops workspace. @u-erin owns the incident summary today."
            )
        )
        val incident = commandService.postChannelMessage(
            incidents.id,
            PostMessageRequest(
                authorMemberId = erin.id.toString(),
                body = "API latency spike detected in ap-northeast. Mitigation is in progress."
            )
        )
        commandService.replyToMessage(
            incident.id,
            PostMessageRequest(
                authorMemberId = workspace.ownerMemberId.toString(),
                body = "Escalate if the p95 stays above threshold for another 10 minutes."
            )
        )
    }
}
