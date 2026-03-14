# BBS-APP

## Target platform

### Mobile

- android
- iOS

### Desktop

- macOS
- Windows
- Linux

## Project structure

- `androidApp` - Android application by Jetpack Compose
- `desktopApp` - Desktop application by Compose Multiplatform
- `../shared` - Shared Kotlin Multiplatform client code
- `iosApp` - SwiftUI host app consuming the shared framework

Shared request/response models, UI, and client code are provided by the root `:shared` module.

## Build

```bash
# From repository root
./gradlew :shared:build :app:desktopApp:build
```

## Run

### Desktop

```bash
# From repository root (requires server to be running)
./gradlew :app:desktopApp:run
```
