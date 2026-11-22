package com.ledgerguard.lock;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public interface LockService {
    Optional<LockHandle> tryAcquire(String resourceKey, Duration ttl, Duration maxWait);
    void release(LockHandle handle);
    <T> T withLock(String resourceKey, Duration ttl, Duration maxWait, Supplier<T> action);
}