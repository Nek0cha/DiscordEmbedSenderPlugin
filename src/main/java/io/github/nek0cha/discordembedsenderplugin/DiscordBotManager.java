package io.github.nek0cha.discordembedsenderplugin;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class DiscordBotManager {
    private JDA jda;
    private final Logger logger;
    private volatile boolean ready = false;

    public DiscordBotManager(Logger logger) {
        this.logger = logger;
    }

    public boolean start(String token) {
        try {
            EnumSet<GatewayIntent> intents = EnumSet.of(GatewayIntent.GUILD_MESSAGES);
            jda = JDABuilder.createDefault(token, intents)
                    .setAutoReconnect(true)
                    .build();

            // awaitReady を別スレッドで実行して ready を立てる（サーバーメインスレッドをブロックしない）
            CompletableFuture.runAsync(() -> {
                try {
                    jda.awaitReady();
                    ready = true;
                    logger.info("JDA is ready.");
                } catch (InterruptedException e) {
                    logger.warning("JDA awaitReady interrupted: " + e.getMessage());
                } catch (Exception e) {
                    logger.warning("JDA failed to be ready: " + e.getMessage());
                }
            });

            logger.info("JDA start requested (非同期).");
            return true;
        } catch (Exception e) {
            logger.severe("JDA start failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void stop() {
        if (jda != null) {
            jda.shutdown();
            logger.info("JDA shutdown");
        }
        ready = false;
    }

    public boolean isReady() {
        return ready;
    }

    public CompletableFuture<Boolean> sendEmbed(long channelId, MessageEmbed embed) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        if (!ready || jda == null) {
            logger.warning("JDA not ready - cannot send embed now.");
            result.complete(false);
            return result;
        }
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            logger.warning("指定したチャンネルID が見つかりません: " + channelId);
            result.complete(false);
            return result;
        }
        channel.sendMessageEmbeds(embed).queue(
                success -> {
                    logger.fine("Embed sent");
                    result.complete(true);
                },
                failure -> {
                    logger.warning("Embed send failed: " + failure.getMessage());
                    result.complete(false);
                }
        );
        return result;
    }

    public void sendMessageToChannel(long channelId, String message) {
        if (!ready || jda == null) {
            logger.warning("JDA not ready - cannot send message now.");
            return;
        }
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            logger.warning("指定したチャンネルID が見つかりません: " + channelId);
            return;
        }
        channel.sendMessage(message).queue(
                success -> logger.fine("Message sent"),
                failure -> logger.warning("Message send failed: " + failure.getMessage())
        );
    }
}