package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.model.User;
import com.shivankkapoor.standbase.repository.UserRepository;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    private final PreAuthService preAuthService;
    private final DiscordService discordService;
    private final CodeVerifier codeVerifier;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       SessionService sessionService, PreAuthService preAuthService,
                       DiscordService discordService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.preAuthService = preAuthService;
        this.discordService = discordService;
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
        verifier.setAllowedTimePeriodDiscrepancy(1);
        this.codeVerifier = verifier;
    }

    // Returns sessionToken normally, or null on failure.
    // Returns null with totpRequired=true signal via loginResult when TOTP is needed — use login() overload below.
    public LoginResult login(String username, String password, String ip) {
        Optional<User> attemptedUser = userRepository.findByUsername(username.toLowerCase());
        if (attemptedUser.isEmpty()) {
            log.warn("No user found for login request {}", username);
            discordService.loginFailed(username, ip);
            return LoginResult.failure();
        }
        User user = attemptedUser.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Invalid password attempt for user {}", username);
            discordService.loginFailed(user.getUsername(), ip);
            return LoginResult.failure();
        }
        if (Boolean.TRUE.equals(user.getTotpEnabled())) {
            discordService.credentialsAccepted(user.getUsername(), ip);
            String preAuthToken = preAuthService.createToken(user.getId());
            return LoginResult.totpRequired(preAuthToken);
        }
        discordService.loginSuccess(user.getUsername(), ip);
        String sessionToken = sessionService.createSession(user.getId(), ip);
        return LoginResult.success(sessionToken);
    }

    public String verifyTotp(String preAuthToken, String totpCode, String ip) {
        UUID userId = preAuthService.validateAndConsume(preAuthToken);
        if (userId == null) {
            log.warn("Invalid or expired pre-auth token");
            return null;
        }
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty() || user.get().getTotpSecret() == null) {
            log.warn("User {} not found or has no TOTP secret", userId);
            return null;
        }
        if (!codeVerifier.isValidCode(user.get().getTotpSecret(), totpCode)) {
            log.warn("Invalid TOTP code for user {}", userId);
            discordService.totpFailed(user.get().getUsername(), ip);
            return null;
        }
        discordService.totpSuccess(user.get().getUsername(), ip);
        return sessionService.createSession(userId, ip);
    }

    public String getUsernameById(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getUsername)
                .orElse(null);
    }

    public void logout(String sessionToken) {
        sessionService.logout(sessionToken);
    }

    public void logoutByUserId(UUID userId, String ip) {
        String username = getUsernameById(userId);
        sessionService.logoutByUserId(userId);
        discordService.logout(username != null ? username : "unknown", ip);
    }

    public record LoginResult(boolean success, boolean totpRequired, String sessionToken, String preAuthToken) {
        public static LoginResult failure() {
            return new LoginResult(false, false, null, null);
        }
        public static LoginResult success(String sessionToken) {
            return new LoginResult(true, false, sessionToken, null);
        }
        public static LoginResult totpRequired(String preAuthToken) {
            return new LoginResult(true, true, null, preAuthToken);
        }
    }
}
