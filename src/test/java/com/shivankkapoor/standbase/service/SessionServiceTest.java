package com.shivankkapoor.standbase.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SessionServiceTest {

    private DiscordService discordService;
    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        discordService = mock(DiscordService.class);
        sessionService = new SessionService(discordService);
    }

    @Test
    void createSession_returnsToken() {
        String token = sessionService.createSession(UUID.randomUUID(), "1.2.3.4");
        assertThat(token).isNotNull().hasSize(64);
    }

    @Test
    void getSessionUserID_validTokenAndIp_returnsUserId() {
        UUID userId = UUID.randomUUID();
        String token = sessionService.createSession(userId, "1.2.3.4");
        assertThat(sessionService.getSessionUserID(token, "1.2.3.4")).isEqualTo(userId);
    }

    @Test
    void getSessionUserID_wrongIp_returnsNull() {
        UUID userId = UUID.randomUUID();
        String token = sessionService.createSession(userId, "1.2.3.4");
        assertThat(sessionService.getSessionUserID(token, "5.6.7.8")).isNull();
    }

    @Test
    void getSessionUserID_wrongIp_invalidatesTokenAndNotifiesDiscord() {
        UUID userId = UUID.randomUUID();
        String token = sessionService.createSession(userId, "1.2.3.4");
        sessionService.getSessionUserID(token, "5.6.7.8");
        assertThat(sessionService.getSessionUserID(token, "1.2.3.4")).isNull();
        verify(discordService).ipMismatch(userId, "1.2.3.4", "5.6.7.8");
    }

    @Test
    void getSessionUserID_unknownToken_returnsNull() {
        assertThat(sessionService.getSessionUserID("nonexistent", "1.2.3.4")).isNull();
    }

    @Test
    void logout_removesSession() {
        UUID userId = UUID.randomUUID();
        String token = sessionService.createSession(userId, "1.2.3.4");
        sessionService.logout(token);
        assertThat(sessionService.getSessionUserID(token, "1.2.3.4")).isNull();
    }

    @Test
    void logoutByUserId_removesSession() {
        UUID userId = UUID.randomUUID();
        String token = sessionService.createSession(userId, "1.2.3.4");
        sessionService.logoutByUserId(userId);
        assertThat(sessionService.getSessionUserID(token, "1.2.3.4")).isNull();
    }

    @Test
    void createSession_invalidatesPreviousSession() {
        UUID userId = UUID.randomUUID();
        String token1 = sessionService.createSession(userId, "1.2.3.4");
        String token2 = sessionService.createSession(userId, "1.2.3.4");
        assertThat(sessionService.getSessionUserID(token1, "1.2.3.4")).isNull();
        assertThat(sessionService.getSessionUserID(token2, "1.2.3.4")).isEqualTo(userId);
    }
}
