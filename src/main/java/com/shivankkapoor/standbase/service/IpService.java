package com.shivankkapoor.standbase.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IpService {
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
                return ip;
            }
        }
        return request.getRemoteAddr();
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
                    if (second >= 16 && second <= 31) return false;
                } catch (NumberFormatException ignored) {}
            }
        }
        return true;
    }

}
