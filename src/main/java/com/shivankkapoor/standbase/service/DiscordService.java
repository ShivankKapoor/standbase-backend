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
import java.util.UUID;

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

    @Async
    public void totpFailed(String username, String ip) {
        sendEmbed("❌ Wrong 2FA Code", RED, username, ip);
    }

    @Async
    public void totpSuccess(String username, String ip) {
        sendEmbed("✅ 2FA Verified — Login Complete", GREEN, username, ip);
    }

    @Async
    public void logout(String username, String ip) {
        sendEmbed("🚪 Logged Out", GREY, username, ip);
    }

    @Async
    public void sessionCleanup(int evicted) {
        if (dev) {
            log.warn("[DEV] Skipping Discord notification: session cleanup evicted {} session(s)", evicted);
            return;
        }
        if (webhookUrl == null || webhookUrl.isBlank()) return;

        Map<String, Object> embed = Map.of(
                "title", "🧹 Session Cleanup",
                "color", GREY,
                "fields", List.of(
                        Map.of("name", "Evicted Sessions", "value", String.valueOf(evicted), "inline", true)
                ),
                "timestamp", Instant.now().toString(),
                "footer", Map.of("text", "Standbase Auth")
        );

        post(embed);
    }

    @Async
    public void ipMismatch(UUID userId, String expectedIp, String actualIp) {
        if (dev) {
            log.warn("[DEV] Skipping Discord notification: IP mismatch for user {} expected {} got {}", userId, expectedIp, actualIp);
            return;
        }
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

        post(embed);
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
