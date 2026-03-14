# KotlinMultiplatformSample

[English](./README.md) | [한국어](./README.ko.md) | [日本語](./README.ja.md)

Kotlin Multiplatform ベースのワークスペース型チャットアプリケーションのリポジトリです。

![コンセプト図](./concept.svg)

このリポジトリには以下が含まれます。

- Ktor API サーバー
- Compose Multiplatform の共有 UI
- API が直接配信する Compose Wasm Web クライアント
- Compose Desktop シェル
- macOS SwiftUI シェル
- 将来の Android / iOS 向けの placeholder モバイルシェル

現在の実装範囲は、Slack ライクな workspace、channel、thread、mention、notification、reaction、link preview を含む縦切りの機能セットです。

## プロジェクト構成

```text
api/
  Ktor サーバー
  messaging domain/application/infrastructure/presentation レイヤー
  Flyway migration
  OpenAPI ドキュメント

shared/
  共有契約とクライアントモデル
  共有 Ktor client
  共有 Compose UI/state/resources
  Compose Wasm エントリポイント

app/androidApp/
  Android placeholder シェル

app/desktopApp/
  Compose Desktop シェル

app/macosApp/
  macOS SwiftUI シェル

app/iosApp/
  iOS placeholder シェル

compose.yaml
  ローカル PostgreSQL 定義
```

## 技術スタック

- Kotlin Multiplatform
- Ktor 3
- Compose Multiplatform
- PostgreSQL
- R2DBC
- Exposed
- Flyway
- WebSocket ベースのチャット通信
- Testcontainers による API 統合テスト

## プラットフォーム状況

- Web: 実装済み、API から直接配信
- Desktop: Compose Desktop で実装済み
- macOS native: `app/macosApp` の SwiftUI シェルで実装済み
- Android: モジュールはあるが、プロダクト UI はまだ未実装
- iOS: モジュールはあるが、プロダクト UI はまだ未実装

## 前提条件

必須:

- JDK 17 以上
- `docker compose` が使える Docker 環境

任意:

- `:app:androidApp` 用の Android SDK
- `app/macosApp` 用の Xcode と Swift 6 toolchain

このリポジトリで確認済みの wrapper バージョン:

- Gradle 9.4.0

## ローカル実行

### 1. PostgreSQL を起動

```bash
docker compose up -d db
```

停止:

```bash
docker compose down
```

ボリュームまで初期化:

```bash
docker compose down -v
docker compose up -d db
```

### 2. API サーバーを起動

```bash
./gradlew :api:run
```

デフォルトでは `compose.yaml` で起動したローカル PostgreSQL を利用します。

利用できる環境変数:

```bash
CHAT_JDBC_URL
CHAT_R2DBC_URL
CHAT_DB_USER
CHAT_DB_PASSWORD
```

例:

```bash
export CHAT_JDBC_URL=jdbc:postgresql://localhost:5432/messaging_app
export CHAT_R2DBC_URL=r2dbc:postgresql://localhost:5432/messaging_app
export CHAT_DB_USER=postgres
export CHAT_DB_PASSWORD=postgres
./gradlew :api:run
```

起動時に行われる処理:

- Flyway migration の適用
- `shared` の Web フロントエンド bundle 生成
- `/` で Web アプリを配信

### 3. アプリへアクセス

デフォルトのエンドポイント:

- App: [http://localhost:8080/](http://localhost:8080/)
- Swagger UI: [http://localhost:8080/docs](http://localhost:8080/docs)
- OpenAPI: [http://localhost:8080/openapi.yaml](http://localhost:8080/openapi.yaml)
- Health: [http://localhost:8080/health](http://localhost:8080/health)

## フロントエンド開発メモ

Web フロントエンドは API サーバーが直接配信するため、別の frontend dev server は不要です。

Wasm の反復速度を上げたい場合は、次のように 2 つのターミナルを使うのが簡単です。

```bash
./gradlew :shared:wasmJsBrowserDevelopmentExecutableDistribution --continuous
./gradlew :api:run
```

## Desktop 実行

先に API を起動し、その後 Desktop シェルを起動します。

```bash
./gradlew :api:run
./gradlew :app:desktopApp:run
```

Compose Desktop クライアントはデフォルトで `http://localhost:8080` に接続します。
macOS ではシェルを明示しない場合、デフォルトで chooser が開きます。
Compose Desktop シェルはメインウィンドウを閉じると終了します。

別のサーバーを使う場合:

```bash
./gradlew -Dmessaging.baseUrl=http://localhost:8080 :app:desktopApp:run
```

macOS では Compose Desktop シェルと SwiftUI native シェルの両方を実行できます。

起動時のシェル選択:

```bash
./gradlew -Dchat.desktop.shell=compose :app:desktopApp:run
./gradlew -Dchat.desktop.shell=chooser :app:desktopApp:run
./gradlew -Dchat.desktop.shell=mac-native :app:desktopApp:run
```

`compose` は Compose Desktop シェルを起動します。macOS では `mac-native` が `app/macosApp` の SwiftUI シェルを起動し、`chooser` はシェル選択ウィンドウを明示的に開きます。

JVM system property の代わりに環境変数でも指定できます。

```bash
CHAT_DESKTOP_SHELL=mac-native ./gradlew :app:desktopApp:run
MESSAGING_BASE_URL=http://localhost:8080 ./gradlew :app:desktopApp:run
```

SwiftUI シェルだけを直接起動することもできます。

```bash
swift run --package-path app/macosApp
```

Android と iOS のモジュールは、現時点では実際のプロダクトクライアントとしては未接続で、今後の native 実装向けの土台のみがあります。

Wasm 出力が更新されると、起動中の API サーバーが新しい静的ファイルを配信します。

## テストと検証

主な検証コマンド:

```bash
./gradlew :shared:jvmTest
./gradlew :api:test
./gradlew :app:desktopApp:compileKotlin
swift test --package-path app/macosApp
```

注意:

- `:api:test` には Docker が必要です
- Android と iOS のビルドは上記の基本検証コマンドには含まれていません

## よくあるローカル問題

- `Docker is not running`
  - Docker Desktop または Docker daemon を先に起動してください。
- `Port 5432 already in use`
  - 競合しているローカル DB を停止するかポートを変更してください。
- `Android build fails`
  - `ANDROID_HOME` または `local.properties` に正しい SDK パスを設定してください。
- `API tests fail on container startup`
  - Docker が利用可能か、コンテナ実行権限があるか確認してください。
