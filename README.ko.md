# KotlinMultiplatformSample

[English](./README.md) | [한국어](./README.ko.md) | [日本語](./README.ja.md)

Kotlin Multiplatform 기반의 워크스페이스형 채팅 애플리케이션 저장소입니다.

![개념 다이어그램](./concept.svg)

이 저장소에는 다음이 포함됩니다.

- Ktor API 서버
- Compose Multiplatform 공용 UI
- API가 직접 서빙하는 Compose Wasm 웹 클라이언트
- Android 및 Desktop 셸 앱

현재 구현 범위는 Slack과 유사한 워크스페이스, 채널, 스레드, 멘션, 알림, 리액션, 링크 미리보기까지 포함한 수직 슬라이스입니다.

## 대상 독자

이 문서는 다음 사용자를 기준으로 작성되었습니다.

- 제품을 확장하는 개발자
- 로컬에서 직접 실행하는 테스터
- 구조와 실행 흐름을 빠르게 파악해야 하는 리뷰어

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
  Android 실행 셸

app/desktopApp/
  Desktop 실행 셸

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
- Testcontainers 기반 API 통합 테스트

## 사전 준비

필수:

- JDK 17 이상
- `docker compose` 사용 가능한 Docker 환경

선택:

- `:app:androidApp` 실행을 위한 Android SDK

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

Desktop 클라이언트는 기본적으로 `http://localhost:8080`에 연결합니다.

다른 서버를 쓰려면:

```bash
./gradlew :app:desktopApp:run -Dmessaging.baseUrl=http://localhost:8080
```

macOS에서는 현재 Compose 셸을 사용하지만, 이후 SwiftUI 기반 네이티브 셸과 공존할 수 있도록 런처 선택 경로를 열어 두었습니다.

실행 시 셸 선택:

```bash
./gradlew :app:desktopApp:run -Dchat.desktop.shell=compose
./gradlew :app:desktopApp:run -Dchat.desktop.shell=chooser
./gradlew :app:desktopApp:run -Dchat.desktop.shell=mac-native
```

현재 실제 구현은 `compose`이고, `mac-native`는 향후 SwiftUI 셸용 예약 경로이며 지금은 Compose로 fallback됩니다.

Wasm 출력물이 갱신되면, 실행 중인 API 서버가 새 정적 파일을 서빙합니다.

## 테스트 및 검증

주요 검증 명령:

```bash
./gradlew :shared:jvmTest
./gradlew :api:test
./gradlew :app:desktopApp:compileKotlin
```

참고:

- `:api:test`는 Docker가 필요합니다.
- Android 빌드는 위 기본 검증 명령에 포함되어 있지 않습니다.

## 현재 기능 범위

구현 완료:

- workspace 생성 및 조회
- 기존 멤버 또는 새 멤버로 workspace 접속
- `#general` 채널 자동 생성
- channel 생성 및 전환
- WebSocket 메시지 전송
- thread reply
- mention 및 mention 알림
- notification center 및 toast 알림
- emoji reaction
- 메시지 내 클릭 가능한 링크
- OG 기반 링크 미리보기
- API가 서빙하는 Compose Wasm 프런트엔드
- 프런트엔드용 공용 이미지 및 폰트 리소스

## 자주 발생하는 로컬 문제

- `Docker is not running`
  - Docker Desktop 또는 Docker daemon을 먼저 실행합니다.
- `Port 5432 already in use`
  - 충돌 중인 로컬 DB를 중지하거나 포트를 변경합니다.
- `Android build fails`
  - `ANDROID_HOME` 또는 `local.properties`에 올바른 SDK 경로를 지정합니다.
- `API tests fail on container startup`
  - Docker 접근 가능 여부와 컨테이너 실행 권한을 확인합니다.

## 저장소 정책

`AGENT.md`와 `docs/` 같은 내부 작업 문서는 공개용 저장소 흐름에서 제외하며, 이 README 링크에도 포함하지 않습니다.
