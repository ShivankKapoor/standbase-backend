package com.shivankkapoor.standbase.service;

import com.shivankkapoor.standbase.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AuthServiceFailureTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final DiscordService discordService = mock(DiscordService.class);
    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://aldrop.test");
    private final MockRestServiceServer aldrop = MockRestServiceServer.bindTo(builder).build();
    private final AuthService authService = new AuthService(builder.build(), userRepository, discordService);

    @Test
    void totpSucceedsEvenIfTheSessionCannotBeResolved() {
        aldrop.expect(requestTo("http://aldrop.test/auth/login/verify-totp"))
                .andRespond(withSuccess("{\"token\":\"t\"}", MediaType.APPLICATION_JSON));
        aldrop.expect(requestTo("http://aldrop.test/auth/validate")).andRespond(withServerError());

        AuthService.VerifyTotpResult result = authService.verifyTotp("challenge", "123456", "9.9.9.9", "agent");

        assertThat(result.sessionToken()).isEqualTo("t");
        verify(discordService, never()).totpSuccess(any(), any());
    }

    @Test
    void sessionIsRejectedWhenAldropIsBroken() {
        aldrop.expect(requestTo("http://aldrop.test/auth/validate")).andRespond(withServerError());

        assertThat(authService.getSessionUserID("t", "9.9.9.9", "agent")).isNull();
        verifyNoInteractions(userRepository);
    }

    @Test
    void logoutStillNotifiesWhenAldropFailsAndTheUserIsUnknown() {
        UUID userId = UUID.randomUUID();
        aldrop.expect(requestTo("http://aldrop.test/auth/logout")).andRespond(withServerError());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        authService.logoutByUserId(userId, "t", "9.9.9.9");

        verify(discordService).logout("unknown", "9.9.9.9");
    }
}
