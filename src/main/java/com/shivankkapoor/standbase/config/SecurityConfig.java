package com.shivankkapoor.standbase.config;

import com.shivankkapoor.standbase.filter.AuthRateLimitFilter;
import com.shivankkapoor.standbase.filter.EntryRateLimitFilter;
import com.shivankkapoor.standbase.filter.SessionAuthFilter;
import com.shivankkapoor.standbase.service.IpService;
import com.shivankkapoor.standbase.service.SessionService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.http.HttpServletResponse;
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
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/auth/**").permitAll()
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
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
