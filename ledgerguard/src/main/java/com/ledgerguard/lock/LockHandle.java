package com.ledgerguard.lock;

import java.time.Duration;
import java.time.Instant;

public record LockHandle(
        String key,
        String token,
        Instant acquiredAt,
        Duration ttl
) {}