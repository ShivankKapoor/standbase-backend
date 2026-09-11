package com.shivankkapoor.standbase.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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
    private final boolean dev;

    public DiscordService(@Value("${discord.webhook.url:}") String webhookUrl,
                          @Value("${application.env:}") String env) {
        this.webhookUrl = webhookUrl;
        this.dev = "DEV".equalsIgnoreCase(env);
        this.restClient = RestClient.create();
    }

    @Async
    public void loginFailed(String username, String ip) {
        sendEmbed("⚠️ Login Failed", RED, username, ip);
    }

    @Async
    public void credentialsAccepted(String username, String ip) {
        sendEmbed("🔑 Password Accepted — Awaiting 2FA", ORANGE, username, ip);
    }

    @Async
    public void loginSuccess(String username, String ip) {
        sendEmbed("✅ Logged In", GREEN, username, ip);
    }

    // Aldrop conflates wrong code, replayed code, and expired/exhausted challenge into one 401
    // with no userId, so this can no longer be attributed to a username — IP only.
    @Async
    public void totpFailed(String ip) {
        if (dev) {
            log.warn("[DEV] Skipping Discord notification: 2FA verification failed from {}", ip);
            return;
        }
        if (webhookUrl == null || webhookUrl.isBlank()) return;

        Map<String, Object> embed = Map.of(
                "title", "❌ 2FA Verification Failed",
                "color", RED,
                "fields", List.of(
                        Map.of("name", "IP Address", "value", "`" + ip + "`", "inline", true)
                ),
                "timestamp", Instant.now().toString(),
                "footer", Map.of("text", "Standbase Auth")
        );

        post(embed);
    }

    @Async
    public void totpSuccess(String username, String ip) {
        sendEmbed("✅ 2FA Verified — Login Complete", GREEN, username, ip);
    }

    @Async
    public void logout(String username, String ip) {
        sendEmbed("🚪 Logged Out", GREY, username, ip);
    }

    private void sendEmbed(String title, int color, String username, String ip) {
        if (dev) {
            log.warn("[DEV] Skipping Discord notification: {} for {} from {}", title, username, ip);
            return;
        }
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

        post(embed);
    }

    private void post(Map<String, Object> embed) {
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
    }
}
