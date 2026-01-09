package io.github.nek0cha.discordembedsenderplugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.configuration.file.FileConfiguration;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;

public class DiscordEmbedSenderPlugin extends JavaPlugin {
    private io.github.nek0cha.discordembedsenderplugin.DiscordBotManager discord;
    private BukkitTask task;
    private Instant startTime;

    @Override
    public void onEnable() {
        // プラグイン初期化
        saveDefaultConfig();
        saveCustomConfig("functions/example.json");
        FileConfiguration cfg = getConfig();

        String token = cfg.getString("token", "");
        long channelId = cfg.getLong("channel", 0L);
        boolean periodicEnabled = cfg.getBoolean("periodic.enabled", false);
        int interval = cfg.getInt("periodic.intervalSeconds", 60);

        if (token == null || token.isBlank()) {
            getLogger().severe("Discord bot token が config.yml にありません。plugins/DiscordBotPlugin/config.yml を確認してください。");
            return;
        }
        if (channelId == 0L) {
            getLogger().severe("channel (ID) が config.yml にありません。送信先チャンネル ID を設定してください。");
            return;
        }

        // JDA 初期化（非同期で起動）
        discord = new io.github.nek0cha.discordembedsenderplugin.DiscordBotManager(getLogger());
        boolean started = discord.start(token);
        if (!started) {
            getLogger().severe("Discord 接続に失敗しました。token を確認してください。");
            return;
        }

        // 開始時間を記録
        startTime = Instant.now();

        // コマンドの登録
        SendDiscordEmbedCommand embedCommand = new SendDiscordEmbedCommand(this, discord);
        getCommand("senddiscordembed").setExecutor(embedCommand);
        getCommand("senddiscordembed").setTabCompleter(embedCommand);

        // 定期送信タスク（periodic.enabled が true の場合のみ開始）
        if (periodicEnabled) {
            task = Bukkit.getScheduler().runTaskTimerAsynchronously(
                    this,
                    () -> {
                        try {
                            sendServerInfo(channelId);
                        } catch (Exception e) {
                            getLogger().severe("サーバー情報送信で例外: " + e.getMessage());
                            e.printStackTrace();
                        }
                    },
                    20L * 5,                 // 5秒後に最初の実行（サーバー起動直後の一気読みを避ける）
                    20L * interval           // interval 秒ごと
            );
            getLogger().info("DiscordBotPlugin 有効化。定期送信: 有効（送信間隔: " + interval + " 秒）");
        } else {
            getLogger().info("DiscordBotPlugin 有効化。定期送信: 無効");
        }
    }

    @Override
    public void onDisable() {
        if (task != null) task.cancel();
        if (discord != null) discord.stop();
        getLogger().info("DiscordBotPlugin 無効化");
    }

    private void sendServerInfo(long channelId) {
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        String players = Bukkit.getOnlinePlayers().stream()
                .map(p -> p.getName())
                .collect(Collectors.joining(", "));
        Duration uptime = Duration.between(startTime, Instant.now());
        String uptimeStr = formatDuration(uptime);

        StringBuilder sb = new StringBuilder();
        sb.append("サーバー情報\n");
        sb.append("オンライン: ").append(online).append("/").append(max).append("\n");
        sb.append("プレイヤー: ").append(players.isEmpty() ? "なし" : players).append("\n");
        sb.append("稼働時間: ").append(uptimeStr).append("\n");
        sb.append("バージョン: ").append(Bukkit.getVersion());

        // Discord へ送信（DiscordBotManager が非同期でも安全に送る）
        discord.sendMessageToChannel(channelId, sb.toString());
    }

    public void saveCustomConfig(String path) {
        java.io.File configFile = new java.io.File(getDataFolder(), path);
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            saveResource(path, false);
        }
    }

    private String formatDuration(Duration d) {
        long days = d.toDays();
        long hours = d.toHours() % 24;
        long minutes = d.toMinutes() % 60;
        long seconds = d.getSeconds() % 60;
        return String.format("%dd %02dh %02dm %02ds", days, hours, minutes, seconds);
    }
}