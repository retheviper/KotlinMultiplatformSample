# KotlinMultiplatformSample

[English](./README.md) | [한국어](./README.ko.md) | [日本語](./README.ja.md)

Kotlin Multiplatform ベースのワークスペース型チャットアプリケーションのリポジトリです。

![コンセプト図](./concept.svg)

このリポジトリには以下が含まれます。

- Ktor API サーバー
- Compose Multiplatform の共有 UI
- API が直接配信する Compose Wasm Web クライアント
- Compose Desktop シェル
- `shared` Compose UI を使う Android シェル
- macOS SwiftUI シェル
- iOS / iPadOS SwiftUI シェル

現在の実装範囲は、Slack ライクな workspace、channel、thread、mention、notification、reaction、link preview を含む縦切りの機能セットです。
notification 更新は Web、Compose Desktop、macOS SwiftUI シェルで push 方式で動作します。

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
  `shared` Compose UI を使う Android シェル

app/desktopApp/
  Compose Desktop シェル

app/macosApp/
  macOS SwiftUI シェル

app/iosApp/
  iOS / iPadOS SwiftUI シェル

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
- notification 更新用 Server-Sent Events
- Testcontainers による API 統合テスト

## プラットフォーム状況

- Web: 実装済み、API から直接配信
- Desktop: Compose Desktop で実装済み
- macOS native: `app/macosApp` の SwiftUI シェルで実装済み
- Android: `app/androidApp` の薄いシェルで実装済み。UI/state/resource/networking は `shared` を再利用
- iOS: `app/iosApp` の SwiftUI シェルで実装済み、contract と networking は `shared` を利用
- iPadOS: 同じ `app/iosApp` ターゲットで実装済み、iPad 向けの適応レイアウトと simulator 実行に対応

## Apple シェルメモ

- iOS と macOS は Swift の model/helper と一部の SwiftUI component を共有する構成に整理されています。
- iOS SwiftUI シェルは root screen、workspace shell、channel/thread screen、message component、overlay component に責務を分割しています。
- Apple 側の実装が SDK バージョンや runtime 挙動に依存する場合は、実装前に SwiftUI、WebKit、UserNotifications、Ktor、Kotlin Multiplatform の最新の公式ドキュメントを確認することを基本方針にします。

## 前提条件

必須:

- JDK 17 以上
- `docker compose` が使える Docker 環境

任意:

- `:app:androidApp` 用の Android SDK
- `app/macosApp` 用の Xcode と Swift 6 toolchain
- `app/iosApp` 用の Xcode と iOS Simulator runtime

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

### 3. Android を実行

先に API を起動し、Android emulator を準備してからアプリをインストールします。

```bash
./gradlew :api:run
emulator -list-avds
emulator @<your-avd-name>
adb devices
./gradlew :app:androidApp:installDebug
adb shell am start -n com.retheviper.chat.android/com.retheviper.chat.android.MainActivity
```

Android shell は emulator から host machine 上のサーバーへ到達できるよう、既定で `http://10.0.2.2:8080` に接続します。
platform code は薄く保ち、プロダクト UI/state/resource/networking は `:shared` に委譲する構成です。
Android Studio を使う場合は Device Manager から emulator を先に起動してから `installDebug` を実行しても構いません。
現在のデフォルトネットワーク設定は Android emulator 向けです。`10.0.2.2` は emulator 専用のため、物理 Android 端末はそのままでは接続先になりません。

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

### 4. アプリへアクセス

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

## iOS Simulator 実行

先に API を起動します。

```bash
./gradlew :api:run
xcodebuild -project app/iosApp/iosApp.xcodeproj -scheme iosApp -showdestinations
xcrun simctl list devices available
```

次に、上のコマンドで確認したインストール済み iPhone simulator 名を使って build します。

```bash
xcodebuild \
  -project app/iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=<your-iphone-simulator>' \
  build
```

生成された `.app` のパスを確認し、simulator を boot してアプリを install し、launch します。

