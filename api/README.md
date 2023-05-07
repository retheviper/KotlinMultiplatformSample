# BBS-SERVER

## Project structure

- `api/js` - Client application by Compose Web
- `api/jvm` - API server by Ktor
- `api/common` - Shared code between client and server by Kotlin Multiplatform

## Build

```bash
# Build client application (client will automatically build when server is built)
./gradlew build
```

## Run

```bash
# Run server (client will automatically build when server is run)
./gradlew run
```