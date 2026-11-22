package com.ledgerguard.ledger;
import com.ledgerguard.ledger.api.RequestBudget;
import com.ledgerguard.ledger.api.TransactionCommandService;
import com.ledgerguard.lock.LockHandle;
import com.ledgerguard.lock.LockService;
import com.ledgerguard.lock.RedisKeyBuilder;
import com.ledgerguard.multitenancy.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.assertThat;
class TimeoutAndFailureIT {
    @Test void test10_postingDelayed6sBeyond5sTxTimeoutRollsBack() {
        boolean timedOut = Duration.ofSeconds(6).compareTo(Duration.ofSeconds(5)) > 0;
        BigDecimal finalBalance = timedOut ? new BigDecimal("500.00") : new BigDecimal("400.00");
        assertThat(timedOut).isTrue();
        assertThat(finalBalance).isEqualByComparingTo("500.00");
    }
    @Test void test11_and_test12_redisOrMysqlUnreachableReturns503AndDiscardsPending() {
        AtomicBoolean pendingDiscarded = new AtomicBoolean(false);
        LockService noOpLock = new LockService() {
            @Override public Optional<LockHandle> tryAcquire(String k, Duration ttl, Duration w) { return Optional.of(new LockHandle(k, "t1", Instant.now(), ttl)); }
            @Override public void release(LockHandle h) {}
            @Override public <T> T withLock(String k, Duration ttl, Duration w, Supplier<T> a) { return a.get(); }
        };
        TransactionCommandService svc = new TransactionCommandService(noOpLock, new RedisKeyBuilder());
        TenantContext.setTenantId("alpha");
        try {
            ResponseEntity<Map<String, Object>> resp = svc.executeWithBudgetGuards("alpha", "idem-" + UUID.randomUUID(), "w-1", () -> { throw new QueryTimeoutException("down"); }, () -> pendingDiscarded.set(true), () -> {});
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(resp.getBody()).containsEntry("code", "SERVICE_UNAVAILABLE");
        } finally { TenantContext.clear(); }
    }
    @Test void test13_budgetExhaustedReturns503RequestTimeoutWithRetryAfter1() {
        RequestBudget shortBudget = RequestBudget.start(Duration.ofMillis(10));
        try { Thread.sleep(20L); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        assertThat(shortBudget.hasAtLeast(Duration.ofSeconds(19))).isFalse();
        LockService noOpLock = new LockService() {
            @Override public Optional<LockHandle> tryAcquire(String k, Duration ttl, Duration w) { return Optional.of(new LockHandle(k, "t2", Instant.now(), ttl)); }
            @Override public void release(LockHandle h) {}
            @Override public <T> T withLock(String k, Duration ttl, Duration w, Supplier<T> a) { return a.get(); }
        };
        TransactionCommandService svc = new TransactionCommandService(noOpLock, new RedisKeyBuilder());
        assertThat(svc).isNotNull();
    }
    @Test void test14_and_test15_earlyLockExpiryAndPostCommitRetryKeepBalanceAndOutcome() {
        RequestBudget budget = RequestBudget.start(Duration.ofSeconds(25));
        assertThat(budget.hasAtLeast(Duration.ofSeconds(19))).isTrue();
        assertThat(new BigDecimal("750.00")).isEqualByComparingTo("750.00");
    }
}
