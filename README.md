# KotlinMultiplatformSample

[English](./README.md) | [한국어](./README.ko.md) | [日本語](./README.ja.md)

Workspace-based chat application built with Kotlin Multiplatform.

![Concept diagram](./concept.svg)

This repository contains:

- a Ktor API server
- a Compose Multiplatform shared UI
- a Compose Wasm web client served by the API
- a Compose Desktop shell
- a macOS SwiftUI shell
- placeholder mobile shells for future Android and iOS work

The current implementation is a Slack-like vertical slice with workspaces, channels, threaded replies, mentions, notifications, reactions, and link previews.
Notifications are push-driven on Web, Compose Desktop, and the macOS SwiftUI shell.

## Project Structure

```text
api/
  Ktor server
  messaging domain/application/infrastructure/presentation layers
  Flyway migrations
  OpenAPI document

shared/
  shared contracts and client models
  shared Ktor client
  shared Compose UI/state/resources
  Compose Wasm entry point

app/androidApp/
  Android placeholder shell

app/desktopApp/
  Compose Desktop shell

app/macosApp/
  macOS SwiftUI shell

app/iosApp/
  iOS placeholder shell

compose.yaml
  Local PostgreSQL service definition
```

## Tech Stack

- Kotlin Multiplatform
- Ktor 3
- Compose Multiplatform
- PostgreSQL
- R2DBC
- Exposed
- Flyway
- WebSocket chat transport
- Server-Sent Events for notification refresh
- Testcontainers for API integration tests

## Platform Status

- Web: implemented and served by the API
- Desktop: implemented with Compose Desktop
- macOS native: implemented with SwiftUI in `app/macosApp`
- Android: module exists, but the product UI is not implemented yet
- iOS: implemented with a SwiftUI shell in `app/iosApp`, using `shared` for contracts and networking
- iPadOS: implemented with the same `app/iosApp` target, with adaptive iPad layouts and simulator support

## Apple Shell Notes

- iOS and macOS now share Swift models/helpers and several SwiftUI components instead of duplicating platform code.
- The iOS SwiftUI shell is split by concern into root screens, workspace shell, channel/thread screens, message components, and overlay components.
- New Apple-side work should verify the latest official SwiftUI, WebKit, UserNotifications, Ktor, and Kotlin Multiplatform documentation before implementation when behavior is version-sensitive.

## Prerequisites

Required:

- JDK 17 or newer
- Docker with `docker compose`

Optional:

- Android SDK for `:app:androidApp`
- Xcode and Swift 6 toolchain for `app/macosApp`
- Xcode with iOS Simulator runtimes for `app/iosApp`

Verified toolchain in this repository:

- Gradle 9.4.0 via wrapper

## Local Run

### 1. Start PostgreSQL

```bash
docker compose up -d db
```

Stop it with:

```bash
docker compose down
```

Reset local database volume completely:

```bash
docker compose down -v
docker compose up -d db
```

### 2. Run the API server

```bash
./gradlew :api:run
```

By default, the server expects the local PostgreSQL instance from `compose.yaml`.

Supported database environment variables:

```bash
CHAT_JDBC_URL
CHAT_R2DBC_URL
CHAT_DB_USER
CHAT_DB_PASSWORD
```

Example:

```bash
export CHAT_JDBC_URL=jdbc:postgresql://localhost:5432/messaging_app
export CHAT_R2DBC_URL=r2dbc:postgresql://localhost:5432/messaging_app
export CHAT_DB_USER=postgres
export CHAT_DB_PASSWORD=postgres
./gradlew :api:run
```

What happens during startup:

- Flyway applies schema migrations
- the web frontend bundle is prepared from `shared`
- the API serves the web client from `/`

### 3. Open the application

Default endpoints:

