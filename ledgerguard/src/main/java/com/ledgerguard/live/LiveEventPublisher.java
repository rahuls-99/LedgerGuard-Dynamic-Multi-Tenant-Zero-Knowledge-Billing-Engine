package com.ledgerguard.live;

import com.ledgerguard.multitenancy.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class LiveEventPublisher {

    private static final char[] BASE32_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final AtomicLong LAST_MS = new AtomicLong(0);
    private static final AtomicLong SEQ = new AtomicLong(0);

    private final LiveBroker broker;
    private final RedisLiveBridge redisLiveBridge;

    @Autowired
    public LiveEventPublisher(LiveBroker broker, @Autowired(required = false) RedisLiveBridge redisLiveBridge) {
        this.broker = broker;
        this.redisLiveBridge = redisLiveBridge;
    }

    public void publishFinalized(TransactionFinalizedPayload payload) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            log.warn("Skipping live event for tx={} because TenantContext is empty", payload.transactionId());
            return;
        }

        String eventId = nextMonotonicId();
        LiveEvent event = LiveEvent.tenantEvent(eventId, "transaction.finalized", tenantId, payload.toMap());

        Runnable dispatch = () -> {
            try {
                broker.publish(tenantId, event);
                if (redisLiveBridge != null) {
                    redisLiveBridge.publishRemote(tenantId, event);
                }
            } catch (Throwable ex) {
                log.warn("Live event dispatch failed for tx={} eventId={}: {}",
                        payload.transactionId(), eventId, ex.getMessage());
            }
        };

        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        dispatch.run();
                    }
                });
            } else {
                dispatch.run();
            }
        } catch (Throwable ex) {
            log.warn("Failed registering afterCommit hook for tx={}: {}", payload.transactionId(), ex.getMessage());
        }
    }

    public static String nextMonotonicId() {
        long now = Instant.now().toEpochMilli();
        long prevMs = LAST_MS.get();
        long seq;
        if (now <= prevMs) {
            now = prevMs;
            seq = SEQ.incrementAndGet();
        } else {
            LAST_MS.set(now);
            SEQ.set(0);
            seq = 0;
        }

        char[] out = new char[26];
        long t = now;
        for (int i = 9; i >= 0; i--) {
            out[i] = BASE32_ALPHABET[(int) (t & 31L)];
            t >>>= 5;
        }
        long r = (seq * 2654435761L) ^ (now * 0x9E3779B97F4A7C15L);
        for (int i = 10; i < 26; i++) {
            out[i] = BASE32_ALPHABET[(int) ((r >>> ((i - 10) * 3)) & 31L)];
            r = r * 6364136223846793005L + 1442695040888963407L;
        }
        return new String(out);
    }
}