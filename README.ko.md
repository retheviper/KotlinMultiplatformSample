# KotlinMultiplatformSample

[English](./README.md) | [한국어](./README.ko.md) | [日本語](./README.ja.md)

Kotlin Multiplatform 기반의 워크스페이스형 채팅 애플리케이션 저장소입니다.

![개념 다이어그램](./concept.svg)

이 저장소에는 다음이 포함됩니다.

- Ktor API 서버
- Compose Multiplatform 공용 UI
- API가 직접 서빙하는 Compose Wasm 웹 클라이언트
- Compose Desktop 셸
- macOS SwiftUI 셸
- iOS/iPadOS SwiftUI 셸

현재 구현 범위는 Slack과 유사한 워크스페이스, 채널, 스레드, 멘션, 알림, 리액션, 링크 미리보기까지 포함한 수직 슬라이스입니다.
알림 갱신은 Web, Compose Desktop, macOS SwiftUI 셸에서 push 방식으로 동작합니다.

## 프로젝트 구조

```text
api/
  Ktor 서버
  messaging domain/application/infrastructure/presentation 레이어
  Flyway 마이그레이션
  OpenAPI 문서

shared/
  공용 계약 및 클라이언트 모델
  공용 Ktor client
  공용 Compose UI/state/resources
  Compose Wasm 진입점

app/androidApp/
  Android placeholder 셸

app/desktopApp/
  Compose Desktop 셸

app/macosApp/
  macOS SwiftUI 셸

app/iosApp/
  iOS/iPadOS SwiftUI 셸

compose.yaml
  로컬 PostgreSQL 정의
```

## 기술 스택

- Kotlin Multiplatform
- Ktor 3
- Compose Multiplatform
- PostgreSQL
- R2DBC
- Exposed
- Flyway
- WebSocket 기반 채팅
- 알림 갱신용 Server-Sent Events
- Testcontainers 기반 API 통합 테스트

## 플랫폼 상태

- Web: 구현 완료, API가 직접 서빙
- Desktop: Compose Desktop으로 구현 완료
- macOS native: `app/macosApp`의 SwiftUI 셸로 구현 완료
- Android: 모듈은 있지만 제품 UI는 아직 미구현
- iOS: `app/iosApp`의 SwiftUI 셸로 구현 완료, contract와 networking은 `shared` 사용
- iPadOS: 같은 `app/iosApp` 타깃으로 구현 완료, iPad 적응형 레이아웃과 시뮬레이터 실행 지원

## Apple 셸 메모

- iOS와 macOS는 Swift 모델/헬퍼와 일부 SwiftUI 컴포넌트를 공용으로 사용하도록 정리되어 있습니다.
- iOS SwiftUI 셸은 루트 화면, 워크스페이스 셸, 채널/스레드 화면, 메시지 컴포넌트, 오버레이 컴포넌트로 역할을 나눠 관리합니다.
- Apple 플랫폼 작업이 SDK 버전이나 런타임 동작에 민감할 때는 구현 전에 SwiftUI, WebKit, UserNotifications, Ktor, Kotlin Multiplatform의 최신 공식 문서를 먼저 확인하는 것을 기본 원칙으로 합니다.

## 사전 준비

필수:

- JDK 17 이상
- `docker compose` 사용 가능한 Docker 환경

선택:

- `:app:androidApp` 실행을 위한 Android SDK
- `app/macosApp` 실행을 위한 Xcode 및 Swift 6 toolchain
- `app/iosApp` 실행을 위한 Xcode 및 iOS Simulator runtime

이 저장소에서 확인한 wrapper 기준 버전:

- Gradle 9.4.0

## 로컬 실행

### 1. PostgreSQL 실행

```bash
docker compose up -d db
```

중지:

```bash
docker compose down
```

볼륨까지 초기화:

```bash
docker compose down -v
docker compose up -d db
```

### 2. API 서버 실행

```bash
./gradlew :api:run
```

기본적으로 `compose.yaml`로 실행한 로컬 PostgreSQL을 사용합니다.

지원 환경 변수:

```bash
CHAT_JDBC_URL
CHAT_R2DBC_URL
CHAT_DB_USER
CHAT_DB_PASSWORD
```

예시:

```bash
export CHAT_JDBC_URL=jdbc:postgresql://localhost:5432/messaging_app
export CHAT_R2DBC_URL=r2dbc:postgresql://localhost:5432/messaging_app
export CHAT_DB_USER=postgres
export CHAT_DB_PASSWORD=postgres
./gradlew :api:run
```

기동 시 수행되는 작업:

- Flyway 마이그레이션 적용
- `shared`의 웹 프런트 번들 준비
- `/` 경로에서 웹 앱 서빙

### 3. 애플리케이션 접속

기본 엔드포인트:

