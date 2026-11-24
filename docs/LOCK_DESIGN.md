# Distributed Lock Design (LOCK_DESIGN.md)

## 1. Key Naming Convention (Strict Tenant Isolation)
All Redis lock keys MUST be built via RedisKeyBuilder using TenantContext.getTenantId():
- Wallet Lock: lg:{tenantId}:lock:wallet:{walletId}
- Idempotency Lock: lg:{tenantId}:lock:idem:{idempotencyKey}

If TenantContext is missing or blank, RedisKeyBuilder throws IllegalStateException before touching Redis.

## 2. Acquisition (SET NX PX)
- Command: SET <key> <ownerToken> NX PX <leaseTtlMs>
- Owner Token: Unique UUID per acquisition (LockHandle.ownerToken) to prevent cross-thread or post-expiry accidental release.
- Default Lease TTL: 10s (prevents deadlocks if a worker crashes mid-operation).
- Default Wait Timeout: 3s with bounded retry/backoff.

## 3. Atomic Release (Lua Script)
Only the holder matching ownerToken may delete the lock:
```lua
if redis.call("get", KEYS[1]) == ARGV[1] then
    return redis.call("del", KEYS[1])
else
    return 0
end
```
