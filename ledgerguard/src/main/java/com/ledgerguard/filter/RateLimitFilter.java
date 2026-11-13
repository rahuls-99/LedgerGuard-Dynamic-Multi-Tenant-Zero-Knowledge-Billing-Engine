package com.ledgerguard.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerguard.context.TenantContext;
import com.ledgerguard.dto.ErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> apiBuckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ratelimit.login.capacity:5}")
    private long loginCapacity;

    @Value("${ratelimit.login.refill-tokens:5}")
    private long loginRefillTokens;

    @Value("${ratelimit.login.refill-minutes:1}")
    private long loginRefillMinutes;

    @Value("${ratelimit.api.capacity:100}")
    private long apiCapacity;

    @Value("${ratelimit.api.refill-tokens:100}")
    private long apiRefillTokens;

    @Value("${ratelimit.api.refill-minutes:1}")
    private long apiRefillMinutes;

    @Value("${security.trust-proxy-headers:false}")
    private boolean trustProxyHeaders;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "/api/health".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean isLoginEndpoint = "/api/auth/login".equals(path) || "/api/real-auth/login".equals(path);

        Bucket bucket;
        if (isLoginEndpoint) {
            String ipKey = "login:" + resolveClientIp(request);
            bucket = loginBuckets.computeIfAbsent(ipKey, k -> createBucket(loginCapacity, loginRefillTokens, loginRefillMinutes));
        } else {
            String tenantUserKey = "api:" + resolveAuthenticatedKey(request);
            bucket = apiBuckets.computeIfAbsent(tenantUserKey, k -> createBucket(apiCapacity, apiRefillTokens, apiRefillMinutes));
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long waitForRefillSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
        String traceId = MDC.get("traceId") != null ? MDC.get("traceId") : "no-trace";

        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(waitForRefillSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = ErrorResponse.of(
                "Too many requests. Please retry after " + waitForRefillSeconds + " seconds.",
                "RATE_LIMITED",
                traceId
        );
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private Bucket createBucket(long capacity, long refillTokens, long refillMinutes) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(refillTokens, Duration.ofMinutes(refillMinutes))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveAuthenticatedKey(HttpServletRequest request) {
        // Key derived strictly from verified JWT SecurityContext / TenantContext — never from client headers
        String tenantId = TenantContext.getTenantId();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = null;

        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof Map<?, ?> map && map.get("userId") != null) {
                userId = map.get("userId").toString();
            } else if (auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
                userId = auth.getName();
            }
        }

        if (tenantId != null && userId != null) {
            return tenantId + ":" + userId;
        }
        return "unauth:" + resolveClientIp(request);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (trustProxyHeaders) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}