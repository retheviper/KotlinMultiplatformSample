# CHAT-API

## Project structure

- `src/jvmMain` - Ktor API server
  - `messaging/domain` - domain model and repository ports
  - `messaging/application` - command/query orchestration and transaction boundaries
  - `messaging/infrastructure/persistence` - Exposed R2DBC persistence
  - `messaging/presentation` - HTTP, SSE, and WebSocket routes
- `src/jvmTest` - JVM tests

Shared request/response models, client code, and shared UI contracts are provided by the root `:shared` module.

## Build

```bash
# From repository root
./gradlew :api:build
```

## Run

```bash
# Run DB
docker compose up -d db

# From repository root
./gradlew :api:run
```

At startup the API:

- applies Flyway migrations
- serves the OpenAPI document and Swagger UI
- serves the Wasm web client from `/`
- exposes WebSocket chat transport and notification SSE streams
- exposes an MCP Streamable HTTP endpoint at `/mcp`

Current MCP tools:

- `get_health`
- `list_workspaces`
- `create_workspace`
- `get_workspace_by_slug`
- `list_workspace_channels`
- `create_channel`
- `list_members`
- `add_member`
- `update_member`
- `list_channel_messages`
- `get_thread`
- `post_message`
- `reply_message`
- `toggle_reaction`
- `list_notifications`
- `mark_notifications_read`

Manual CLI test helper:

```bash
./scripts/mcp_cli.py init
./scripts/mcp_cli.py tools
./scripts/mcp_cli.py call list_workspaces
```