- App: [http://localhost:8080/](http://localhost:8080/)
- Swagger UI: [http://localhost:8080/docs](http://localhost:8080/docs)
- OpenAPI: [http://localhost:8080/openapi.yaml](http://localhost:8080/openapi.yaml)
- Health: [http://localhost:8080/health](http://localhost:8080/health)

## Frontend Development Notes

The API serves the web frontend directly. You do not need a separate frontend dev server.

For faster iteration on the Wasm frontend, run the frontend distribution task continuously in one terminal and the API in another:

```bash
./gradlew :shared:wasmJsBrowserDevelopmentExecutableDistribution --continuous
./gradlew :api:run
```

When the Wasm output changes, the running API will serve the updated static files.

## Desktop Run

Start the API first, then launch the desktop shell:

```bash
./gradlew :api:run
./gradlew :app:desktopApp:run
```

The Compose Desktop client connects to `http://localhost:8080` by default.
On macOS, running without an explicit shell selection opens the chooser by default.
The Compose Desktop shell exits when the main window is closed.

To point it at a different server:

```bash
./gradlew -Dmessaging.baseUrl=http://localhost:8080 :app:desktopApp:run
```

On macOS, the desktop launcher can run either the Compose Desktop shell or the SwiftUI native shell.

Shell selection options:

```bash
./gradlew -Dchat.desktop.shell=compose :app:desktopApp:run
./gradlew -Dchat.desktop.shell=chooser :app:desktopApp:run
./gradlew -Dchat.desktop.shell=mac-native :app:desktopApp:run
```

`compose` runs the Compose Desktop shell. On macOS, `mac-native` launches the SwiftUI shell from `app/macosApp`, and `chooser` opens the shell picker explicitly.

You can also use environment variables instead of JVM system properties:

```bash
CHAT_DESKTOP_SHELL=mac-native ./gradlew :app:desktopApp:run
MESSAGING_BASE_URL=http://localhost:8080 ./gradlew :app:desktopApp:run
```

You can also run the SwiftUI shell directly:

```bash
swift run --package-path app/macosApp
```

## iOS Simulator Run

Start the API first:

```bash
./gradlew :api:run
```

Then build the iPhone app for the simulator:

```bash
xcodebuild \
  -project app/iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.3.1' \
  build
```

Boot the simulator, install the app, and launch it:

```bash
xcrun simctl boot "iPhone 17"
xcrun simctl bootstatus "iPhone 17" -b
xcrun simctl install "iPhone 17" ~/Library/Developer/Xcode/DerivedData/iosApp-gfhryxmsbbcerxgboevyilzzzfda/Build/Products/Debug-iphonesimulator/iosApp.app
xcrun simctl launch "iPhone 17" orgIdentifier.iosApp
```

Default server target for the iOS shell:

```bash
MESSAGING_BASE_URL=http://localhost:8080
```

If you prefer Xcode, open `app/iosApp/iosApp.xcodeproj`, choose an iPhone simulator such as `iPhone 17`, and run the `iosApp` scheme.

## iPadOS Simulator Run

Start the API first:

```bash
./gradlew :api:run
```

Then build the same target for an iPad simulator:

```bash
xcodebuild \
  -project app/iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPad Air 13-inch (M3),OS=26.3.1' \
  build
```

Boot the iPad simulator, install the app, and launch it:

```bash
xcrun simctl boot "iPad Air 13-inch (M3)"
xcrun simctl bootstatus "iPad Air 13-inch (M3)" -b
xcrun simctl install "iPad Air 13-inch (M3)" ~/Library/Developer/Xcode/DerivedData/iosApp-gfhryxmsbbcerxgboevyilzzzfda/Build/Products/Debug-iphonesimulator/iosApp.app
xcrun simctl launch "iPad Air 13-inch (M3)" orgIdentifier.iosApp
```

If you prefer Xcode, open `app/iosApp/iosApp.xcodeproj`, choose an iPad simulator such as `iPad Air 13-inch (M3)`, and run the `iosApp` scheme.

## Test and Verification

Core verification commands:

```bash
./gradlew :shared:jvmTest
./gradlew :api:test
./gradlew :app:desktopApp:compileKotlin
swift test --package-path app/macosApp
xcodebuild -project app/iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.3.1' build
xcodebuild -project app/iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPad Air 13-inch (M3),OS=26.3.1' build
```

Notes:

- `:api:test` uses Testcontainers and requires Docker
- iOS simulator names and OS versions must match the runtimes installed in your local Xcode

## Common Local Issues

- `Docker is not running`
  - Start Docker Desktop or your Docker daemon first.
- `Port 5432 already in use`
  - Change the local PostgreSQL mapping or stop the conflicting service.
- `Android build fails`
  - Provide a valid Android SDK via `ANDROID_HOME` or `local.properties`.
- `API tests fail on container startup`
  - Check Docker availability and container permissions.
