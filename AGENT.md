# AGENT.md

## Objective

Build a Slack-like messaging product on top of this repository using Kotlin Multiplatform, Domain-Driven Design, and a modular monolith backend.

The current codebase is a BBS sample. Treat it as a technical starting point, not as the target architecture.

## Product Scope

The first meaningful product slice is:

- workspace creation and membership
- channels
- direct messages
- threaded messages
- reactions
- unread tracking
- presence and notifications as internal modules

Out of scope for the first slice:

- external federation
- microservices
- plugin marketplace
- end-to-end encrypted multi-device sync

## Architectural Stance

### 1. Modular monolith first

- Keep the backend as a single deployable application.
- Enforce boundaries through packages, interfaces, module tests, and dependency rules.
- Do not split into microservices until a concrete operational bottleneck is proven.

### 2. DDD by bounded context

Design around business capabilities, not technical layers.

Initial bounded contexts:

- `identity-access`
- `workspace`
- `channel`
- `conversation`
- `message`
- `notification`
- `presence`
- `search`
- `file`

Shared kernel should stay small:

- ids
- time
- domain events
- result/error abstractions

### 3. Kotlin Multiplatform discipline

- Put pure business rules in shared Kotlin where practical.
- Keep platform UI, persistence drivers, and OS integrations outside shared domain code.
- Favor shared application contracts and DTOs over leaking server persistence models into clients.

## Required Module Shape

Each backend bounded context should converge toward this internal structure:

```text
<context>/
  domain/
    model/
    service/
    event/
    repository/
  application/
    command/
    query/
    handler/
    dto/
  infrastructure/
    persistence/
    external/
    config/
  presentation/
    http/
    websocket/
```

Rules:

- `domain` must not depend on `infrastructure` or `presentation`.
- `application` can depend on `domain`.
- `infrastructure` implements domain/application ports.
- `presentation` only orchestrates transport concerns.
- Cross-context access must go through explicit application services or domain events.

## Ubiquitous Language

Use these terms consistently:

- `Workspace`: tenant boundary
- `Member`: user belonging to a workspace
- `Channel`: group conversation space inside a workspace
- `Conversation`: DM or group DM container
- `Thread`: reply chain rooted in one message
- `Message`: immutable authored content with editable metadata
- `Reaction`: lightweight member response to a message
- `ReadReceipt`: member's last seen marker
- `Presence`: ephemeral online/activity state

Avoid reusing legacy BBS terms such as `board` or `article` in new code.

## Implementation Priorities

Build in this order unless a task explicitly requires otherwise:

1. shared kernel
2. identity and workspace onboarding
3. channel and membership model
4. messaging and threads
5. read model queries
6. notifications and presence
7. file attachments and search

## Current Repository Mapping

- `api`: backend monolith candidate
- `app/shared`: KMP shared client code candidate
- `app/androidApp`, `app/desktopApp`, `app/iosApp`: platform presentation shells

Near-term direction:

- migrate legacy `board` and `message` areas toward the bounded contexts above
- keep one database and one backend deployable
- introduce clearer package boundaries before introducing new features broadly

## Delivery Rules For Agents

- Prefer small vertical slices over broad rewrites.
- When adding a feature, define the domain language first.
- Add tests for aggregates, application services, and module boundary behavior.
- Do not introduce cross-context repository access without an explicit port.
- Do not add new global util packages unless they belong in the shared kernel.
- Do not rename large legacy areas unless the task needs it; isolate and replace incrementally.

## Minimum Done Criteria

A feature is not done unless:

- domain invariants are explicit
- application use cases are named as commands/queries
- transport models are separated from domain models
- tests cover the main business rule path
- the change respects bounded-context boundaries

## Working References

- [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md)
- [docs/BOOTSTRAP.md](./docs/BOOTSTRAP.md)
- [docs/DOMAIN_MODEL.md](./docs/DOMAIN_MODEL.md)
