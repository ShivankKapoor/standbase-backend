package com.shivankkapoor.standbase.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IpServiceTest {

    private IpService ipService;

    @BeforeEach
    void setUp() {
        ipService = new IpService();
    }

    private HttpServletRequest request(String... headerPairs) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader(anyString())).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("9.9.9.9");
        for (int i = 0; i < headerPairs.length; i += 2) {
            when(req.getHeader(headerPairs[i])).thenReturn(headerPairs[i + 1]);
        }
        return req;
    }

    @Test
    void cfConnectingIp_validPublicIp_returned() {
        assertThat(ipService.getClientIp(request("CF-Connecting-IP", "203.0.113.1"))).isEqualTo("203.0.113.1");
    }

    @Test
    void xForwardedFor_multipleIps_returnsFirst() {
        assertThat(ipService.getClientIp(request("X-Forwarded-For", "203.0.113.1, 10.0.0.1"))).isEqualTo("203.0.113.1");
    }

    @Test
    void cfConnectingIp_192168Block_fallsThrough() {
        assertThat(ipService.getClientIp(request("CF-Connecting-IP", "192.168.1.1"))).isEqualTo("9.9.9.9");
    }

    @Test
    void cfConnectingIp_10Block_fallsThrough() {
        assertThat(ipService.getClientIp(request("CF-Connecting-IP", "10.0.0.5"))).isEqualTo("9.9.9.9");
    }

    @Test
    void cfConnectingIp_172_16to31Block_fallsThrough() {
        assertThat(ipService.getClientIp(request("CF-Connecting-IP", "172.16.0.1"))).isEqualTo("9.9.9.9");
        assertThat(ipService.getClientIp(request("CF-Connecting-IP", "172.31.255.255"))).isEqualTo("9.9.9.9");
    }

    @Test
    void cfConnectingIp_172_32Block_isAccepted() {
        // 172.32.x.x is outside RFC 1918, should be accepted
        assertThat(ipService.getClientIp(request("CF-Connecting-IP", "172.32.0.1"))).isEqualTo("172.32.0.1");
    }

    @Test
    void cfConnectingIp_localhost_fallsThrough() {
        assertThat(ipService.getClientIp(request("CF-Connecting-IP", "127.0.0.1"))).isEqualTo("9.9.9.9");
    }

    @Test
    void cfConnectingIp_unknown_fallsThrough() {
        assertThat(ipService.getClientIp(request("CF-Connecting-IP", "unknown"))).isEqualTo("9.9.9.9");
    }

    @Test
    void noHeaders_returnsRemoteAddr() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader(anyString())).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("8.8.8.8");
        assertThat(ipService.getClientIp(req)).isEqualTo("8.8.8.8");
    }

    @Test
    void headerPriority_cfConnectingIpBeforeXForwardedFor() {
        assertThat(ipService.getClientIp(request(
                "CF-Connecting-IP", "203.0.113.1",
                "X-Forwarded-For", "203.0.113.2"
        ))).isEqualTo("203.0.113.1");
    }
}
