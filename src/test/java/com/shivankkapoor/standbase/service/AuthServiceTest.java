package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.model.User;
import com.shivankkapoor.standbase.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private SessionService sessionService;
    private PreAuthService preAuthService;
    private DiscordService discordService;
    private AuthEventService authEventService;
    private AuthService authService;

    private static final String IP = "1.2.3.4";
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        sessionService = mock(SessionService.class);
        preAuthService = mock(PreAuthService.class);
        discordService = mock(DiscordService.class);
        authEventService = mock(AuthEventService.class);
        authService = new AuthService(userRepository, PASSWORD_ENCODER, sessionService, preAuthService, discordService, authEventService);
    }

    private User buildUser(String password, boolean totpEnabled) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("shivank");
        user.setPassword(PASSWORD_ENCODER.encode(password));
        user.setTotpEnabled(totpEnabled);
        if (totpEnabled) {
            user.setTotpSecret("JBSWY3DPEHPK3PXP");
        }
        return user;
    }

    @Test
    void login_unknownUser_returnsFailure() {
        when(userRepository.findByUsername("shivank")).thenReturn(Optional.empty());
        AuthService.LoginResult result = authService.login("shivank", "pass", IP);
        assertThat(result.success()).isFalse();
        assertThat(result.sessionToken()).isNull();
    }

    @Test
    void login_wrongPassword_returnsFailure() {
        when(userRepository.findByUsername("shivank")).thenReturn(Optional.of(buildUser("correct", false)));
        AuthService.LoginResult result = authService.login("shivank", "wrong", IP);
        assertThat(result.success()).isFalse();
    }

    @Test
    void login_validCredentials_noTotp_returnsSessionToken() {
        User user = buildUser("correct", false);
        when(userRepository.findByUsername("shivank")).thenReturn(Optional.of(user));
        when(sessionService.createSession(user.getId(), IP)).thenReturn("session-token");

        AuthService.LoginResult result = authService.login("shivank", "correct", IP);
        assertThat(result.success()).isTrue();
        assertThat(result.totpRequired()).isFalse();
        assertThat(result.sessionToken()).isEqualTo("session-token");
        assertThat(result.preAuthToken()).isNull();
    }

    @Test
    void login_validCredentials_totpEnabled_returnsTotpRequired() {
        User user = buildUser("correct", true);
        when(userRepository.findByUsername("shivank")).thenReturn(Optional.of(user));
        when(preAuthService.createToken(user.getId())).thenReturn("pre-auth-token");

        AuthService.LoginResult result = authService.login("shivank", "correct", IP);
        assertThat(result.success()).isTrue();
        assertThat(result.totpRequired()).isTrue();
        assertThat(result.preAuthToken()).isEqualTo("pre-auth-token");
        assertThat(result.sessionToken()).isNull();
        verifyNoInteractions(sessionService);
    }

    @Test
    void verifyTotp_invalidPreAuthToken_returnsNull() {
        when(preAuthService.validateAndConsume("bad-token")).thenReturn(null);
        assertThat(authService.verifyTotp("bad-token", "123456", IP)).isNull();
        verifyNoInteractions(sessionService);
    }

    @Test
    void verifyTotp_userNotFound_returnsNull() {
        UUID userId = UUID.randomUUID();
        when(preAuthService.validateAndConsume("token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThat(authService.verifyTotp("token", "123456", IP)).isNull();
    }

    @Test
    void verifyTotp_invalidTotpCode_returnsNull() {
        User user = buildUser("pass", true);
        when(preAuthService.validateAndConsume("token")).thenReturn(user.getId());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        // "000000" is almost certainly not a valid TOTP code for this secret
        assertThat(authService.verifyTotp("token", "000000", IP)).isNull();
        verifyNoInteractions(sessionService);
    }

    @Test
    void getUsernameById_knownUser_returnsUsername() {
        User user = buildUser("pass", false);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        assertThat(authService.getUsernameById(user.getId())).isEqualTo("shivank");
    }

    @Test
    void getUsernameById_unknownUser_returnsNull() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        assertThat(authService.getUsernameById(id)).isNull();
    }
}
