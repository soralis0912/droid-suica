# Droid Suica

Droid SuicaはSuica等のFeliCaベースの交通系ICカードから情報を読み取るAndroidアプリです。

## 機能

- **NFC読み取り**: Android端末のNFC機能を使用してFeliCaカードを読み取り
- **リモート認証**: 暗号領域の読み出しにリモート認証サーバーを利用  
- **カード情報表示**: 残高、利用履歴、カード詳細の表示
- **駅名解決**: station_codes.csvに基づく会社名・路線名・駅名の解決
- **データ保存**: カード情報をJSONファイルとして保存

## 必要環境

- Android 8.0 (API level 26) 以上
- NFC機能搭載端末
- インターネット接続（リモート認証サーバーとの通信のため）

## 技術スタック

- **Kotlin**: プログラミング言語
- **Android Jetpack**: UI・アーキテクチャコンポーネント
- **Material Design 3**: UIフレームワーク
- **OkHttp**: HTTP通信
- **Coroutines**: 非同期処理

## プロジェクト構造

```
app/src/main/java/com/example/droidsuica/
├── MainActivity.kt                  # メインアクティビティ
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

1. Android Studioでプロジェクトを開く
2. Gradleの同期を実行
3. Android端末またはエミュレータでアプリを実行

## 設定

### 認証サーバーURL
- デフォルト: `https://felica-auth.nyaa.ws`
- アプリの設定画面から変更可能

### 注意事項
- 認証サーバーには個人情報やカード識別子などの機密データが送信される可能性があります
- 信頼できる環境でのみ接続してください

## 参考プロジェクト

このプロジェクトは以下のプロジェクトを参考にして作成されています：

- [suica-viewer](../suica-viewer/) - Python版Suica Viewer
- [suica-viewer-android](../suica-viewer-android/) - Flutter版Suica Viewer  
- [aa](../aa/) - WebView版Suica Viewer

## ライセンス

MIT License

## 注意

このアプリは実験的なものです。実際のFeliCaカードとの通信には、適切な権限と認証が必要です。カードの暗号化された領域へのアクセスは、正当な目的でのみ使用してください。
