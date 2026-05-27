package com.shivankkapoor.standbase.filter;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class EntryRateLimitFilter extends OncePerRequestFilter {

    private final LoadingCache<UUID, Bucket> cache;

    public EntryRateLimitFilter() {
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .build(key -> newBucket());
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(30)
                        .refillGreedy(30, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.startsWith("/entry") && !uri.startsWith("/session");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UUID userId)) {
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket = cache.get(userId);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
        } else {
            long waitSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(waitSeconds));
        }
    }
}
