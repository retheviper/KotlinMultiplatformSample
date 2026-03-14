# Domain Model

## Core Entities

### Workspace

- owns channels, conversations, members, and policies
- tenant boundary for authorization and data partitioning

### Member

- a user within a workspace
- may have roles such as owner, admin, member, guest

### Channel

- workspace-scoped conversation space
- has visibility, topic, membership policy, and archival state

### Conversation

- DM or group DM
- has participants and lifecycle state

### Message

- authored by a member in a channel or conversation
- may have thread parent, attachments, reactions, and edit metadata

### Thread

- rooted in a parent message
- groups replies and unread state

### Reaction

- member intent attached to a message
- unique per member/message/emoji

### ReadReceipt

- tracks member progress in a channel, conversation, or thread

## Example Invariants

- a member can post only in a workspace they belong to
- a private channel can only be read by its members
- a message reply must belong to the same channel or conversation as its root
- a reaction cannot exist without its target message
- unread counts derive from durable read markers, not ad hoc client math

## Example Commands

- `CreateWorkspace`
- `InviteMember`
- `CreateChannel`
- `JoinChannel`
- `StartDirectConversation`
- `PostMessage`
- `ReplyInThread`
- `AddReaction`
- `MarkChannelRead`

## Example Queries

- `GetWorkspaceHome`
- `ListSidebarChannels`
- `GetChannelMessages`
- `GetThread`
- `GetUnreadSummary`

## Event Candidates

- `WorkspaceCreated`
- `MemberInvited`
- `MemberJoinedWorkspace`
- `ChannelCreated`
- `ChannelMemberJoined`
- `ConversationStarted`
- `MessagePosted`
- `ThreadReplyPosted`
- `ReactionAdded`
- `ChannelReadMarkerUpdated`
