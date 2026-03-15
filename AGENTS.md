# AGENTS.md

## Purpose

This repository contains a Kotlin Multiplatform workspace-style real-time chat product.

- Product direction: Slack-like collaboration messaging
- Server direction: DDD-oriented modular monolith
- Client direction: shared KMP contracts/UI plus platform-specific shells

Current product shells and modules:

- Ktor API
- `shared` KMP contracts, networking, and Compose UI
- Compose Desktop
- Compose Wasm web client
- macOS SwiftUI native shell
- iOS/iPadOS SwiftUI shell

Legacy BBS sample code is not the product reference implementation and should not drive new design decisions.

Changes should preserve the intended split:

- `shared` owns contracts and reusable networking utilities
- platform shells own platform-specific UI composition
- SwiftUI shells should prefer shared Swift model/helpers where practical

## Current Status

The currently working product surface includes:

- workspace, member, and channel creation/query
- channel messages and thread replies
- WebSocket-based real-time chat
- mention, notification, and reaction flows
- link preview and image preview
- Compose Wasm web client
- Compose Desktop shell
- macOS SwiftUI native shell
- Ktor API with OpenAPI, PostgreSQL, and Flyway

These areas are still thinner shells than the web, desktop, and macOS surfaces:

- `app/androidApp` reuses the shared Compose UI with limited platform-specific behavior
- `app/iosApp` is functional, but still lower priority than web, desktop, and macOS for product depth

## Module Layout

```text
api
  Ktor server
  messaging domain/application/infrastructure/presentation
  database wiring, Flyway, OpenAPI

shared
  shared contracts
  Ktor client
  Compose shared UI/state/resources
  Wasm entry

app/desktopApp
  Compose Desktop shell

app/macosApp
  macOS SwiftUI native shell

app/androidApp
  Android shell backed by shared Compose UI

app/iosApp
  iOS SwiftUI shell
```

## Session Reflection

This repository already showed why version-sensitive work must not rely on memory alone.

- Ktor, Kotlin Multiplatform, SwiftUI, WebKit, and Apple platform behavior changed enough that recalled knowledge was not reliable.
- When behavior depends on current SDK/runtime/framework details, verify the latest official documentation or source before changing code.
- If an issue appears simulator-specific, confirm whether it is a simulator regression before redesigning product behavior around it.

## Required Working Rules

1. Check the latest official docs first when touching:
   - Ktor client/server APIs
   - Kotlin Multiplatform / Kotlin Native interop
   - SwiftUI, WebKit, UserNotifications, and other Apple frameworks
   - Xcode build settings, simulator/runtime behavior, or deployment-target-specific APIs
2. Prefer primary sources over summaries:
   - official docs
   - official API references
   - upstream source code or release notes when docs are ambiguous
3. Treat remembered API behavior as a hypothesis, not a fact.
4. When a platform-specific workaround is needed, document:
   - what is OS/runtime-specific
   - what is the preferred modern API
   - why any fallback still exists
5. Keep SwiftUI-first on Apple shells.
   - Do not introduce UIKit/AppKit for new UI work unless there is a clear, documented blocker.
   - If a compatibility bridge is unavoidable, isolate it and make it easy to delete later.

## Implementation Principles

### 1. Preserve bounded contexts and layers

The primary backend context is currently `messaging`. New backend work should keep this shape:

```text
<context>/
  domain/
  application/
  infrastructure/
  presentation/
```

Rules:

- `domain` must not depend on `infrastructure` or `presentation`
- transaction boundaries belong in `application`
- repositories handle persistence access only
- `presentation` handles HTTP/WebSocket transport only

### 2. Use SOLID as the default design baseline

- Keep rendering, orchestration, and I/O concerns from collapsing into one file
- Extend behavior by adding focused types instead of expanding large conditional branches
- Keep test doubles and production implementations aligned to the same contract
- Expose only the operations a caller actually needs
- Make wiring explicit instead of hiding concrete implementations behind default constructor instantiation

Practical rules:

- Do not instantiate concrete dependencies directly in default constructor arguments
- If a file must remain large, still separate state, rendering, side effects, and pure computations
- Extract pure computations into focused functions or models and add tests where practical

### 3. Shared contract rules

- DTOs and command/event contracts used by both server and client belong in `shared`
- Do not expose server domain models directly as external contracts
- Prefer Kotlin/kotlinx serialization and networking utilities where they fit

### 4. Real-time communication rules

- Chat send/receive and live updates use WebSocket
- query/documentation/admin surfaces remain HTTP JSON
- WebSocket command/event changes must be updated together with `shared` contracts

### 5. Client implementation rules

- Keep product logic in `shared` or other pure logic layers where possible
- Platform shells should focus on host integration and platform UX
- Keep macOS SwiftUI behavior aligned with the Compose shell unless there is a documented reason not to
- Android and iOS are currently lower priority than web, desktop, and macOS for full product work
- In shared Compose UI, keep screen composition and state orchestration separated where practical
- In SwiftUI, avoid turning one store into a monolith; split transport, notifications, sockets, and feature state by concern
- Do not use `runBlocking` in main/product source sets. Treat `runBlocking` as test-only, and prefer suspend propagation or platform-native async bridging for app/runtime code, especially for Wasm and Apple interop

### 6. Naming rules

- Do not put `slack`, `bbs`, or framework/ORM names into package or file names unless they are required external contract/config names
- Prefer domain language

### 7. Deprecation handling

- Fix deprecation warnings in implementation code when practical
- Prefer replacing deprecated APIs over suppressing warnings
- Separate toolchain/plugin warnings from product-code warnings

## Architecture Notes

### Apple shells

- `app/macosApp/Sources/ChatMacNative/Shared/` contains shared SwiftUI helpers/components used by both macOS and iOS
- `app/iosApp/iosApp/` should keep files split by concern:
  - app/root screens
  - workspace shell/navigation
  - channel/thread screens
  - message/reaction components
  - overlay/support UI

### View models

- Keep view state in `ChatStore` and related extensions
- Split transport, notifications, and socket behavior by file/concern rather than growing one monolithic support file
- Prefer small focused SwiftUI views with explicit inputs/actions

## Technical Baseline

- Kotlin 2.3.x
- Gradle version catalog
- current stable Gradle wrapper line
- Ktor 3.x
- Exposed + R2DBC
- PostgreSQL
- Flyway
- Kotlin experimental UUID where appropriate, preferably `Uuid.generateV7()`

## Definition of Done

Work is complete when it satisfies the relevant parts below:

- domain invariants are visible in code and tests
- transaction boundaries are visible in application services
- dependencies are explicit in wiring instead of hidden in concrete defaults
- `shared` contracts are updated when required
- OpenAPI or other public contracts are updated when required
- tests match the scale of the change
- no new deprecation warnings remain in implementation code

## Testing Principles

- Backend: verify domain, application, and routes with Kotest and integration tests
- `shared`: verify pure state transitions and command creation logic with JVM tests
- `app/macosApp`: prefer Swift package tests for pure logic
- Even without full UI E2E coverage, extract and test high-risk state logic

## Current Priorities

1. Stabilize workspace, channel, message, and thread flows
2. Improve unread, notification, and read-state behavior
3. Maintain functional parity across native and shared shells
4. Add direct messages and group conversations
5. Add file attachments
6. Add presence

## Before Merging

- Build the iOS simulator target with `xcodebuild`
- Build the macOS package with `swift build`
- If networking or KMP interop changed, verify the `shared` framework path still builds correctly from Xcode

## References

- `README.md` is the primary public-facing reference
- If this file, local docs, and code disagree, prefer `README.md` and the actual implementation
