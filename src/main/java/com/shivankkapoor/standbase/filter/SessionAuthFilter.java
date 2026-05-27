package com.shivankkapoor.standbase.filter;

import com.shivankkapoor.standbase.service.IpService;
import com.shivankkapoor.standbase.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class SessionAuthFilter extends OncePerRequestFilter {
    private final SessionService sessionService;
    private final IpService ipService;

    public SessionAuthFilter(SessionService sessionService, IpService ipService) {
        this.sessionService = sessionService;
        this.ipService = ipService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String ip = ipService.getClientIp(request);
            UUID userId = sessionService.getSessionUserID(token, ip);
            if (userId != null) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId, null, List.of()
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }
}
