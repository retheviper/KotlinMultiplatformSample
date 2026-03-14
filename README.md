# KotlinMultiplatformSample

[English](./README.md) | [한국어](./README.ko.md) | [日本語](./README.ja.md)

Workspace-based chat application built with Kotlin Multiplatform.

![Concept diagram](./concept.svg)

This repository contains:

- a Ktor API server
- a Compose Multiplatform shared UI
- a Compose Wasm web client served by the API
- thin Android and Desktop shells

The current implementation is a Slack-like vertical slice with workspaces, channels, threaded replies, mentions, notifications, reactions, and link previews.

## Audience

This README is written for:

- developers extending the product
- testers running the stack locally
- reviewers who need a quick picture of the architecture and startup flow

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
  Android launcher shell

app/desktopApp/
  Desktop launcher shell

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
- Testcontainers for API integration tests

## Prerequisites

Required:

- JDK 17 or newer
- Docker with `docker compose`

Optional:

- Android SDK for `:app:androidApp`

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

The desktop client connects to `http://localhost:8080` by default.

To point it at a different server:

```bash
./gradlew :app:desktopApp:run -Dmessaging.baseUrl=http://localhost:8080
```

On macOS, the desktop launcher also leaves room for a future native shell alongside the current Compose shell.

Shell selection options:

```bash
./gradlew :app:desktopApp:run -Dchat.desktop.shell=compose
./gradlew :app:desktopApp:run -Dchat.desktop.shell=chooser
./gradlew :app:desktopApp:run -Dchat.desktop.shell=mac-native
```

Today, `compose` is the implemented shell. `mac-native` is a reserved future path for a SwiftUI-based shell and currently falls back to Compose.

## Test and Verification

Core verification commands:

```bash
./gradlew :shared:jvmTest
./gradlew :api:test
./gradlew :app:desktopApp:compileKotlin
```

Notes:

- `:api:test` uses Testcontainers and requires Docker
- Android compilation is not included in the commands above

## Functional Scope Today

Implemented:

- workspace creation and listing
- workspace join through existing or new member identity
- automatic `#general` channel creation
- channel creation and switching
- WebSocket message posting
- threaded replies
- mentions and mention notifications
- notification center and toast notifications
- emoji reactions
- clickable links in messages
- OG-based link previews
- Compose Wasm frontend served by the API
- shared image and font resources for the frontend

## Common Local Issues

- `Docker is not running`
  - Start Docker Desktop or your Docker daemon first.
- `Port 5432 already in use`
  - Change the local PostgreSQL mapping or stop the conflicting service.
- `Android build fails`
  - Provide a valid Android SDK via `ANDROID_HOME` or `local.properties`.
- `API tests fail on container startup`
  - Check Docker availability and container permissions.

## Repository Policy

Internal working notes such as `AGENT.md` and the `docs/` directory are intentionally excluded from the public repository flow and are not part of this README navigation.