- 앱: [http://localhost:8080/](http://localhost:8080/)
- Swagger UI: [http://localhost:8080/docs](http://localhost:8080/docs)
- OpenAPI: [http://localhost:8080/openapi.yaml](http://localhost:8080/openapi.yaml)
- Health: [http://localhost:8080/health](http://localhost:8080/health)

## 프런트엔드 개발 메모

웹 프런트는 API 서버가 직접 서빙하므로 별도 프런트 dev server는 필요하지 않습니다.

Wasm 반복 속도를 높이려면 다음처럼 두 개의 터미널을 쓰는 편이 좋습니다.

```bash
./gradlew :shared:wasmJsBrowserDevelopmentExecutableDistribution --continuous
./gradlew :api:run
```

## Desktop 실행

먼저 API를 실행한 뒤 Desktop 셸을 실행합니다.

```bash
./gradlew :api:run
./gradlew :app:desktopApp:run
```

Compose Desktop 클라이언트는 기본적으로 `http://localhost:8080`에 연결합니다.
macOS에서는 셸을 지정하지 않고 실행하면 기본적으로 chooser가 열립니다.
Compose Desktop 셸은 메인 창을 닫으면 프로세스가 종료됩니다.

다른 서버를 쓰려면:

```bash
./gradlew -Dmessaging.baseUrl=http://localhost:8080 :app:desktopApp:run
```

macOS에서는 Compose Desktop 셸과 SwiftUI 네이티브 셸을 모두 실행할 수 있습니다.

실행 시 셸 선택:

```bash
./gradlew -Dchat.desktop.shell=compose :app:desktopApp:run
./gradlew -Dchat.desktop.shell=chooser :app:desktopApp:run
./gradlew -Dchat.desktop.shell=mac-native :app:desktopApp:run
```

`compose`는 Compose Desktop 셸을 실행합니다. macOS에서는 `mac-native`가 `app/macosApp`의 SwiftUI 셸을 실행하고, `chooser`는 셸 선택 창을 명시적으로 엽니다.

JVM system property 대신 환경 변수로도 지정할 수 있습니다.

```bash
CHAT_DESKTOP_SHELL=mac-native ./gradlew :app:desktopApp:run
MESSAGING_BASE_URL=http://localhost:8080 ./gradlew :app:desktopApp:run
```

SwiftUI 셸만 직접 실행할 수도 있습니다.

```bash
swift run --package-path app/macosApp
```

## iOS 시뮬레이터 실행

먼저 API를 실행합니다.

```bash
./gradlew :api:run
```

그 다음 iPhone 시뮬레이터용으로 앱을 빌드합니다.

```bash
xcodebuild \
  -project app/iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.3.1' \
  build
```

시뮬레이터를 부팅하고 앱을 설치한 뒤 실행합니다.

```bash
xcrun simctl boot "iPhone 17"
xcrun simctl bootstatus "iPhone 17" -b
xcrun simctl install "iPhone 17" ~/Library/Developer/Xcode/DerivedData/iosApp-gfhryxmsbbcerxgboevyilzzzfda/Build/Products/Debug-iphonesimulator/iosApp.app
xcrun simctl launch "iPhone 17" orgIdentifier.iosApp
```

iOS 셸의 기본 서버 주소:

```bash
MESSAGING_BASE_URL=http://localhost:8080
```

Xcode를 사용할 경우 `app/iosApp/iosApp.xcodeproj`를 열고 `iosApp` 스킴에서 `iPhone 17` 같은 iPhone Simulator를 선택해 실행하면 됩니다.

## iPadOS 시뮬레이터 실행

먼저 API를 실행합니다.

```bash
./gradlew :api:run
```

같은 타깃을 iPad 시뮬레이터용으로 빌드합니다.

```bash
xcodebuild \
  -project app/iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPad Air 13-inch (M3),OS=26.3.1' \
  build
```

iPad 시뮬레이터를 부팅하고 앱을 설치한 뒤 실행합니다.

```bash
xcrun simctl boot "iPad Air 13-inch (M3)"
xcrun simctl bootstatus "iPad Air 13-inch (M3)" -b
xcrun simctl install "iPad Air 13-inch (M3)" ~/Library/Developer/Xcode/DerivedData/iosApp-gfhryxmsbbcerxgboevyilzzzfda/Build/Products/Debug-iphonesimulator/iosApp.app
xcrun simctl launch "iPad Air 13-inch (M3)" orgIdentifier.iosApp
```

Xcode를 사용할 경우 `app/iosApp/iosApp.xcodeproj`를 열고 `iosApp` 스킴에서 `iPad Air 13-inch (M3)` 같은 iPad Simulator를 선택해 실행하면 됩니다.

Wasm 출력물이 갱신되면, 실행 중인 API 서버가 새 정적 파일을 서빙합니다.

## 테스트 및 검증

주요 검증 명령:

```bash
./gradlew :shared:jvmTest
./gradlew :api:test
./gradlew :app:desktopApp:compileKotlin
swift test --package-path app/macosApp
xcodebuild -project app/iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.3.1' build
xcodebuild -project app/iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPad Air 13-inch (M3),OS=26.3.1' build
```

메모:

- `:api:test`는 Testcontainers를 사용하므로 Docker가 필요합니다.
- iOS 시뮬레이터 이름과 OS 버전은 로컬 Xcode에 설치된 runtime에 맞게 바꿔야 합니다.

참고:

- `:api:test`는 Docker가 필요합니다.
- Android와 iOS 빌드는 위 기본 검증 명령에 포함되어 있지 않습니다.

## 자주 발생하는 로컬 문제

- `Docker is not running`
  - Docker Desktop 또는 Docker daemon을 먼저 실행합니다.
- `Port 5432 already in use`
  - 충돌 중인 로컬 DB를 중지하거나 포트를 변경합니다.
- `Android build fails`
  - `ANDROID_HOME` 또는 `local.properties`에 올바른 SDK 경로를 지정합니다.
- `API tests fail on container startup`
  - Docker 접근 가능 여부와 컨테이너 실행 권한을 확인합니다.
