package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.model.User;
import com.shivankkapoor.standbase.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AuthServiceTest {

    private static final String BASE_URL = "http://aldrop.test";
    private static final String IP = "1.2.3.4";
    private static final String USER_AGENT = "test-agent";

    private UserRepository userRepository;
    private DiscordService discordService;
    private MockRestServiceServer server;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        discordService = mock(DiscordService.class);

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        authService = new AuthService(restClient, userRepository, discordService);
    }

    private User buildUser(UUID id, String username) {
        return new User(id, username, java.time.OffsetDateTime.now());
    }

    private void expectValidateSucceeds(UUID userId, String username) {
        server.expect(requestTo(BASE_URL + "/auth/validate"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"userId\":\"" + userId + "\",\"username\":\"" + username
                                + "\",\"expiresAt\":\"2026-01-01T00:00:00Z\"}",
                        MediaType.APPLICATION_JSON));
    }

    @Test
    void login_wrongCredentials_returnsFailure() {
        server.expect(requestTo(BASE_URL + "/auth/login"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"Wrong username or password\"}"));

        AuthService.LoginResult result = authService.login("shivank", "wrong", IP, USER_AGENT);

        assertThat(result.success()).isFalse();
        assertThat(result.rateLimited()).isFalse();
        verify(discordService).loginFailed("shivank", IP);
        server.verify();
    }

    @Test
    void login_rateLimitedByAldrop_returnsRateLimited() {
        server.expect(requestTo(BASE_URL + "/auth/login"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"Too many attempts\"}"));

        AuthService.LoginResult result = authService.login("shivank", "pass", IP, USER_AGENT);

        assertThat(result.rateLimited()).isTrue();
        assertThat(result.success()).isFalse();
        verifyNoInteractions(discordService);
        server.verify();
    }

    @Test
    void login_validCredentials_noTotp_returnsSessionToken() {
        // Login alone makes no /auth/validate call — the local user row is provisioned on the
        // first authenticated request instead.
        server.expect(requestTo(BASE_URL + "/auth/login"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"token\":\"session-token\",\"totpToken\":null,\"expiresAt\":\"2026-01-01T00:00:00Z\"}",
                        MediaType.APPLICATION_JSON));

        AuthService.LoginResult result = authService.login("shivank", "correct", IP, USER_AGENT);

        assertThat(result.success()).isTrue();
        assertThat(result.totpRequired()).isFalse();
        assertThat(result.sessionToken()).isEqualTo("session-token");
        verify(discordService).loginSuccess("shivank", IP);
        server.verify();
    }

    @Test
    void login_validCredentials_totpEnabled_returnsTotpRequired() {
        server.expect(requestTo(BASE_URL + "/auth/login"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"token\":null,\"totpToken\":\"totp-token\",\"expiresAt\":\"2026-01-01T00:05:00Z\"}",
                        MediaType.APPLICATION_JSON));

        AuthService.LoginResult result = authService.login("shivank", "correct", IP, USER_AGENT);

        assertThat(result.success()).isTrue();
        assertThat(result.totpRequired()).isTrue();
        assertThat(result.preAuthToken()).isEqualTo("totp-token");
        assertThat(result.sessionToken()).isNull();
        verify(discordService).credentialsAccepted("shivank", IP);
        server.verify();
    }

    @Test
    void verifyTotp_rejectedByAldrop_returnsFailureWithIpOnlyAlert() {
        server.expect(requestTo(BASE_URL + "/auth/login/verify-totp"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"Invalid code\"}"));

        AuthService.VerifyTotpResult result = authService.verifyTotp("totp-token", "000000", IP, USER_AGENT);

        assertThat(result.success()).isFalse();
        verify(discordService).totpFailed(IP);
        server.verify();
    }

    @Test
    void verifyTotp_rateLimitedByAldrop_returnsRateLimited() {
        server.expect(requestTo(BASE_URL + "/auth/login/verify-totp"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"Too many attempts\"}"));

        AuthService.VerifyTotpResult result = authService.verifyTotp("totp-token", "123456", IP, USER_AGENT);

        assertThat(result.rateLimited()).isTrue();
        verifyNoInteractions(discordService);
        server.verify();
    }

    @Test
    void verifyTotp_validCode_returnsSessionToken() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(true);

        server.expect(requestTo(BASE_URL + "/auth/login/verify-totp"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"token\":\"session-token\",\"totpToken\":null,\"expiresAt\":\"2026-01-01T00:00:00Z\"}",
                        MediaType.APPLICATION_JSON));
        expectValidateSucceeds(userId, "shivank");

        AuthService.VerifyTotpResult result = authService.verifyTotp("totp-token", "123456", IP, USER_AGENT);

        assertThat(result.success()).isTrue();
        assertThat(result.sessionToken()).isEqualTo("session-token");
        verify(discordService).totpSuccess("shivank", IP);
        server.verify();
    }

    @Test
    void getSessionUserID_validToken_provisionsUnseenUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(false);
        expectValidateSucceeds(userId, "shivank");

        assertThat(authService.getSessionUserID("token", IP, USER_AGENT)).isEqualTo(userId);

        verify(userRepository).save(org.mockito.ArgumentMatchers.any(User.class));
        server.verify();
    }

    @Test
    void getSessionUserID_everyRequestRevalidatesAgainstAldrop() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(true);

        // Nothing is cached, so two lookups of the same token must make two /auth/validate calls.
        expectValidateSucceeds(userId, "shivank");
        expectValidateSucceeds(userId, "shivank");

        assertThat(authService.getSessionUserID("token", IP, USER_AGENT)).isEqualTo(userId);
        assertThat(authService.getSessionUserID("token", IP, USER_AGENT)).isEqualTo(userId);
        server.verify();
    }

    @Test
    void getSessionUserID_tokenReplayedFromAnotherDevice_isRejected() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(true);

        // The legitimate device succeeds; the same token from another device must still reach
        // Aldrop, which rejects the device-binding mismatch.
        expectValidateSucceeds(userId, "shivank");
        server.expect(requestTo(BASE_URL + "/auth/validate"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"Invalid session\"}"));

        assertThat(authService.getSessionUserID("token", IP, USER_AGENT)).isEqualTo(userId);
        assertThat(authService.getSessionUserID("token", "9.9.9.9", "attacker-agent")).isNull();
        server.verify();
    }

    @Test
    void getSessionUserID_invalidToken_returnsNull() {
        server.expect(requestTo(BASE_URL + "/auth/validate"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"Invalid session\"}"));

        assertThat(authService.getSessionUserID("bad-token", IP, USER_AGENT)).isNull();
        server.verify();
    }

    @Test
    void logoutByUserId_callsAldropAndAlerts() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "shivank");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        server.expect(requestTo(BASE_URL + "/auth/logout"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        authService.logoutByUserId(userId, "session-token", IP);

        verify(discordService).logout("shivank", IP);
        server.verify();
    }

    @Test
    void getUsernameById_knownUser_returnsUsername() {
        User user = buildUser(UUID.randomUUID(), "shivank");
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
