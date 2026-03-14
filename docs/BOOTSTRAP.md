# Bootstrap

## What To Build First

Create the first vertical slice with these outcomes:

1. A member can sign in to a workspace.
2. A member can see joined channels.
3. A member can enter one channel and load messages.
4. A member can post a message and reply in a thread.
5. Unread state updates from persisted read markers.

## Initial Technical Setup

### Backend

- keep `api` as one Ktor deployable
- add new bounded-context packages beside legacy packages
- centralize DI composition in a bootstrap area
- use one relational database schema with context-owned tables
- use WebSocket only for realtime delivery, not core write logic

### Shared client

- place business-facing contracts in `app/shared`
- model features by `auth`, `workspace`, `channel`, `conversation`, `message`
- keep DTO mapping in a separate layer from UI state

### Platform clients

- Android/Desktop: Compose UI
- iOS: SwiftUI shell + shared Kotlin state/use cases
- avoid pushing platform lifecycle details into shared domain code

## Suggested First Folder Additions

When implementation starts, prefer adding folders in this order:

```text
api/src/jvmMain/kotlin/com/retheviper/bbs/sharedkernel
api/src/jvmMain/kotlin/com/retheviper/bbs/identityaccess
api/src/jvmMain/kotlin/com/retheviper/bbs/workspace
api/src/jvmMain/kotlin/com/retheviper/bbs/channel
api/src/jvmMain/kotlin/com/retheviper/bbs/conversation
api/src/jvmMain/kotlin/com/retheviper/bbs/message
app/shared/src/commonMain/kotlin/com/retheviper/bbs/core
app/shared/src/commonMain/kotlin/com/retheviper/bbs/feature
```

## Definition Of Ready For New Work

Before implementing a new feature, write down:

- bounded context owner
- aggregate root
- command/query names
- domain invariants
- required events
- transport contract
- test scope

## Near-Term Refactoring Priorities

- isolate legacy `board` flows from new messaging work
- replace ambiguous `common` packages with purpose-specific shared kernel code
- separate domain models from HTTP request/response models more aggressively
- move toward feature-based packages in `app/shared`
