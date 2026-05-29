package com.shivankkapoor.standbase.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {
    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    record Session(UUID userId, String ip, Instant expiresAt) {
    }

    private final DiscordService discordService;
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> reverseSessionLookup = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public SessionService(DiscordService discordService) {
        this.discordService = discordService;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public String createSession(UUID userId, String ip) {
        log.info("Create session request for user {}", userId);
        invalidateSessions(userId);
        Instant expireTime = Instant.now().plus(30, ChronoUnit.MINUTES);
        Session val = new Session(userId, ip, expireTime);
        String userToken = generateToken();
        sessions.put(userToken, val);
        reverseSessionLookup.put(userId, userToken);
        return userToken;
    }

    private boolean isTokenValid(String sessionToken, String ip) {
        Session session = sessions.get(sessionToken);
        if (session == null) {
            log.warn("No session found for incoming request from IP {}", ip);
            return false;
        }
        if (!(session.ip.equals(ip))) {
            log.warn("IP mismatch for user {} expected IP:{} received IP:{} — invalidating session", session.userId(), session.ip(), ip);
            logout(sessionToken);
            discordService.ipMismatch(session.userId(), session.ip(), ip);
            return false;
        }
        if (Instant.now().isAfter(session.expiresAt)) {
            log.info("Session expired for user {}", session.userId());
            logout(sessionToken);
            return false;
        }
        return true;
    }

    public UUID getSessionUserID(String sessionToken, String ip) {
        if (!(isTokenValid(sessionToken, ip))) {
            return null;
        }
        Session session = sessions.get(sessionToken);
        return session.userId();
    }

    public void logout(String sessionToken) {
        Session session = sessions.remove(sessionToken);
        if (session == null) {
            log.warn("Logout request for non-existent session");
            return;
        }
        reverseSessionLookup.remove(session.userId());
        log.info("Session logged out for user {}", session.userId());
    }

    public void logoutByUserId(UUID userId) {
        String sessionToken = reverseSessionLookup.get(userId);
        if (sessionToken == null) {
            log.warn("Logout request for non-existent session for user {}", userId);
            return;
        }
        logout(sessionToken);
    }

    @Scheduled(cron = "0 0 12 * * *", zone = "America/Chicago")
    public void evictExpiredSessions() {
        int[] count = {0};
        sessions.entrySet().removeIf(entry -> {
            if (Instant.now().isAfter(entry.getValue().expiresAt())) {
                reverseSessionLookup.remove(entry.getValue().userId());
                count[0]++;
                return true;
            }
            return false;
        });
        log.info("Session cleanup complete — evicted {} expired session(s)", count[0]);
        discordService.sessionCleanup(count[0]);
    }

    private void invalidateSessions(UUID userId){
        String prevSessionToken = reverseSessionLookup.get(userId);
        if(prevSessionToken==null){
            log.info("No previous session for {}",userId);
        }else{
            log.info("Invalidating previous session {}",prevSessionToken);
            logout(prevSessionToken);
        }
    }
}
