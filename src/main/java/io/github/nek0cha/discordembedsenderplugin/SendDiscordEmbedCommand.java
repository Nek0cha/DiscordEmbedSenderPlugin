package io.github.nek0cha.discordembedsenderplugin;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;

public class SendDiscordEmbedCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final DiscordBotManager discord;
    private final Gson gson = new Gson();
    private final Logger logger;

    public SendDiscordEmbedCommand(JavaPlugin plugin, DiscordBotManager discord) {
        this.plugin = plugin;
        this.discord = discord;
        this.logger = plugin.getLogger();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 引数チェック
        if (args.length < 1) {
            sender.sendMessage("使用法: /senddiscordembed <jsonファイル名(拡張子無し)>");
            return true;
        }

        // 実行元チェック: プレイヤーなら OP のみ、コマンドブロックとコンソールは OK
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (!p.isOp() && !p.hasPermission("discordbot.sendembed")) {
                p.sendMessage("このコマンドを実行する権限がありません（OP 必須）。");
                return true;
            }
        } else if (sender instanceof BlockCommandSender) {
            // command block は許可
        } else if (sender instanceof ConsoleCommandSender) {
            // console（datapack の function 等）を許可
        } else {
            sender.sendMessage("この実行元からはコマンドを実行できません。");
            return true;
        }

        String name = args[0];
        File functionsDir = new File(plugin.getDataFolder(), "functions");
        if (!functionsDir.exists()) functionsDir.mkdirs();
        File file = new File(functionsDir, name + ".json");

        if (!file.exists()) {
            sender.sendMessage("指定された JSON ファイルが見つかりません: " + file.getPath());
            return true;
        }

        // JSON を読み込み Embed を作って送信
        try (FileReader fr = new FileReader(file)) {
            EmbedConfig cfg = gson.fromJson(fr, EmbedConfig.class);
            if (cfg == null) {
                sender.sendMessage("JSON のパースに失敗しました（空ファイル等）");
                return true;
            }
            if (cfg.channel == 0L) {
                sender.sendMessage("JSON に channel が指定されていません。");
                return true;
            }

            if (!discord.isReady()) {
                sender.sendMessage("Discord Bot がまだ準備完了していません。少し待ってから再実行してください。");
                return true;
            }

            EmbedBuilder eb = new EmbedBuilder();
            if (cfg.title != null) eb.setTitle(cfg.title);
            if (cfg.description != null) eb.setDescription(cfg.description);
            if (cfg.color != null) {
                try {
                    Color c = parseColor(cfg.color);
                    eb.setColor(c);
                } catch (Exception e) {
                    logger.warning("色のパースに失敗: " + e.getMessage());
                }
            }
            if (cfg.timestamp) eb.setTimestamp(Instant.now());
            if (cfg.footer != null && cfg.footer.text != null) eb.setFooter(cfg.footer.text, cfg.footer.icon_url);
            if (cfg.image != null && cfg.image.url != null) eb.setImage(cfg.image.url);
            if (cfg.thumbnail != null && cfg.thumbnail.url != null) eb.setThumbnail(cfg.thumbnail.url);
            if (cfg.fields != null) {
                for (EmbedConfig.Field f : cfg.fields) {
                    eb.addField(f.name == null ? "" : f.name, f.value == null ? "" : f.value, f.inline);
                }
            }

            // 送信
            discord.sendEmbed(cfg.channel, eb.build()).thenAccept(success -> {
                if (success) {
                    sender.sendMessage("§a正常に送信されました");
                } else {
                    sender.sendMessage("§c送信に失敗しました。チャンネルIDや権限を確認してください");
                }
            });
        } catch (JsonSyntaxException jse) {
            sender.sendMessage("JSON の構文エラー: " + jse.getMessage());
            logger.warning("JSON syntax error: " + jse.getMessage());
        } catch (Exception e) {
            sender.sendMessage("エラーが発生しました: " + e.getMessage());
            logger.warning("Error sending embed: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private Color parseColor(String s) {
        // "#RRGGBB" または "RRGGBB" を受け付ける。
        if (s.startsWith("#")) s = s.substring(1);
        try {
            int rgb = Integer.parseInt(s, 16);
            return new Color(rgb);
        } catch (NumberFormatException e) {
            // try parse int
            int val = Integer.parseInt(s);
            return new Color(val);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // 引数が 1 個目のとき functions フォルダのファイル名を補完
        if (args.length == 1) {
            File functionsDir = new File(plugin.getDataFolder(), "functions");
            if (!functionsDir.exists()) return List.of();
            String prefix = args[0].toLowerCase();
            return List.of(functionsDir.list((dir, name) -> {
                if (!name.toLowerCase().endsWith(".json")) return false;
                String base = name.substring(0, name.length() - 5);
                return base.toLowerCase().startsWith(prefix);
            })).stream().map(s -> s.substring(0, s.length() - 5)).toList();
        }
        return List.of();
    }
}