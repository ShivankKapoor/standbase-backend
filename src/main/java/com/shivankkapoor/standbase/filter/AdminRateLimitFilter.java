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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class AdminRateLimitFilter extends OncePerRequestFilter {

    private final IpService ipService;
    private final LoadingCache<String, Bucket> cache;

    public AdminRateLimitFilter(IpService ipService) {
        this.ipService = ipService;
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(1, TimeUnit.HOURS)
                .build(key -> newBucket());
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(5)
                        .refillGreedy(5, Duration.ofHours(1))
                        .build())
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/admin/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String ip = ipService.getClientIp(request);
        Bucket bucket = cache.get(ip);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            EstimationProbe estimation = bucket.estimateAbilityToConsume(1);
            long waitSeconds = Math.max(1, estimation.getNanosToWaitForRefill() / 1_000_000_000L);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(waitSeconds));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
