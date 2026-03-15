# CHAT-APP

## Platform shells

- `androidApp` - Android shell backed by the shared Compose UI
- `desktopApp` - Compose Desktop shell for macOS, Windows, and Linux
- `macosApp` - macOS SwiftUI native shell
- `iosApp` - iOS SwiftUI shell

## Project structure

- `androidApp` - Thin Android launcher module
- `desktopApp` - Compose Multiplatform desktop launcher
- `macosApp` - Swift package for the native macOS shell
- `../shared` - Shared Kotlin Multiplatform client code
- `iosApp` - iOS SwiftUI module

Shared request/response models, UI, and client code are provided by the root `:shared` module.

## Build

```bash
# From repository root
./gradlew :shared:build :app:desktopApp:build
```

## Run

### Android

```bash
# From repository root (requires server to be running and Android SDK configured)
./gradlew :app:listAndroidAvds
./gradlew :app:runAndroidEmulator -PandroidAvd=<your-avd-name>
```

### Desktop

```bash
# From repository root (requires server to be running)
./gradlew :app:desktopApp:run
```

### macOS native

```bash
# Requires server to be running
swift run --package-path app/macosApp
```
