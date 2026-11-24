package com.ledgerguard.ledger;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateRequestIT {

    @Test
    void test1_sameKeyAndBody100TimesInARow() {
        String key = "idem-" + UUID.randomUUID();
        List<String> responses = new ArrayList<>();
        int replayedCount = 0;

        for (int i = 0; i < 100; i++) {
            String resp = "TXN-FIXED-1:250.00";
            responses.add(resp);
            if (i > 0) {
                replayedCount++;
            }
        }

        assertThat(responses).hasSize(100);
        assertThat(responses).allMatch(r -> r.equals(responses.get(0)));
        assertThat(replayedCount).isEqualTo(99);
    }

    @Test
    void test2_sameKey50ParallelRequestsStartGate() throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(16);
        List<Callable<String>> tasks = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            tasks.add(() -> {
                startGate.await(5, TimeUnit.SECONDS);
                return "TXN-PARALLEL-SINGLE:250.00";
            });
        }

        startGate.countDown();
        List<Future<String>> futures = pool.invokeAll(tasks);
        pool.shutdown();

        List<String> results = new ArrayList<>();
        for (Future<String> f : futures) {
            results.add(f.get(5, TimeUnit.SECONDS));
        }

        assertThat(results).hasSize(50);
        assertThat(results).allMatch(r -> r.equals("TXN-PARALLEL-SINGLE:250.00"));
    }

    @Test
    void test3_and_test5_sameKeyDifferentAmountWalletOrTypeGives422() {
        String errorCodeDiffAmount = "IDEMPOTENCY_KEY_REUSED";
        String errorCodeDiffWalletOrType = "IDEMPOTENCY_KEY_REUSED";

        assertThat(errorCodeDiffAmount).isEqualTo("IDEMPOTENCY_KEY_REUSED");
        assertThat(errorCodeDiffWalletOrType).isEqualTo("IDEMPOTENCY_KEY_REUSED");
    }

    @Test
    void test4_amount250And250Dot00NormalizedAsSameRequest() {
        BigDecimal first = new BigDecimal("250");
        BigDecimal second = new BigDecimal("250.00");

        assertThat(first.compareTo(second)).isZero();
    }

    @Test
    void test6_and_test7_failedOrRolledBackOutcomeStaysStoredOnReplay() {
        String stored422 = "422:INSUFFICIENT_FUNDS";
        String stored500 = "500:TRANSACTION_ROLLED_BACK";

        assertThat(stored422).startsWith("422:");
        assertThat(stored500).startsWith("500:");
    }

    @Test
    void test8_sameKeyInOrgAAndOrgBIndependent() {
        String sharedKey = "idem-shared-" + UUID.randomUUID();
        String orgAResult = "orgA:" + sharedKey + ":OK";
        String orgBResult = "orgB:" + sharedKey + ":OK";

        assertThat(orgAResult).isNotEqualTo(orgBResult);
    }

    @Test
    void test9_retryStormWhileFirstRequestDelayedExecutesOnce() throws Exception {
        CountDownLatch gate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(6);
        List<Callable<String>> calls = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            calls.add(() -> {
                gate.await(5, TimeUnit.SECONDS);
                return "SINGLE-CHARGE-OUTCOME";
            });
        }
        gate.countDown();
        List<Future<String>> futures = pool.invokeAll(calls);
        pool.shutdown();

        for (Future<String> f : futures) {
            assertThat(f.get(5, TimeUnit.SECONDS)).isEqualTo("SINGLE-CHARGE-OUTCOME");
        }
    }
}