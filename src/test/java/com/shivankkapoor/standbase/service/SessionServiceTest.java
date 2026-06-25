package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.model.Session;
import com.shivankkapoor.standbase.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SessionServiceTest {

    private DiscordService discordService;
    private SessionRepository sessionRepository;
    private SessionService sessionService;
    private Map<String, Session> store;

    @BeforeEach
    void setUp() {
        store = new HashMap<>();
        discordService = mock(DiscordService.class);
        sessionRepository = mock(SessionRepository.class);

        when(sessionRepository.save(any())).thenAnswer(inv -> {
            Session s = inv.getArgument(0);
            store.put(s.getToken(), s);
            return s;
        });
        when(sessionRepository.findById(any())).thenAnswer(inv ->
                Optional.ofNullable(store.get((String) inv.getArgument(0))));
        when(sessionRepository.findByUserId(any())).thenAnswer(inv ->
                store.values().stream()
                        .filter(s -> s.getUserId().equals(inv.getArgument(0)))
                        .findFirst());
        doAnswer(inv -> {
            store.remove(((Session) inv.getArgument(0)).getToken());
            return null;
        }).when(sessionRepository).delete(any());
        doAnswer(inv -> {
            store.remove((String) inv.getArgument(0));
            return null;
        }).when(sessionRepository).deleteById(any());
        doAnswer(inv -> {
            UUID userId = inv.getArgument(0);
            int before = store.size();
            store.values().removeIf(s -> s.getUserId().equals(userId));
            return before - store.size();
        }).when(sessionRepository).deleteByUserId(any());

        sessionService = new SessionService(discordService, sessionRepository);
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
    void getSessionUserID_expiredSession_returnsNull() {
        UUID userId = UUID.randomUUID();
        Session expired = new Session("expiredtoken", userId, "1.2.3.4", Instant.now().minusSeconds(1));
        store.put("expiredtoken", expired);
        assertThat(sessionService.getSessionUserID("expiredtoken", "1.2.3.4")).isNull();
    }

    @Test
    void getSessionUserID_expiredSession_notifiesDiscord() {
        UUID userId = UUID.randomUUID();
        Session expired = new Session("expiredtoken", userId, "1.2.3.4", Instant.now().minusSeconds(1));
        store.put("expiredtoken", expired);
        sessionService.getSessionUserID("expiredtoken", "1.2.3.4");
        verify(discordService).sessionExpired(userId, "1.2.3.4");
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
