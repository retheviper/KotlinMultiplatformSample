# KotlinMultiplatformSample

[English](./README.md) | [한국어](./README.ko.md) | [日本語](./README.ja.md)

Kotlin Multiplatform 기반의 워크스페이스형 채팅 애플리케이션 저장소입니다.

![개념 다이어그램](./concept.svg)

이 저장소에는 다음이 포함됩니다.

- Ktor API 서버
- Compose Multiplatform 공용 UI
- API가 직접 서빙하는 Compose Wasm 웹 클라이언트
- Compose Desktop 셸
- `shared` Compose UI를 사용하는 Android 셸
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
  `shared` Compose UI를 사용하는 Android 셸

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
- Android: `app/androidApp`의 얇은 셸로 구현 완료, UI/상태/리소스/네트워킹은 `shared`를 재사용
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

### 3. Android 실행

먼저 API를 실행한 뒤 Gradle 단축 태스크를 사용합니다.

```bash
./gradlew :api:run
./gradlew :app:listAndroidAvds
./gradlew :app:runAndroidEmulator -PandroidAvd=<your-avd-name>
```

이미 실행 중인 에뮬레이터가 정확히 하나라면 `-PandroidAvd=...`는 생략할 수 있습니다. 여러 에뮬레이터가 연결돼 있으면 `-PandroidDeviceSerial=<adb-serial>`을 넘기면 됩니다.
`androidJdkImage` / `jlink` 단계에서 Android 빌드가 실패하면, 이 저장소에서는 GraalVM 대신 Temurin 21 같은 일반 JDK로 Gradle을 실행하세요.

Android 셸은 에뮬레이터에서 호스트 머신의 서버에 접근할 수 있도록 기본값으로 `http://10.0.2.2:8080`에 연결합니다.
플랫폼 코드는 얇게 유지하고, 제품 UI/상태/리소스/네트워킹은 `:shared`에 위임하는 구성을 기본으로 합니다.
Android Studio를 쓴다면 Device Manager에서 에뮬레이터를 먼저 실행한 뒤 `installDebug`를 수행해도 됩니다.
현재 기본 네트워크 설정은 Android 에뮬레이터 기준입니다. `10.0.2.2`는 에뮬레이터 전용이므로, 실기기는 별도 설정 없이 바로 연결되지는 않습니다.

예시:

```bash
JAVA_HOME=/Users/youngbinkim/Library/Java/JavaVirtualMachines/temurin-21.0.9/Contents/Home \
./gradlew :app:runAndroidEmulator -PandroidAvd=<your-avd-name>
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

### 4. 애플리케이션 접속

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
./gradlew :app:listAppleSimulators
```

그 다음 설치된 iPhone 시뮬레이터 이름을 골라 Gradle로 빌드, 설치, 실행을 한 번에 수행합니다.

```bash
./gradlew :app:runIosSimulator -PiosSimulator="<your-iphone-simulator>"
```

시뮬레이터 실행 시 서버 주소를 바꾸려면:

```bash
./gradlew :app:runIosSimulator \
  -PiosSimulator="<your-iphone-simulator>" \
  -Pmessaging.baseUrl=http://localhost:8080
```

iOS 셸의 기본 서버 주소:

```bash
MESSAGING_BASE_URL=http://localhost:8080
```

iOS 셸은 기본적으로 `http://localhost:8080`에 연결하며, iOS 시뮬레이터는 Mac 호스트 네트워크를 공유하므로 이 기본값이 그대로 동작합니다.
Gradle 태스크는 `-Pmessaging.baseUrl=...` 값을 `SIMCTL_CHILD_MESSAGING_BASE_URL`로 전달합니다.

Xcode를 사용할 경우 `app/iosApp/iosApp.xcodeproj`를 열고 설치된 iPhone Simulator를 선택한 뒤, 필요하면 Run scheme에 `MESSAGING_BASE_URL`을 지정해서 실행하면 됩니다.

## iPadOS 시뮬레이터 실행

먼저 API를 실행합니다.

```bash
./gradlew :api:run
./gradlew :app:listAppleSimulators
```

같은 타깃을 설치된 iPad 시뮬레이터 이름에 맞춰 Gradle로 빌드, 설치, 실행합니다.

```bash
./gradlew :app:runIpadSimulator -PipadSimulator="<your-ipad-simulator>"
```

시뮬레이터 실행 시 서버 주소를 바꾸려면:

```bash
./gradlew :app:runIpadSimulator \
  -PipadSimulator="<your-ipad-simulator>" \
  -Pmessaging.baseUrl=http://localhost:8080
```

Xcode를 사용할 경우 `app/iosApp/iosApp.xcodeproj`를 열고 설치된 iPad Simulator를 선택해 실행하면 됩니다.

Wasm 출력물이 갱신되면, 실행 중인 API 서버가 새 정적 파일을 서빙합니다.

## 테스트 및 검증

주요 검증 명령:

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

메모:

- `:api:test`는 Testcontainers를 사용하므로 Docker가 필요합니다.
- iOS 시뮬레이터 이름과 OS 버전은 로컬 Xcode에 설치된 runtime에 맞게 바꿔야 합니다.
- `:app:androidApp:assembleDebug`는 에뮬레이터 없이 Android 모듈 빌드만 검증할 때 사용할 수 있습니다.
- `:app:androidApp:installDebug`는 실행 가능한 에뮬레이터나 연결된 기기가 필요하지만, 기본 base URL은 Android 에뮬레이터 기준입니다.

## 자주 발생하는 로컬 문제

- `Docker is not running`
  - Docker Desktop 또는 Docker daemon을 먼저 실행합니다.
- `Port 5432 already in use`
  - 충돌 중인 로컬 DB를 중지하거나 포트를 변경합니다.
- `Android build fails`
  - `ANDROID_HOME` 또는 `local.properties`에 올바른 SDK 경로를 지정합니다.
- `adb` 또는 `emulator` 명령을 찾을 수 없음
  - Android SDK command-line tools를 설치하고 SDK의 `platform-tools`, `emulator` 디렉터리를 `PATH`에 추가합니다.
- `No iOS simulator matches the destination`
  - `xcodebuild -showdestinations` 또는 `xcrun simctl list devices available`로 설치된 시뮬레이터 이름을 확인하고 그 값으로 바꿉니다.
- `simctl install`에서 `iosApp.app`를 찾지 못함
  - `xcodebuild` 빌드를 다시 수행한 뒤 `~/Library/Developer/Xcode/DerivedData`에서 `.app` 경로를 다시 확인합니다.
- `API tests fail on container startup`
  - Docker 접근 가능 여부와 컨테이너 실행 권한을 확인합니다.
