# BBS-SERVER

## Project structure

- `src/jvmMain` - Ktor API server
- `src/jvmTest` - JVM tests

Shared request/response models and constants are provided by the root `:contract` module.

## Build

```bash
# From repository root
./gradlew :api:build
```

## Run

```bash
# Run DB
docker-compose up db -d

# From repository root
./gradlew :api:run
```
