package com.shivankkapoor.standbase.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class IpServiceHeaderTest {

    @ParameterizedTest
    @ValueSource(strings = {"", "unknown", "127.0.0.1", "192.168.1.10", "169.254.1.1", "no-such-host.invalid"})
    void ignoresUnusableHeaderValues(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", value);
        request.setRemoteAddr("8.8.4.4");

        assertThat(new IpService().getClientIp(request)).isEqualTo("8.8.4.4");
    }
}
