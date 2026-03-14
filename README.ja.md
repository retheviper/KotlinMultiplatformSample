# KotlinMultiplatformSample

[English](./README.md) | [한국어](./README.ko.md) | [日本語](./README.ja.md)

Kotlin Multiplatform ベースのワークスペース型チャットアプリケーションのリポジトリです。

![コンセプト図](./concept.svg)

このリポジトリには以下が含まれます。

- Ktor API サーバー
- Compose Multiplatform の共有 UI
- API が直接配信する Compose Wasm Web クライアント
- Android / Desktop のシェルアプリ

現在の実装範囲は、Slack ライクな workspace、channel、thread、mention、notification、reaction、link preview を含む縦切りの機能セットです。

## 想定読者

この README は以下の利用者を想定しています。

- プロダクトを拡張する開発者
- ローカルで動作確認するテスター
- 構成と起動手順を短時間で把握したいレビュアー

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
  Android ランチャーシェル

app/desktopApp/
  Desktop ランチャーシェル

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

## 前提条件

必須:

- JDK 17 以上
- `docker compose` が使える Docker 環境

任意:

- `:app:androidApp` 用の Android SDK

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

Desktop クライアントはデフォルトで `http://localhost:8080` に接続します。

別のサーバーを使う場合:

```bash
./gradlew :app:desktopApp:run -Dmessaging.baseUrl=http://localhost:8080
```

macOS では現時点では Compose シェルを使いますが、将来 SwiftUI ベースの native シェルを共存させられるようにランチャー選択経路を用意しています。

起動時のシェル選択:

```bash
./gradlew :app:desktopApp:run -Dchat.desktop.shell=compose
./gradlew :app:desktopApp:run -Dchat.desktop.shell=chooser
./gradlew :app:desktopApp:run -Dchat.desktop.shell=mac-native
```

現時点で実装されているのは `compose` で、`mac-native` は将来の SwiftUI シェル用に予約された経路であり、今は Compose に fallback します。

Wasm 出力が更新されると、起動中の API サーバーが新しい静的ファイルを配信します。

## テストと検証

主な検証コマンド:

```bash
./gradlew :shared:jvmTest
./gradlew :api:test
./gradlew :app:desktopApp:compileKotlin
```

注意:

- `:api:test` には Docker が必要です
- Android ビルドは上記の基本検証コマンドには含まれていません

## 現在の機能範囲

実装済み:

- workspace の作成と一覧取得
- 既存メンバーまたは新規メンバーとして workspace に入るフロー
- `#general` channel の自動作成
- channel の作成と切り替え
- WebSocket メッセージ送信
- thread reply
- mention と mention notification
- notification center と toast notification
- emoji reaction
- メッセージ内のクリック可能なリンク
- OG ベースの link preview
- API から配信される Compose Wasm フロントエンド
- フロントエンド向けの共有画像・フォントリソース

## よくあるローカル問題

- `Docker is not running`
  - Docker Desktop または Docker daemon を先に起動してください。
- `Port 5432 already in use`
  - 競合しているローカル DB を停止するかポートを変更してください。
- `Android build fails`
  - `ANDROID_HOME` または `local.properties` に正しい SDK パスを設定してください。
- `API tests fail on container startup`
  - Docker が利用可能か、コンテナ実行権限があるか確認してください。

## リポジトリ運用方針

`AGENT.md` や `docs/` のような内部向け作業文書は公開リポジトリのフローから外しており、この README のリンクにも含めていません。
