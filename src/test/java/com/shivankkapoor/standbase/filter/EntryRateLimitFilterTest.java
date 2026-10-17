package com.shivankkapoor.standbase.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EntryRateLimitFilterTest {

    private EntryRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new EntryRateLimitFilter();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** Sends 35 requests and returns how many were refused with 429. */
    private int blockedOutOf35(String uri) throws Exception {
        int blocked = 0;
        for (int i = 0; i < 35; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(new MockHttpServletRequest("GET", uri), response, new MockFilterChain());
            if (response.getStatus() == 429) {
                blocked++;
            }
        }
        return blocked;
    }

    // Percent-encoded forms route to the same controllers, so they must share the limit
    @ParameterizedTest
    @ValueSource(strings = {"/entry", "/entry/2026-01-01", "/%65ntry", "/session/check", "/%73ession/check",
            "/heatmap", "/%68eatmap"})
    void limitsHoweverThePathIsEncoded(String uri) throws Exception {
        assertThat(blockedOutOf35(uri)).isEqualTo(5);
    }

    @Test
    void ignoresOtherPaths() throws Exception {
        assertThat(blockedOutOf35("/todos")).isZero();
        assertThat(blockedOutOf35("/entryx")).isZero();
    }
}
