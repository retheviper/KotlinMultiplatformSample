# BBS-MOBILE / BBS-DESKTOP

## Target platform

### BBS-MOBILE

- android
- iOS

### BBS-DESKTOP

- macOS
- Windows
- Linux

## Project structure

- `iosApp` - Client application by SwiftUI (iOS/iPadOS)
- `andoroidApp` - Client application by Jetpack Compose (Android)
- `desktopApp` - Client application by Compose for Desktop (macOS/Windows/Linux)
- `shared` - Shared code between iOS/iPadOS/Android/macOS/Windows/Linux by Kotlin Multiplatform

## Build

```bash
# Build client application (client will automatically build when server is built)
./gradlew build
```

## Run

### Desktop

```bash
# Run application (Requires server to be running)
./gradlew run
```