package com.shivankkapoor.standbase.filter;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.shivankkapoor.standbase.service.IpService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.EstimationProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

// Every request carrying a Bearer token is forwarded to Aldrop's /auth/validate, which has no
// rate limit of its own. This throttles repeated invalid-token hits per IP so that garbage or
// stolen tokens can't be used to hammer Aldrop through Standbase. Only failed validations consume
// the bucket, so a legitimate session under normal load is never throttled.
public class InvalidTokenRateLimitFilter extends OncePerRequestFilter {

    private final IpService ipService;
    private final LoadingCache<String, Bucket> cache;

    public InvalidTokenRateLimitFilter(IpService ipService) {
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
        String authHeader = request.getHeader("Authorization");
        return authHeader == null || !authHeader.startsWith("Bearer ");
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
