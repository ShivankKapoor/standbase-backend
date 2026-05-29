package com.shivankkapoor.standbase.config;

import com.shivankkapoor.standbase.filter.AuthRateLimitFilter;
import com.shivankkapoor.standbase.filter.EntryRateLimitFilter;
import com.shivankkapoor.standbase.filter.SessionAuthFilter;
import com.shivankkapoor.standbase.service.IpService;
import com.shivankkapoor.standbase.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public AuthRateLimitFilter authRateLimitFilter(IpService ipService) {
        return new AuthRateLimitFilter(ipService);
    }

    @Bean
    public SessionAuthFilter sessionAuthFilter(SessionService sessionService, IpService ipService) {
        return new SessionAuthFilter(sessionService, ipService);
    }

    @Bean
    public EntryRateLimitFilter entryRateLimitFilter() {
        return new EntryRateLimitFilter();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthRateLimitFilter authRateLimitFilter,
                                           SessionAuthFilter sessionAuthFilter,
                                           EntryRateLimitFilter entryRateLimitFilter) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/auth/**", "/health","/monitor").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
            )
            .addFilterBefore(authRateLimitFilter, LogoutFilter.class)
            .addFilterBefore(sessionAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(entryRateLimitFilter, AnonymousAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public FilterRegistrationBean<AuthRateLimitFilter> authRateLimitFilterRegistration(AuthRateLimitFilter filter) {
        FilterRegistrationBean<AuthRateLimitFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<SessionAuthFilter> sessionAuthFilterRegistration(SessionAuthFilter filter) {
        FilterRegistrationBean<SessionAuthFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<EntryRateLimitFilter> entryRateLimitFilterRegistration(EntryRateLimitFilter filter) {
        FilterRegistrationBean<EntryRateLimitFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = List.of(allowedOrigins.split(","));
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new LoggingCorsConfigurationSource(source, origins);
    }

    private static class LoggingCorsConfigurationSource implements CorsConfigurationSource {
        private static final Logger log = LoggerFactory.getLogger(LoggingCorsConfigurationSource.class);
        private final CorsConfigurationSource delegate;
        private final Set<String> allowedOrigins;

        LoggingCorsConfigurationSource(CorsConfigurationSource delegate, List<String> origins) {
            this.delegate = delegate;
            this.allowedOrigins = Set.copyOf(origins);
        }

        @Override
        public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
            String origin = request.getHeader("Origin");
            if (origin != null && !allowedOrigins.contains(origin)) {
                log.warn("CORS rejected: origin '{}' not allowed — {} {}", origin, request.getMethod(), request.getRequestURI());
            }
            return delegate.getCorsConfiguration(request);
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
