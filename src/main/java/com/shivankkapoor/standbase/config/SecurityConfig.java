package com.shivankkapoor.standbase.config;

import com.shivankkapoor.standbase.filter.AuthRateLimitFilter;
import com.shivankkapoor.standbase.filter.EntryRateLimitFilter;
import com.shivankkapoor.standbase.filter.SessionAuthFilter;
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

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final SessionAuthFilter sessionAuthFilter;
    private final AuthRateLimitFilter authRateLimitFilter;
    private final EntryRateLimitFilter entryRateLimitFilter;

    public SecurityConfig(SessionAuthFilter sessionAuthFilter,
                          AuthRateLimitFilter authRateLimitFilter,
                          EntryRateLimitFilter entryRateLimitFilter) {
        this.sessionAuthFilter = sessionAuthFilter;
        this.authRateLimitFilter = authRateLimitFilter;
        this.entryRateLimitFilter = entryRateLimitFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(authRateLimitFilter, LogoutFilter.class)
            .addFilterBefore(sessionAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(entryRateLimitFilter, AnonymousAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
