# CHAT-API

## Project structure

- `src/jvmMain` - Ktor API server
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
