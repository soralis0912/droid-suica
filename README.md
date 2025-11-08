# Droid Suica

Droid SuicaはSuica等のFeliCaベースの交通系ICカードから情報を読み取るAndroidアプリです。

## 機能

- **NFC読み取り**: Android端末のNFC機能を使用してFeliCaカードを読み取り
- **リモート認証**: 暗号領域の読み出しにリモート認証サーバーを利用  
- **カード情報表示**: 残高、利用履歴、カード詳細の表示
- **駅名解決**: station_codes.csvに基づく会社名・路線名・駅名の解決
- **データ保存**: カード情報をJSONファイルとして保存
- **設定管理**: 認証サーバーURLなどの設定をアプリ内で変更可能

## 必要環境

- Android 8.0 (API level 26) 以上
- NFC機能搭載端末
- インターネット接続（リモート認証サーバーとの通信のため）

## 技術スタック

- **Kotlin**: プログラミング言語
- **Android Jetpack**: UI・アーキテクチャコンポーネント
  - ViewModel & LiveData
  - Navigation Component
  - Preference
- **Material Design 3**: UIフレームワーク
- **OkHttp 4.12**: HTTP通信
- **Kotlinx Serialization**: JSON解析
- **Coroutines**: 非同期処理

## プロジェクト構造

```
app/src/main/java/com/example/droidsuica/
├── MainActivity.kt                  # メインアクティビティ
├── SettingsActivity.kt              # 設定画面アクティビティ
├── model/
│   └── CardData.kt                 # カードデータモデル
├── service/
│   ├── NFCService.kt               # NFC通信サービス
│   ├── AuthClient.kt               # リモート認証クライアント
│   ├── StationLookupService.kt     # 駅コードルックアップ
│   ├── CardParserService.kt        # カードデータ解析
│   └── SettingsManager.kt          # 設定管理サービス
└── ui/
    └── MainViewModel.kt            # メインViewModel
```

## セットアップ

1. リポジトリをクローン
   ```bash
   git clone https://github.com/your-username/droid-suica.git
   cd droid-suica
   ```

2. Android Studioでプロジェクトを開く

3. Gradleの同期を実行

4. Android端末（NFC搭載）でアプリをビルド・実行
   - エミュレータではNFC機能が使えないため、実機が必要です

## ビルド

### デバッグビルド
```bash
./gradlew assembleDebug
```

### リリースビルド
```bash
export STORE_PASSWORD=your_keystore_password
export KEY_ALIAS=your_key_alias
export KEY_PASSWORD=your_key_password
./gradlew assembleRelease
```

## 設定

### 認証サーバーURL
- デフォルト: `https://felica-auth.nyaa.ws`
- アプリの設定画面から変更可能

### 注意事項
- 認証サーバーには個人情報やカード識別子などの機密データが送信される可能性があります
- 信頼できる環境でのみ接続してください

## ライセンス

MIT License - 詳細は[LICENSE](LICENSE)を参照してください。

Copyright (c) 2025 droid-suica contributors

## 注意

このアプリは実験的なものです。実際のFeliCaカードとの通信には、適切な権限と認証が必要です。カードの暗号化された領域へのアクセスは、正当な目的でのみ使用してください。
