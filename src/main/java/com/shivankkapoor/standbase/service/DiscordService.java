package com.shivankkapoor.standbase.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class DiscordService {
    private static final Logger log = LoggerFactory.getLogger(DiscordService.class);

    // Colour palette
    private static final int RED    = 0xED4245; // failure
    private static final int ORANGE = 0xFF6319; // pending (Gulf Orange)
    private static final int GREEN  = 0x57F287; // success
    private static final int GREY   = 0x99AAB5; // neutral

    private final RestClient restClient;
    private final String webhookUrl;

    public DiscordService(@Value("${discord.webhook.url:}") String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.restClient = RestClient.create();
    }

    public void loginFailed(String username, String ip) {
        sendEmbed("⚠️ Login Failed", RED, username, ip);
    }

    public void credentialsAccepted(String username, String ip) {
        sendEmbed("🔑 Password Accepted — Awaiting 2FA", ORANGE, username, ip);
    }

    public void loginSuccess(String username, String ip) {
        sendEmbed("✅ Logged In", GREEN, username, ip);
    }

    public void totpFailed(String username, String ip) {
        sendEmbed("❌ Wrong 2FA Code", RED, username, ip);
    }

    public void totpSuccess(String username, String ip) {
        sendEmbed("✅ 2FA Verified — Login Complete", GREEN, username, ip);
    }

    public void logout(String username, String ip) {
        sendEmbed("🚪 Logged Out", GREY, username, ip);
    }

    public void ipMismatch(UUID userId, String expectedIp, String actualIp) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;

        Map<String, Object> embed = Map.of(
                "title", "🚨 IP Mismatch — Session Invalidated",
                "color", RED,
                "fields", List.of(
                        Map.of("name", "User ID", "value", "`" + userId + "`", "inline", false),
                        Map.of("name", "Expected IP", "value", "`" + expectedIp + "`", "inline", true),
                        Map.of("name", "Actual IP",   "value", "`" + actualIp + "`",  "inline", true)
                ),
                "timestamp", Instant.now().toString(),
                "footer", Map.of("text", "Standbase Auth")
        );

        CompletableFuture.runAsync(() -> {
            try {
                restClient.post()
                        .uri(webhookUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("embeds", List.of(embed)))
                        .retrieve()
                        .toBodilessEntity();
            } catch (Exception e) {
                log.warn("Discord notification failed: {}", e.getMessage());
            }
        });
    }

    private void sendEmbed(String title, int color, String username, String ip) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;

        Map<String, Object> embed = Map.of(
                "title", title,
                "color", color,
                "fields", List.of(
                        Map.of("name", "Username", "value", username, "inline", true),
                        Map.of("name", "IP Address", "value", "`" + ip + "`", "inline", true)
                ),
                "timestamp", Instant.now().toString(),
                "footer", Map.of("text", "Standbase Auth")
        );

        CompletableFuture.runAsync(() -> {
            try {
                restClient.post()
                        .uri(webhookUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("embeds", List.of(embed)))
                        .retrieve()
                        .toBodilessEntity();
            } catch (Exception e) {
                log.warn("Discord notification failed: {}", e.getMessage());
            }
        });
    }
}
