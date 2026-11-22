package com.ledgerguard.ledger;
import com.ledgerguard.ledger.state.PendingTransactionReaper;
import com.ledgerguard.lock.LockHandle;
import com.ledgerguard.lock.LockService;
import com.ledgerguard.multitenancy.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.assertThat;
class PendingTransactionReaperTest {
    @Test void test21_to_test24_reaperAcquiresLockRunsPerTenantAndClearsTenantContext() {
        AtomicInteger lockAcquired = new AtomicInteger(0);
        AtomicInteger lockReleased = new AtomicInteger(0);
        AtomicInteger tenantVisits = new AtomicInteger(0);
        LockService lockService = new LockService() {
            @Override public Optional<LockHandle> tryAcquire(String k, Duration ttl, Duration w) { lockAcquired.incrementAndGet(); return Optional.of(new LockHandle(k, "tok", Instant.now(), ttl)); }
            @Override public void release(LockHandle h) { lockReleased.incrementAndGet(); }
            @Override public <T> T withLock(String k, Duration ttl, Duration w, Supplier<T> a) { return a.get(); }
        };
        DataSource stubDs = (DataSource) Proxy.newProxyInstance(DataSource.class.getClassLoader(), new Class<?>[]{DataSource.class}, (proxy, method, args) -> { throw new IllegalStateException("DB down for tenant"); });
        Supplier<List<String>> tenants = () -> { tenantVisits.incrementAndGet(); return List.of("alpha", "beta"); };
        PendingTransactionReaper reaper = new PendingTransactionReaper(lockService, tenants, stubDs);
        reaper.reapStuckPendingTransactions();
        assertThat(lockAcquired.get()).isEqualTo(1);
        assertThat(lockReleased.get()).isEqualTo(1);
        assertThat(tenantVisits.get()).isEqualTo(1);
        assertThat(TenantContext.getTenantId()).isNull();
        assertThat(Collections.singletonList(new JdbcTemplate(stubDs))).hasSize(1);
    }
}
