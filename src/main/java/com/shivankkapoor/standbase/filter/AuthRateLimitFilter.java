package com.shivankkapoor.standbase.filter;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.shivankkapoor.standbase.service.IpService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.EstimationProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final IpService ipService;
    private final LoadingCache<String, Bucket> cache;

    public AuthRateLimitFilter(IpService ipService) {
        this.ipService = ipService;
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .build(key -> newBucket());
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(10)
                        .refillGreedy(10, Duration.ofMinutes(15))
                        .build())
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!request.getMethod().equals("POST")) return true;
        String uri = request.getRequestURI();
        return !uri.equals("/auth/login") && !uri.equals("/auth/totp/verify");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String ip = ipService.getClientIp(request);
        Bucket bucket = cache.get(ip);

        if (bucket.getAvailableTokens() <= 0) {
            EstimationProbe probe = bucket.estimateAbilityToConsume(1);
            long waitSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(waitSeconds));
            return;
        }

        filterChain.doFilter(request, response);

        if (response.getStatus() == 401) {
            bucket.tryConsume(1);
        }
    }
}
