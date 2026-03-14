# Kotlin Multiplatform Sample (Working)

## TL;DR

Web / Desktop / Mobile BBS Application sample by Kotlin. (And Swift)

## Direction

This repository is being repositioned toward a Slack-like messaging product built with Kotlin Multiplatform.

- Architecture: DDD + modular monolith
- Backend style: one deployable monolith with strong internal module boundaries
- Client style: shared KMP domain/application models with platform-specific presentation
- Primary references: [AGENT.md](./AGENT.md), [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md), [docs/BOOTSTRAP.md](./docs/BOOTSTRAP.md)

## Diagram

![diagram](./concept.svg)

## Used

- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-mpp/) for Desktop / Web
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for Android
- [Kotlin Multiplatform Mobile](https://kotlinlang.org/lp/mobile/) for iOS / iPadOS / Android
- [SwiftUI](https://developer.apple.com/xcode/swiftui/) for iOS / iPadOS / macOS
- [Ktor](https://ktor.io/) for HTTP Client / Server
- [Koin](https://insert-koin.io/) for DI
- [Exposed](https://github.com/JetBrains/Exposed) for ORM

and etc.
