package com.ledgerguard.lock;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class RedisLockServiceImpl implements LockService {

    private static final String RELEASE_LUA = "if redis.call(\"get\", KEYS[1]) == ARGV[1] then return redis.call(\"del\", KEYS[1]) else return 0 end";
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(RELEASE_LUA, Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisLockServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<LockHandle> tryAcquire(String resourceKey, Duration ttl, Duration maxWait) {
        long deadlineNs = System.nanoTime() + (maxWait != null ? maxWait.toNanos() : 0L);
        String token = UUID.randomUUID().toString();
        while (true) {
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(resourceKey, token, ttl);
            if (Boolean.TRUE.equals(ok)) {
                return Optional.of(new LockHandle(resourceKey, token, Instant.now(), ttl));
            }
            if (System.nanoTime() >= deadlineNs) {
                return Optional.empty();
            }
            try {
                Thread.sleep(25L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }
    }

    @Override
    public void release(LockHandle handle) {
        if (handle == null || handle.key() == null || handle.token() == null) {
            return;
        }
        List<String> keys = Collections.singletonList(handle.key());
        redisTemplate.execute(RELEASE_SCRIPT, keys, handle.token());
    }

    @Override
    public <T> T withLock(String resourceKey, Duration ttl, Duration maxWait, Supplier<T> action) {
        LockHandle handle = tryAcquire(resourceKey, ttl, maxWait)
                .orElseThrow(() -> new IllegalStateException("Could not acquire lock: " + resourceKey));
        try {
            return action.get();
        } finally {
            release(handle);
        }
    }
}
