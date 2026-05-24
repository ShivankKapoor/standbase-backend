package com.shivankkapoor.standbase.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {
    record Session(UUID userId, String ip, Instant expiresAt) {
    }

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public String createSession(UUID userId, String ip) {
        Instant expireTime = Instant.now().plus(30, ChronoUnit.MINUTES);
        Session val = new Session(userId, ip, expireTime);
        String userToken = generateToken();
        sessions.put(userToken, val);
        return userToken;
    }

    private boolean isTokenValid(String sessionToken, String ip) {
        Session session = sessions.get(sessionToken);
        if (session == null) {
            return false;
        }
        if (!(session.ip.equals(ip))) {
            return false;
        }
        if (Instant.now().isAfter(session.expiresAt)) {
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
        return session.userId;
    }

    public void logout(String sessionToken) {
        sessions.remove(sessionToken);
    }
}
