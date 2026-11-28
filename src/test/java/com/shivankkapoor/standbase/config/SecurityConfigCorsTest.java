package com.shivankkapoor.standbase.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigCorsTest {

    @Test
    void onlyConfiguredOriginsAreAllowed() {
        SecurityConfig config = new SecurityConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", "https://app.example");
        CorsConfigurationSource source = config.corsConfigurationSource();

        for (String origin : new String[]{null, "https://app.example", "https://evil.example"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/entry");
            if (origin != null) request.addHeader("Origin", origin);

            assertThat(source.getCorsConfiguration(request).getAllowedOrigins())
                    .containsExactly("https://app.example");
        }
    }
}
