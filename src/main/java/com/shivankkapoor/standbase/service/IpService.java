package com.shivankkapoor.standbase.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IpService {
    private static final Logger log = LoggerFactory.getLogger(IpService.class);

    public String getClientIp(HttpServletRequest request) {
        String[] headers = {
                "CF-Connecting-IP",
                "X-Forwarded-For",
                "X-Real-IP",
                "X-Client-IP"
        };

        for (String header : headers) {
            var value = request.getHeader(header);
            if (value == null) {
                continue;
            }
            String ip = value.split(",")[0].trim();
            if (isValid(ip)) {
                log.debug("Resolved client IP {} from header {}", ip, header);
                return ip;
            }
            log.debug("Header {} present but value '{}' failed validation", header, value);
        }
        String fallback = request.getRemoteAddr();
        log.debug("No valid proxy header found, falling back to remote address {}", fallback);
        return fallback;
    }

    private boolean isValid(String ip) {
        if (ip.isBlank())
            return false;
        if (ip.length() < 7)
            return false;
        String lower = ip.toLowerCase();
        if (List.of("unknown", "localhost", "127.0.0.1", "::1").contains(lower))
            return false;
        if (lower.startsWith("192.168.") || lower.startsWith("10."))
            return false;
        if (lower.startsWith("172.")) {
            String[] parts = lower.split("\\.");
            if (parts.length >= 2) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    if (second >= 16 && second <= 31)
                        return false;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return true;
    }

}
