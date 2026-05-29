package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.model.AuthEvent;
import com.shivankkapoor.standbase.model.AuthEventType;
import com.shivankkapoor.standbase.repository.AuthEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthEventService {
    private static final Logger log = LoggerFactory.getLogger(AuthEventService.class);
    private final AuthEventRepository authEventRepository;
    private final boolean dev;

    public AuthEventService(AuthEventRepository authEventRepository,
                            @Value("${application.env:}") String env) {
        this.authEventRepository = authEventRepository;
        this.dev = "DEV".equalsIgnoreCase(env);
    }

    @Async
    public void logAuthEvent(UUID userId, String ip, AuthEventType eventType) {
        if (dev) {
            log.warn("[DEV] Skipping auth event storage: {} for user {} from {}", eventType, userId, ip);
            return;
        }
        AuthEvent authEvent = new AuthEvent();
        authEvent.setUserId(userId);
        authEvent.setIpAddress(ip);
        authEvent.setEventType(eventType);
        log.info("Logging auth event {} for user {} from {}", eventType, userId, ip);
        try {
            authEventRepository.save(authEvent);
        } catch (Exception e) {
            log.error("Unable to save auth event", e);
        }
    }
}
