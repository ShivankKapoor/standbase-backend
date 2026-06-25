package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.model.Session;
import com.shivankkapoor.standbase.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class SessionService {
    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    private static final long SESSION_DURATION_HOURS = 4;

    private final DiscordService discordService;
    private final SessionRepository sessionRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public SessionService(DiscordService discordService, SessionRepository sessionRepository) {
        this.discordService = discordService;
        this.sessionRepository = sessionRepository;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    @Transactional
    public String createSession(UUID userId, String ip) {
        log.info("Create session request for user {}", userId);
        invalidateSessions(userId);
        Instant expireTime = Instant.now().plus(SESSION_DURATION_HOURS, ChronoUnit.HOURS);
        String token = generateToken();
        sessionRepository.save(new Session(token, userId, ip, expireTime));
        return token;
    }

    private boolean isTokenValid(Session session, String ip) {
        if (!session.getIp().equals(ip)) {
            log.warn("IP mismatch for user {} expected IP:{} received IP:{} — invalidating session",
                    session.getUserId(), session.getIp(), ip);
            sessionRepository.delete(session);
            UUID userId = session.getUserId();
            String sessionIp = session.getIp();
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        discordService.ipMismatch(userId, sessionIp, ip);
                    }
                });
            } else {
                discordService.ipMismatch(userId, sessionIp, ip);
            }
            return false;
        }
        if (Instant.now().isAfter(session.getExpiresAt())) {
            log.info("Session expired for user {}", session.getUserId());
            sessionRepository.delete(session);
            UUID userId = session.getUserId();
            discordService.sessionExpired(userId, ip);
            return false;
        }
        return true;
    }

    @Transactional
    public UUID getSessionUserID(String sessionToken, String ip) {
        Optional<Session> sessionOpt = sessionRepository.findById(sessionToken);
        if (sessionOpt.isEmpty()) {
            log.warn("No session found for incoming request from IP {}", ip);
            return null;
        }
        Session session = sessionOpt.get();
        if (!isTokenValid(session, ip)) {
            return null;
        }
        return session.getUserId();
    }

    @Transactional
    public void logout(String sessionToken) {
        Optional<Session> session = sessionRepository.findById(sessionToken);
        if (session.isEmpty()) {
            log.warn("Logout request for non-existent session");
            return;
        }
        sessionRepository.deleteById(sessionToken);
        log.info("Session logged out for user {}", session.get().getUserId());
    }

    @Transactional
    public void logoutByUserId(UUID userId) {
        int deleted = sessionRepository.deleteByUserId(userId);
        if (deleted == 0) {
            log.warn("Logout request for non-existent session for user {}", userId);
            return;
        }
        log.info("Session logged out for user {}", userId);
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "America/Chicago")
    @Transactional
    public void evictAllSessions() {
        long count = sessionRepository.count();
        sessionRepository.deleteAll();
        log.info("Session cleanup complete — evicted {} session(s)", count);
        discordService.sessionCleanup((int) count);
    }

    private void invalidateSessions(UUID userId) {
        int deleted = sessionRepository.deleteByUserId(userId);
        if (deleted > 0) {
            log.info("Invalidating previous session for user {}", userId);
        } else {
            log.info("No previous session for {}", userId);
        }
    }
}
