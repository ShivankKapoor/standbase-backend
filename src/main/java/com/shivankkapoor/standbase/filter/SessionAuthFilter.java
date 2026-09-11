package com.shivankkapoor.standbase.filter;

import com.shivankkapoor.standbase.service.AuthService;
import com.shivankkapoor.standbase.service.IpService;
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
    public static final String SESSION_TOKEN_ATTRIBUTE = "aldropSessionToken";

    private final AuthService authService;
    private final IpService ipService;

    public SessionAuthFilter(AuthService authService, IpService ipService) {
        this.authService = authService;
        this.ipService = ipService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String ip = ipService.getClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            UUID userId = authService.getSessionUserID(token, ip, userAgent);
            if (userId != null) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId, null, List.of()
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
                request.setAttribute(SESSION_TOKEN_ATTRIBUTE, token);
            }
        }
        filterChain.doFilter(request, response);
    }
}
