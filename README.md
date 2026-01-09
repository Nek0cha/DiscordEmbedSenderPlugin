# DiscordEmbedSenderPlugin — 使い方

このプラグインは、Minecraft（Paper 1.21.11）サーバーから Discord に「埋め込みメッセージ（Embed）」を送信するためのものです。  
送信内容はサーバー側の JSON ファイル（plugins/DiscordEmbedSenderPlugin/functions/*.json）で作成・管理でき、サーバー内のコマンド、コマンドブロック、またはデータパックの function から送信できます。

前提：
- Paper 1.21.11 を使ったサーバー
- Java 21
- Discord の Bot を作成できる権限（Discord サーバー管理者）

目次
- 準備（Discord 側）
- プラグインの導入（サーバー側）
- 設定ファイル（config.yml）
- functions フォルダの JSON（送信テンプレート）作成例
- 送信方法（プレイヤー、コマンドブロック、データパック）
- よくあるトラブルと対処
- セキュリティ注意事項
- 補足（ビルド方法、ログの確認）

---

準備（Discord 側）
1. Discord Developer Portal を開く。（ https://discord.com/developers/applications ）
2. 「New Application」でアプリを作成 → 左メニューの「Bot」から Bot を作成。
3. Bot のトークンをコピー（後で server の config.yml に貼ります）。トークンは絶対に他人に見せないでください。
4. Bot を自分の Discord サーバーに招待：
    - 「OAuth2 → URL Generator」を開く
    - scopes に `bot` をチェック
    - Bot Permissions に最低限 `Send Messages`と`Embed Links`を付ける。
    - 生成された URL で Bot を招待

プラグインの導入（サーバー側）
1. プラグインの jar（例：discordembedsenderplugin-x.x.jar）を Paper サーバーの `plugins/` フォルダに入れます。
2. サーバーを起動（または再起動）します。初回起動でプラグインのフォルダ `plugins/DiscordEmbedSenderPlugin` と `config.yml` が自動生成されます。

config.yml（設定ファイル）
- 生成された `plugins/DiscordEmbedSenderPlugin/config.yml` をテキストエディタで開きます。
- 必須項目は `token`（Bot トークン）と `channel`（送信先チャンネルID）です。
- `periodic:enabled`はデフォルトで`false`ですが、サーバーの情報をDiscordに定期送信する場合は`true`にします。

例（token は実際のトークンに置き換えてください）:
```yaml
token: "ここにBotのトークンを入れてください（絶対に公開しないこと）"
# 送信先チャンネル ID（数値）: Discord 上で対象チャンネルを右クリック → コピーID
channel: 123456789012345678

# 定期送信機能の設定
periodic:
  # 定期送信を有効にするか（デフォルト: false）
  enabled: false
  # 送信間隔（秒）
  intervalSeconds: 300
```

チャンネル ID の取得方法
1. Discord のユーザー設定 → 詳細設定 → 「開発者モード」を ON にする
2. 送信先チャンネルを右クリック → 「ID をコピー」

functions フォルダと JSON（送信テンプレート）
- `plugins/DiscordEmbedSenderPlugin/functions` フォルダを作成します（なければプラグインが自動で作るか、自分で作成）。
- ここに `<name>.json`（拡張子 .json、例: `status.json`）を置きます。
- JSON の中で送信先チャンネルや埋め込み（タイトル・本文・フィールド等）を定義します。

例：plugins/DiscordEmbedSenderPlugin/functions/example.json
```json
{
  "channel": 123456789012345678,
  "title": "サーバーステータス",
  "description": "現在のサーバー情報を手動送信します。",
  "color": "#00ff00",
  "timestamp": true,
  "fields": [
    { "name": "オンライン", "value": "3/50", "inline": true },
    { "name": "プレイヤー", "value": "player1, player2, player3", "inline": false }
  ],
  "footer": { "text": "Minecraft Server", "icon_url": null }
}
```
ポイント：
- `channel` は必ず数値（Discord のチャンネルID）にしてください。
- `color` は `#RRGGBB` の形式、または16進数のみでも可。
- `fields` は配列で複数の項目（名前、値）を指定できます。

送信方法（使い方）
- コマンド（プレイヤーから）
    - OP（サーバー管理者）または `discordbot.sendembed` 権限を持つプレイヤーが実行できます。
    - 使い方: /senddiscordembed <jsonファイル名（拡張子無し）>
    - 例: /senddiscordembed status

- コマンドブロックから
    - コマンドブロックに `senddiscordembed status` と入力して実行。

- データパック（function）から
    - データパックの .mcfunction ファイル内に以下を記述：
      /senddiscordembed status
    - /function を使ってその function を呼べば、サーバー（またはコマンドブロック）実行として送信できます。

補助機能
- タブ補完：/senddiscordembed コマンドは `functions` フォルダ内のファイル名補完に対応しています。
- 実行元の制限：
    - プレイヤー → OP または permission `discordbot.sendembed`
    - コマンドブロック → 許可
    - データパック（サーバー/コンソール実行） → 許可

よくあるトラブルと対処

- サーバーが起動しなくなった
    - config.yml の `token` が間違っていると起動時にエラーになります。正しいトークンを設定してください。

- 「Bot が準備完了ではありません」 と表示される
    - サーバー起動直後に JDA（Discord ライブラリ）が準備されるまで少し時間がかかります。数十秒待って再試行してください。

- Discord にメッセージが届かない
    - config.yml の `token` が正しいか確認（間違っているとエラーになります）。
    - JSON の `channel` が正しい ID か確認。
    - Bot にそのチャンネルへの送信権限（Send Messages / Embed Links）が与えられているか確認。
    - Bot が招待されているサーバー（ギルド）とチャンネルにアクセスできるか確認。

- JSON の読み込みエラー
    - JSON の文法が正しくない（カンマの付け忘れなど）とエラーになります。JSON 検証ツール（Web 上の JSON lint）で確認してください。

- コマンドが実行できない（プレイヤー）
    - 実行者が OP であるか、`discordbot.sendembed` 権限を持っているか確認してください。権限は plugin.yml の default が `op` に設定されています。

ログの確認
- サーバーのコンソール（または `logs/latest.log`）を確認すると、プラグインや Discord 関連のエラーが出ます。問題が解決しないときはログの該当部分をメモしてサポートを尋ねてください。

セキュリティ注意事項
- Bot トークンは秘密です。絶対に公開しないでください。公開してしまった場合は Discord Developer Portal でトークンを再生成してください。
- config.yml を Git リポジトリにコミットしないでください（.gitignore に追加推奨）。

補足：プラグインを手作業でビルドする（開発者向け）
- ソースからビルドする場合（開発者向け）:
    - Java と Maven が必要です。
    - ターミナルでプロジェクトルートから:
      mvn clean package
    - 生成された `target/*.jar` をサーバーの `plugins/` に入れて起動します。

質問・トラブル報告方法
問題が直らない場合、以下を教えてください：
1. `server.log` の該当エラーメッセージ（コピー）
2. `plugins/DiscordEmbedSenderPlugin/config.yml`（token は伏せてください）
3. 該当する functions の JSON（ファイル名と中身）
   これらがあれば、原因の調査と具体的な修正方法を提示できます。

---