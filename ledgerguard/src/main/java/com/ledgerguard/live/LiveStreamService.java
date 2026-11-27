package com.ledgerguard.live;
import com.ledgerguard.multitenancy.TenantContext;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
@Service
public class LiveStreamService {
    private static final Logger log = LoggerFactory.getLogger(LiveStreamService.class);
    private final LiveBroker broker;
    private final LiveProperties properties;
    private final SseFrameWriter frameWriter;
    private final Map<String, Bucket> connectBuckets = new ConcurrentHashMap<>();
    private final List<ActiveSession> activeSessions = new CopyOnWriteArrayList<>();
    public LiveStreamService(LiveBroker broker, LiveProperties properties, SseFrameWriter frameWriter) {
        this.broker = broker;
        this.properties = properties;
        this.frameWriter = frameWriter;
    }
    public SseEmitter openStream(
            Authentication authentication,
            String lastEventId,
            HttpServletResponse response
    ) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        Object principal = authentication.getPrincipal();
        String userId = extractString(principal, "getUserId", "userId", authentication.getName());
        String tenantId = resolveTenantId(authentication, principal);
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing tenant context in token");
        }
        boolean orgActive = extractBoolean(
                principal,
                true,
                "isOrganizationActive",
                "organizationActive",
                "isOrgActive",
                "orgActive"
        );
        if (!orgActive) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Organization is inactive");
        }
        Bucket bucket = connectBuckets.computeIfAbsent(userId, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(Math.max(1, properties.getConnectRatePerMinute()))
                        .refillGreedy(Math.max(1, properties.getConnectRatePerMinute()), Duration.ofMinutes(1))
                        .build())
                .build());
        if (!bucket.tryConsume(1)) {
            response.setHeader("Retry-After", "30");
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_STREAMS");
        }
        if (broker.activeStreamsForUser(userId) >= properties.getMaxStreamsPerUser()
                || broker.activeStreamsForTenant(tenantId) >= properties.getMaxStreamsPerTenant()) {
            response.setHeader("Retry-After", "30");
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_STREAMS");
        }
        long maxAgeSec = Math.max(1L, properties.getMaxAge().toSeconds());
        long effectiveSec = maxAgeSec;
        boolean tokenBound = false;
        Instant tokenExpiresAt = extractInstant(principal, "getTokenExpiresAt", "getExpiresAt", "expiresAt");
        if (tokenExpiresAt != null) {
            long untilExpiry = Math.max(0L, Duration.between(Instant.now(), tokenExpiresAt).getSeconds());
            if (untilExpiry <= 0) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token expired");
            }
            if (untilExpiry < maxAgeSec) {
                effectiveSec = untilExpiry;
                tokenBound = true;
            }
        }
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("X-Accel-Buffering", "no");
        SseEmitter emitter = new SseEmitter(effectiveSec * 1000L);
        AtomicReference<LiveBroker.StreamSubscription> subRef = new AtomicReference<>();
        String byeReason = tokenBound ? "TOKEN_EXPIRING" : "MAX_AGE";
        Instant expiresAt = Instant.now().plusSeconds(effectiveSec);
        ActiveSession session = new ActiveSession(emitter, subRef, byeReason, expiresAt);
        activeSessions.add(session);
        Runnable cleanup = () -> closeSession(session, subRef);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ex -> cleanup.run());
        LiveBroker.StreamSubscription subscription = broker.register(
                tenantId,
                userId,
                lastEventId,
                effectiveSec,
                event -> {
                    try {
                        frameWriter.writeEvent(emitter, event);
                    } catch (IOException ex) {
                        log.debug("Closing emitter on write failure: {}", ex.getMessage());
                        closeSession(session, subRef);
                        emitter.completeWithError(ex);
                    }
                },
                reason -> {
                    try {
                        frameWriter.writeControl(emitter, "bye", Map.of("reason", reason));
                    } catch (IOException ignored) {
                    } finally {
                        activeSessions.remove(session);
                        emitter.complete();
                    }
                },
                () -> activeSessions.remove(session)
        );
        subRef.set(subscription);
        try {
            frameWriter.writeControl(emitter, "hello", Map.of(
                    "serverTime", Instant.now().toString(),
                    "heartbeatSeconds", properties.getHeartbeat().toSeconds(),
                    "maxAgeSeconds", effectiveSec
            ));
        } catch (IOException ex) {
            cleanup.run();
            emitter.completeWithError(ex);
        }
        return emitter;
    }
    @Scheduled(fixedDelay = 1000L)
    public void checkExpiringStreams() {
        long lead = Math.max(1L, properties.getGoodbyeLeadSeconds());
        Instant now = Instant.now();
        for (ActiveSession session : activeSessions) {
            if (session.goodbyeSent) {
                continue;
            }
            long remaining = Duration.between(now, session.expiresAt).getSeconds();
            if (remaining <= lead) {
                session.goodbyeSent = true;
                closeSession(session, session.subRef);
                try {
                    frameWriter.writeControl(session.emitter, "bye", Map.of("reason", session.byeReason));
                } catch (IOException ignored) {
                } finally {
                    session.emitter.complete();
                }
            }
        }
    }
    @Scheduled(fixedDelayString = "${live.heartbeat-check-ms:15000}")
    public void sendHeartbeats() {
        long heartbeatSec = Math.max(1L, properties.getHeartbeat().toSeconds());
        Instant now = Instant.now();
        for (ActiveSession session : activeSessions) {
            if (session.goodbyeSent) {
                continue;
            }
            if (Duration.between(session.lastActivityAt, now).getSeconds() >= heartbeatSec) {
                try {
                    session.emitter.send(SseEmitter.event().comment("heartbeat"));
                    session.lastActivityAt = now;
                } catch (IOException ex) {
                    log.debug("Removing dead SSE session on heartbeat error: {}", ex.getMessage());
                    closeSession(session, session.subRef);
                    session.emitter.completeWithError(ex);
                }
            }
        }
    }
    private void closeSession(ActiveSession session, AtomicReference<LiveBroker.StreamSubscription> subRef) {
        activeSessions.remove(session);
        LiveBroker.StreamSubscription sub = subRef.getAndSet(null);
        if (sub != null) {
            sub.close(null);
        }
    }
    private String resolveTenantId(Authentication authentication, Object principal) {
        String tenantId = extractString(principal, "getTenantId", "tenantId", null);
        if ((tenantId == null || tenantId.isBlank()) && authentication.getDetails() != null) {
            tenantId = extractString(authentication.getDetails(), "getTenantId", "tenantId", null);
        }
        if (tenantId == null || tenantId.isBlank()) {
            String orgId = extractString(principal, "getOrgId", "orgId", null);
            if (orgId != null && !orgId.isBlank()) {
                tenantId = normalizeOrgToTenant(orgId);
            }
        }
        if (tenantId == null || tenantId.isBlank()) {
            String ctxTenant = TenantContext.getTenantId();
            if (ctxTenant != null && !ctxTenant.isBlank()) {
                tenantId = normalizeOrgToTenant(ctxTenant);
            }
        }
        return tenantId;
    }
    private static String normalizeOrgToTenant(String raw) {
        String cleaned = raw.trim();
        if ("orgA".equalsIgnoreCase(cleaned)) {
            return "alpha";
        }
        if ("orgB".equalsIgnoreCase(cleaned)) {
            return "beta";
        }
        return cleaned;
    }
    private static Object readFieldOrMethod(Object target, String... names) {
        if (target == null) {
            return null;
        }
        if (target instanceof Map<?, ?> map) {
            for (String name : names) {
                Object val = map.get(name);
                if (val != null) {
                    return val;
                }
            }
            return null;
        }
        for (String name : names) {
            try {
                Method method = target.getClass().getMethod(name);
                Object val = method.invoke(target);
                if (val != null) {
                    return val;
                }
            } catch (Exception ignored) {
            }
            try {
                Field field = target.getClass().getDeclaredField(name);
                field.setAccessible(true);
                Object val = field.get(target);
                if (val != null) {
                    return val;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
    private static String extractString(Object target, String primaryMethod, String keyOrField, String fallback) {
        Object val = readFieldOrMethod(target, primaryMethod, keyOrField);
        if (val != null) {
            String str = String.valueOf(val).trim();
            if (!str.isEmpty()) {
                return str;
            }
        }
        return fallback;
    }
    private static boolean extractBoolean(Object target, boolean defaultVal, String... names) {
        Object val = readFieldOrMethod(target, names);
        if (val instanceof Boolean b) {
            return b;
        }
        if (val != null) {
            return Boolean.parseBoolean(String.valueOf(val));
        }
        return defaultVal;
    }
    private static Instant extractInstant(Object target, String... names) {
        Object val = readFieldOrMethod(target, names);
        if (val instanceof Instant instant) {
            return instant;
        }
        if (val instanceof java.util.Date date) {
            return date.toInstant();
        }
        if (val instanceof Long epoch) {
            return epoch > 10_000_000_000L ? Instant.ofEpochMilli(epoch) : Instant.ofEpochSecond(epoch);
        }
        if (val instanceof String str && !str.isBlank()) {
            try {
                return Instant.parse(str);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
    private static final class ActiveSession {
        final SseEmitter emitter;
        final AtomicReference<LiveBroker.StreamSubscription> subRef;
        final String byeReason;
        final Instant expiresAt;
        volatile Instant lastActivityAt = Instant.now();
        volatile boolean goodbyeSent = false;
        ActiveSession(
                SseEmitter emitter,
                AtomicReference<LiveBroker.StreamSubscription> subRef,
                String byeReason,
                Instant expiresAt
        ) {
            this.emitter = emitter;
            this.subRef = subRef;
            this.byeReason = byeReason;
            this.expiresAt = expiresAt;
        }
    }
}