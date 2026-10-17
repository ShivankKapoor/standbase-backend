package com.shivankkapoor.standbase.filter;

import com.shivankkapoor.standbase.service.IpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthRateLimitFilterTest {

    private AuthRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        IpService ipService = mock(IpService.class);
        when(ipService.getClientIp(any())).thenReturn("203.0.113.9");
        filter = new AuthRateLimitFilter(ipService);
    }

    /** Sends 15 failed attempts and returns how many were refused with 429. */
    private int blockedOutOf15(String method, String uri) throws Exception {
        int blocked = 0;
        for (int i = 0; i < 15; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(new MockHttpServletRequest(method, uri), response,
                    (req, res) -> ((MockHttpServletResponse) res).setStatus(401));
            if (response.getStatus() == 429) {
                blocked++;
            }
        }
        return blocked;
    }

    // Percent-encoded forms route to the same controller, so they must share the limit
    @ParameterizedTest
    @ValueSource(strings = {"/auth/login", "/auth/%6cogin", "/%61uth/login", "/auth/totp/verify", "/auth/totp/%76erify"})
    void limitsFailedLoginsHoweverThePathIsEncoded(String uri) throws Exception {
        assertThat(blockedOutOf15("POST", uri)).isEqualTo(5);
    }

    @Test
    void ignoresOtherMethodsAndPaths() throws Exception {
        assertThat(blockedOutOf15("GET", "/auth/login")).isZero();
        assertThat(blockedOutOf15("POST", "/entry")).isZero();
    }
}
