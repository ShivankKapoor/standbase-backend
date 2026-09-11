package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.dto.aldrop.AldropLoginRequestDTO;
import com.shivankkapoor.standbase.dto.aldrop.AldropLoginResponseDTO;
import com.shivankkapoor.standbase.dto.aldrop.AldropLogoutRequestDTO;
import com.shivankkapoor.standbase.dto.aldrop.AldropValidateSessionRequestDTO;
import com.shivankkapoor.standbase.dto.aldrop.AldropValidateSessionResponseDTO;
import com.shivankkapoor.standbase.dto.aldrop.AldropVerifyTotpRequestDTO;
import com.shivankkapoor.standbase.model.User;
import com.shivankkapoor.standbase.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final RestClient aldropRestClient;
    private final UserRepository userRepository;
    private final DiscordService discordService;

    public AuthService(RestClient aldropRestClient, UserRepository userRepository,
                       DiscordService discordService) {
        this.aldropRestClient = aldropRestClient;
        this.userRepository = userRepository;
        this.discordService = discordService;
    }

    public LoginResult login(String username, String password, String ip, String userAgent) {
        AldropLoginResponseDTO response;
        try {
            response = post("/auth/login", new AldropLoginRequestDTO(username, password, ip, userAgent),
                    AldropLoginResponseDTO.class);
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("Login rate-limited by Aldrop for user {}", username);
            return LoginResult.tooManyAttempts();
        } catch (HttpClientErrorException e) {
            log.warn("Login rejected by Aldrop for user {}: {}", username, e.getStatusCode());
            discordService.loginFailed(username, ip);
            return LoginResult.failure();
        }

        if (response.token() != null) {
            discordService.loginSuccess(username, ip);
            return LoginResult.success(response.token());
        }

        discordService.credentialsAccepted(username, ip);
        return LoginResult.totpRequired(response.totpToken());
    }

    public VerifyTotpResult verifyTotp(String totpToken, String code, String ip, String userAgent) {
        AldropLoginResponseDTO response;
        try {
            response = post("/auth/login/verify-totp",
                    new AldropVerifyTotpRequestDTO(totpToken, code, ip, userAgent), AldropLoginResponseDTO.class);
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("TOTP verification rate-limited by Aldrop");
            return VerifyTotpResult.tooManyAttempts();
        } catch (HttpClientErrorException e) {
            log.warn("TOTP verification rejected by Aldrop: {}", e.getStatusCode());
            discordService.totpFailed(ip);
            return VerifyTotpResult.failure();
        }

        // Resolved for the username only — verify-totp carries no username of its own.
        ValidatedSession session = resolveSession(response.token(), ip, userAgent);
        if (session != null) {
            discordService.totpSuccess(session.username(), ip);
        }
        return VerifyTotpResult.success(response.token());
    }

    public UUID getSessionUserID(String token, String ip, String userAgent) {
        ValidatedSession resolved = resolveSession(token, ip, userAgent);
        return resolved != null ? resolved.userId() : null;
    }

    public String getUsernameById(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getUsername)
                .orElse(null);
    }

    public void logoutByUserId(UUID userId, String token, String ip) {
        try {
            postNoContent("/auth/logout", new AldropLogoutRequestDTO(token, ip));
        } catch (Exception e) {
            log.warn("Aldrop logout call failed for user {}", userId, e);
        }
        String username = getUsernameById(userId);
        discordService.logout(username != null ? username : "unknown", ip);
    }

    // Every authenticated request resolves through Aldrop — no caching, so a revoked or expired
    // session stops working immediately and device binding is enforced on every call.
    private ValidatedSession resolveSession(String token, String ip, String userAgent) {
        AldropValidateSessionResponseDTO response;
        try {
            response = post("/auth/validate", new AldropValidateSessionRequestDTO(token, ip, userAgent),
                    AldropValidateSessionResponseDTO.class);
        } catch (HttpClientErrorException e) {
            log.debug("Aldrop rejected session validation: {}", e.getStatusCode());
            return null;
        } catch (Exception e) {
            log.warn("Aldrop session validation call failed", e);
            return null;
        }

        provisionIfAbsent(response.userId(), response.username());
        return new ValidatedSession(response.userId(), response.username());
    }

    // No-op for the single pre-linked user set up during migration; provisions a local row for
    // any Aldrop user Standbase hasn't seen before.
    private void provisionIfAbsent(UUID userId, String username) {
        if (userRepository.existsById(userId)) {
            return;
        }
        userRepository.save(new User(userId, username, OffsetDateTime.now()));
        log.info("Provisioned local user record for Aldrop user {}", userId);
    }

    private <T> T post(String uri, Object body, Class<T> responseType) {
        return aldropRestClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(responseType);
    }

    private void postNoContent(String uri, Object body) {
        aldropRestClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    public record ValidatedSession(UUID userId, String username) {
    }

    public record LoginResult(boolean success, boolean totpRequired, boolean rateLimited,
                              String sessionToken, String preAuthToken) {
        public static LoginResult failure() {
            return new LoginResult(false, false, false, null, null);
        }
        public static LoginResult tooManyAttempts() {
            return new LoginResult(false, false, true, null, null);
        }
        public static LoginResult success(String sessionToken) {
            return new LoginResult(true, false, false, sessionToken, null);
        }
        public static LoginResult totpRequired(String preAuthToken) {
            return new LoginResult(true, true, false, null, preAuthToken);
        }
    }

    public record VerifyTotpResult(boolean success, boolean rateLimited, String sessionToken) {
        public static VerifyTotpResult failure() {
            return new VerifyTotpResult(false, false, null);
        }
        public static VerifyTotpResult tooManyAttempts() {
            return new VerifyTotpResult(false, true, null);
        }
        public static VerifyTotpResult success(String sessionToken) {
            return new VerifyTotpResult(true, false, sessionToken);
        }
    }
}
