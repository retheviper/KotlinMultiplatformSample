# AGENTS.md

## Purpose

This repository contains a Kotlin Multiplatform chat product with multiple shells:

- Ktor API
- shared KMP contracts, networking, and Compose UI
- Compose Desktop
- macOS SwiftUI
- iOS/iPadOS SwiftUI

Changes should preserve the intended split:

- `shared` owns contracts and reusable networking utilities
- platform shells own platform-specific UI composition
- SwiftUI shells should prefer shared Swift model/helpers where practical

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

## Architecture Notes

### Apple shells

- `app/macosApp/Sources/ChatMacNative/Shared/` contains shared SwiftUI helpers/components used by both macOS and iOS.
- `app/iosApp/iosApp/` should keep files split by concern:
  - app/root screens
  - workspace shell/navigation
  - channel/thread screens
  - message/reaction components
  - overlay/support UI

### View models

- Keep view state in `ChatStore` and related extensions.
- Split transport, notifications, and socket behavior by file/concern rather than growing one monolithic support file.
- Prefer small focused SwiftUI views with explicit inputs/actions.

## Before Merging

- Build iOS simulator target with `xcodebuild`
- Build macOS package with `swift build`
- If networking or KMP interop changed, verify the `shared` framework path still builds correctly from Xcode
