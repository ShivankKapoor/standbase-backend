package com.shivankkapoor.standbase.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
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
        if ("unknown".equalsIgnoreCase(ip))
            return false;
        try {
            InetAddress addr = InetAddress.getByName(ip);
            if (addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress())
                return false;
        } catch (UnknownHostException e) {
            return false;
        }
        return true;
    }

}
