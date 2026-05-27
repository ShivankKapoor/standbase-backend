package com.shivankkapoor.standbase.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PreAuthService {
    private static final Logger log = LoggerFactory.getLogger(PreAuthService.class);

    record PreAuthToken(UUID userId, Instant expiresAt) {}

    private final ConcurrentHashMap<String, PreAuthToken> tokens = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public String createToken(UUID userId) {
        String token = generateToken();
        tokens.put(token, new PreAuthToken(userId, Instant.now().plus(5, ChronoUnit.MINUTES)));
        log.info("Pre-auth token created for user {}", userId);
        return token;
    }

    public UUID validateAndConsume(String token) {
        PreAuthToken preAuth = tokens.remove(token);
        if (preAuth == null) {
            log.warn("Pre-auth token not found");
            return null;
        }
        if (Instant.now().isAfter(preAuth.expiresAt())) {
            log.warn("Pre-auth token expired for user {}", preAuth.userId());
            return null;
        }
        return preAuth.userId();
    }
}
