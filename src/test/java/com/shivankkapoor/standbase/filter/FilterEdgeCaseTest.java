package com.shivankkapoor.standbase.filter;

import com.shivankkapoor.standbase.service.AuthService;
import com.shivankkapoor.standbase.service.IpService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FilterEdgeCaseTest {

    private final IpService ipService = mock(IpService.class);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static MockHttpServletRequest request(String method, String uri, String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        if (authorization != null) request.addHeader("Authorization", authorization);
        return request;
    }

    private static FilterChain answering(int status) {
        return (req, res) -> ((HttpServletResponse) res).setStatus(status);
    }

    @Test
    void sessionFilterIgnoresNonBearerCredentials() throws Exception {
        AuthService authService = mock(AuthService.class);

        new SessionAuthFilter(authService, ipService)
                .doFilter(request("GET", "/entry", "Basic abc"), new MockHttpServletResponse(), new MockFilterChain());

        verifyNoInteractions(authService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void entryLimitIgnoresRequestsWithoutAUserId() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("not-a-uuid", null, List.of()));
        EntryRateLimitFilter filter = new EntryRateLimitFilter();

        for (int i = 0; i < 35; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request("GET", "/entry", null), response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void invalidTokensAreBlockedAfterFiveRejections() throws Exception {
        InvalidTokenRateLimitFilter filter = new InvalidTokenRateLimitFilter(ipService);
        when(ipService.getClientIp(any())).thenReturn("9.9.9.9");

        for (int i = 0; i < 5; i++) {
            filter.doFilter(request("GET", "/entry", "Bearer bad"), new MockHttpServletResponse(), answering(401));
        }
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(request("GET", "/entry", "Bearer bad"), blocked, answering(401));

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isNotNull();
    }

    @Test
    void invalidTokenLimitSkipsRequestsWithoutABearerToken() throws Exception {
        InvalidTokenRateLimitFilter filter = new InvalidTokenRateLimitFilter(ipService);

        for (String authorization : new String[]{null, "Basic abc"}) {
            filter.doFilter(request("GET", "/entry", authorization), new MockHttpServletResponse(), answering(401));
        }

        verifyNoInteractions(ipService);
    }

    @Test
    void successfulLoginsDoNotUseUpTheLoginLimit() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(ipService);
        when(ipService.getClientIp(any())).thenReturn("9.9.9.9");

        for (int i = 0; i < 15; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request("POST", "/auth/login", null), response, answering(200));
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }
}
