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
- an Android shell backed by the shared Compose UI
- an iOS SwiftUI shell backed by shared contracts and networking

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
  Android shell using the shared Compose UI

app/desktopApp/
  Compose Desktop shell

app/macosApp/
  macOS SwiftUI shell

app/iosApp/
  iOS SwiftUI shell

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
- Android: implemented with a thin shell in `app/androidApp`, reusing the `shared` Compose UI, state, resources, and networking
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

### 3. Run Android

Start the API first, then use the Gradle shortcut task:

```bash
./gradlew :api:run
./gradlew :app:listAndroidAvds
./gradlew :app:runAndroidEmulator -PandroidAvd=<your-avd-name>
```

If you already have exactly one emulator running, `-PandroidAvd=...` is optional. If multiple emulators are connected, pass `-PandroidDeviceSerial=<adb-serial>`.
If Android builds fail during the `androidJdkImage` / `jlink` step, run Gradle with a standard JDK such as Temurin 21 instead of GraalVM for this repository.

The Android shell connects to `http://10.0.2.2:8080` by default so the emulator can reach the host machine.
It intentionally keeps platform code thin and delegates product UI, state, resources, and networking to `:shared`.
If you launch the emulator from Android Studio instead, start any device from Device Manager before running `installDebug`.
The current default networking setup targets the Android emulator. A physical Android device is not configured out of the box because `10.0.2.2` is emulator-only.

Example:

```bash
JAVA_HOME=/Users/youngbinkim/Library/Java/JavaVirtualMachines/temurin-21.0.9/Contents/Home \
./gradlew :app:runAndroidEmulator -PandroidAvd=<your-avd-name>
```

What happens during startup:

- Flyway applies schema migrations
- the web frontend bundle is prepared from `shared`
- the API serves the web client from `/`

### 4. Open the application

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
./gradlew :app:listAppleSimulators
```

Then choose an installed iPhone simulator name and let Gradle build, install, and launch the app:

```bash
./gradlew :app:runIosSimulator -PiosSimulator="<your-iphone-simulator>"
```

To override the server base URL during simulator launch:

```bash
./gradlew :app:runIosSimulator \
  -PiosSimulator="<your-iphone-simulator>" \
  -Pmessaging.baseUrl=http://localhost:8080
```

Default server target for the iOS shell:

```bash
MESSAGING_BASE_URL=http://localhost:8080
```

The iOS shell defaults to `http://localhost:8080`, which works for the iOS Simulator because it shares the Mac host network.
The Gradle task forwards `-Pmessaging.baseUrl=...` to the simulator launch as `SIMCTL_CHILD_MESSAGING_BASE_URL`.

If you prefer Xcode, open `app/iosApp/iosApp.xcodeproj`, choose any installed iPhone Simulator, set `MESSAGING_BASE_URL` in the Run scheme if needed, and run the `iosApp` scheme.

## iPadOS Simulator Run

Start the API first:

```bash
./gradlew :api:run
./gradlew :app:listAppleSimulators
```

Then choose an installed iPad simulator name and let Gradle build, install, and launch the same target:

```bash
./gradlew :app:runIpadSimulator -PipadSimulator="<your-ipad-simulator>"
```

To override the server base URL during simulator launch:

```bash
./gradlew :app:runIpadSimulator \
  -PipadSimulator="<your-ipad-simulator>" \
  -Pmessaging.baseUrl=http://localhost:8080
```

If you prefer Xcode, open `app/iosApp/iosApp.xcodeproj`, choose any installed iPad Simulator, and run the `iosApp` scheme.

## Test and Verification

Core verification commands:

```bash
./gradlew :shared:jvmTest
./gradlew :api:test
./gradlew :app:androidApp:assembleDebug
./gradlew :app:desktopApp:compileKotlin
swift test --package-path app/macosApp
xcodebuild -project app/iosApp/iosApp.xcodeproj -scheme iosApp -showdestinations
xcodebuild -project app/iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=<installed-iphone-simulator>' build
xcodebuild -project app/iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=<installed-ipad-simulator>' build
```

Notes:

- `:api:test` uses Testcontainers and requires Docker
- iOS simulator names and OS versions must match the runtimes installed in your local Xcode
- `:app:androidApp:assembleDebug` verifies the Android module without requiring a running emulator
- `:app:androidApp:installDebug` requires an available emulator or connected device, but the default base URL is set up for the Android emulator

## Common Local Issues

- `Docker is not running`
  - Start Docker Desktop or your Docker daemon first.
- `Port 5432 already in use`
  - Change the local PostgreSQL mapping or stop the conflicting service.
- `Android build fails`
  - Provide a valid Android SDK via `ANDROID_HOME` or `local.properties`.
- `adb` or `emulator` command is not found
  - Install the Android SDK command-line tools and add the SDK `platform-tools` and `emulator` directories to your `PATH`.
- `No iOS simulator matches the destination`
  - Run `xcodebuild -showdestinations` or `xcrun simctl list devices available` and replace the simulator name with one installed in your Xcode.
- `simctl install` cannot find `iosApp.app`
  - Re-run the `xcodebuild` step and resolve the app path again from `~/Library/Developer/Xcode/DerivedData`.
- `API tests fail on container startup`
  - Check Docker availability and container permissions.
