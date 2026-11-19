package com.ledgerguard.service;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
@Service
public class RedisDistributedLockService {
    private record LockEntry(String token, long expiresAtMillis) {}
    private final ConcurrentHashMap<String, LockEntry> locks = new ConcurrentHashMap<>();
    public String acquireLock(String lockKey, Duration ttl) {
        long now = System.currentTimeMillis();
        long expiresAt = now + ttl.toMillis();
        String lockToken = UUID.randomUUID().toString();
        LockEntry result = locks.compute(lockKey, (key, existing) -> {
            if (existing == null || existing.expiresAtMillis() <= now) {
                return new LockEntry(lockToken, expiresAt);
            }
            return existing;
        });
        return lockToken.equals(result.token()) ? lockToken : null;
    }
    public boolean releaseLock(String lockKey, String lockToken) {
        if (lockToken == null) return false;
        return locks.computeIfPresent(lockKey, (key, existing) ->
                existing.token().equals(lockToken) ? null : existing
        ) == null;
    }
}