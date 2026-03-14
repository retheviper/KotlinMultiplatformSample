# Architecture

## Goal

Evolve this sample into a Slack-like collaboration product with:

- Kotlin Multiplatform clients
- a single Kotlin backend deployable
- DDD bounded contexts
- modular monolith boundaries

## System Shape

```text
Clients
  Android (Compose)
  Desktop (Compose Multiplatform)
  iOS (SwiftUI + shared KMP)
        |
        v
API Monolith (Ktor)
  identity-access
  workspace
  channel
  conversation
  message
  notification
  presence
  search
  file
        |
        v
Single relational database
```

## Bounded Contexts

### identity-access

- sign in
- auth session
- workspace-scoped membership identity
- authorization policies

### workspace

- workspace lifecycle
- invitations
- member onboarding
- roles

### channel

- channel creation
- channel membership
- public/private visibility
- default channels

### conversation

- direct messages
- group direct messages
- participant membership

### message

- message posting
- edit/delete policy
- threads
- reactions
- read receipts

### notification

- unread counts
- mention notifications
- delivery preferences

### presence

- online state
- last active
- typing indicators

### search

- indexing policy
- message/channel search queries

### file

- upload metadata
- attachment ownership
- retention policy

## Dependency Rules

Allowed dependencies should remain narrow:

- `identity-access` -> shared kernel
- `workspace` -> shared kernel, identity-access
- `channel` -> shared kernel, workspace
- `conversation` -> shared kernel, workspace
- `message` -> shared kernel, workspace, channel, conversation
- `notification` -> shared kernel, message, identity-access
- `presence` -> shared kernel, identity-access
- `search` -> shared kernel, message, channel, conversation
- `file` -> shared kernel, workspace, message

When a dependency is needed, prefer:

1. application port
2. domain event subscription
3. read-only query model

Direct table or repository access across contexts is not allowed.

## Tactical DDD Patterns

Use selectively, not mechanically:

- Aggregates: `Workspace`, `Channel`, `Conversation`, `MessageThread`
- Value objects: `WorkspaceId`, `ChannelId`, `MessageId`, `MemberId`
- Domain services: authorization and policy decisions spanning aggregates
- Domain events: `MessagePosted`, `ReactionAdded`, `MemberJoinedWorkspace`
- Application services: use-case orchestration and transaction boundaries

## Recommended Package Direction

### Backend

```text
api/src/jvmMain/kotlin/.../
  sharedkernel/
  identityaccess/
  workspace/
  channel/
  conversation/
  message/
  notification/
  presence/
  search/
  file/
  bootstrap/
```

### Shared client code

```text
app/shared/src/commonMain/kotlin/.../
  core/
  feature/auth/
  feature/workspace/
  feature/channel/
  feature/conversation/
  feature/message/
```

### Platform apps

- keep view models / presenters close to each platform shell
- keep OS integrations platform-specific
- reuse shared application contracts where possible

## Migration Notes From Current Sample

Current legacy areas:

- `board` maps poorly to the new product and should not be extended
- current `message` package can inform transport and persistence, but not final domain language
- common DTO packages can be reused selectively after renaming to new ubiquitous language

Migration strategy:

1. preserve running sample behavior
2. add new bounded-context packages beside legacy code
3. implement new slices in the new structure only
4. retire legacy packages after replacement
