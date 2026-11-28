package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.support.StubHttpServer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class DiscordServiceTest {

    private static final List<Consumer<DiscordService>> ALL = List.of(
            s -> s.loginFailed("sam", "9.9.9.9"),
            s -> s.credentialsAccepted("sam", "9.9.9.9"),
            s -> s.loginSuccess("sam", "9.9.9.9"),
            s -> s.totpFailed("9.9.9.9"),
            s -> s.totpSuccess("sam", "9.9.9.9"),
            s -> s.logout("sam", "9.9.9.9"));

    @Test
    void postsEveryNotificationToTheWebhook() throws Exception {
        try (StubHttpServer server = new StubHttpServer(204)) {
            DiscordService service = new DiscordService(server.url(), "PROD");
            ALL.forEach(call -> call.accept(service));

            assertThat(server.requests).hasSize(ALL.size());
            assertThat(server.requests).allSatisfy(r -> assertThat(r.body()).contains("embeds", "9.9.9.9"));
        }
    }

    @Test
    void sendsNothingInDev() throws Exception {
        try (StubHttpServer server = new StubHttpServer(204)) {
            DiscordService service = new DiscordService(server.url(), "dev");
            ALL.forEach(call -> call.accept(service));

            assertThat(server.requests).isEmpty();
        }
    }

    @Test
    void doesNothingWithoutAWebhook() {
        for (String webhook : new String[]{null, " "}) {
            DiscordService service = new DiscordService(webhook, "PROD");
            ALL.forEach(call -> assertThatCode(() -> call.accept(service)).doesNotThrowAnyException());
        }
    }

    @Test
    void swallowsWebhookFailures() throws Exception {
        try (StubHttpServer server = new StubHttpServer(500)) {
            DiscordService service = new DiscordService(server.url(), "PROD");

            assertThatCode(() -> service.loginSuccess("sam", "9.9.9.9")).doesNotThrowAnyException();
            assertThat(server.requests).hasSize(1);
        }
    }
}