```bash
find ~/Library/Developer/Xcode/DerivedData -path '*/Build/Products/Debug-iphonesimulator/iosApp.app' | head -n 1
xcrun simctl boot "<your-iphone-simulator>"
xcrun simctl bootstatus "<your-iphone-simulator>" -b
xcrun simctl install booted "$(find ~/Library/Developer/Xcode/DerivedData -path '*/Build/Products/Debug-iphonesimulator/iosApp.app' | head -n 1)"
xcrun simctl launch booted orgIdentifier.iosApp
```

iOS シェルのデフォルト接続先:

```bash
MESSAGING_BASE_URL=http://localhost:8080
```

iOS shell はデフォルトで `http://localhost:8080` に接続します。iOS Simulator は Mac host のネットワークを共有するため、この既定値で動作します。
CLI で simulator 起動時に接続先を上書きしたい場合は、次のように環境変数を注入できます。

```bash
SIMCTL_CHILD_MESSAGING_BASE_URL=http://localhost:8080 \
xcrun simctl launch booted orgIdentifier.iosApp
```

Xcode を使う場合は `app/iosApp/iosApp.xcodeproj` を開き、インストール済みの iPhone Simulator を選択し、必要なら Run scheme に `MESSAGING_BASE_URL` を設定して実行してください。

## iPadOS Simulator 実行

先に API を起動します。

```bash
./gradlew :api:run
xcodebuild -project app/iosApp/iosApp.xcodeproj -scheme iosApp -showdestinations
xcrun simctl list devices available
```

同じターゲットを、インストール済み iPad simulator 名に合わせて build します。

```bash
xcodebuild \
  -project app/iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=<your-ipad-simulator>' \
  build
```

iPad simulator を boot し、アプリを install して launch します。

```bash
xcrun simctl boot "<your-ipad-simulator>"
xcrun simctl bootstatus "<your-ipad-simulator>" -b
xcrun simctl install booted "$(find ~/Library/Developer/Xcode/DerivedData -path '*/Build/Products/Debug-iphonesimulator/iosApp.app' | head -n 1)"
xcrun simctl launch booted orgIdentifier.iosApp
```

Xcode を使う場合は `app/iosApp/iosApp.xcodeproj` を開き、インストール済みの iPad Simulator を選択して実行してください。

Wasm 出力が更新されると、起動中の API サーバーが新しい静的ファイルを配信します。

## テストと検証

主な検証コマンド:

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

メモ:

- `:api:test` は Testcontainers を使うため Docker が必要です。
- iOS simulator の名前と OS バージョンは、ローカルの Xcode に入っている runtime に合わせて変更してください。
- `:app:androidApp:assembleDebug` は emulator なしで Android モジュールのビルド確認を行うためのコマンドです。
- `:app:androidApp:installDebug` には起動済み emulator または接続済み端末が必要ですが、デフォルトの base URL は Android emulator 向けです。

## よくあるローカル問題

- `Docker is not running`
  - Docker Desktop または Docker daemon を先に起動してください。
- `Port 5432 already in use`
  - 競合しているローカル DB を停止するかポートを変更してください。
- `Android build fails`
  - `ANDROID_HOME` または `local.properties` に正しい SDK パスを設定してください。
- `adb` または `emulator` コマンドが見つからない
  - Android SDK command-line tools を入れ、SDK の `platform-tools` と `emulator` ディレクトリを `PATH` に追加してください。
- `No iOS simulator matches the destination`
  - `xcodebuild -showdestinations` または `xcrun simctl list devices available` で利用可能な simulator 名を確認し、その名前に置き換えてください。
- `simctl install` で `iosApp.app` が見つからない
  - `xcodebuild` をやり直し、`~/Library/Developer/Xcode/DerivedData` から `.app` のパスを再確認してください。
- `API tests fail on container startup`
  - Docker が利用可能か、コンテナ実行権限があるか確認してください。
