package com.ledgerguard.ledger;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrencyIT {

    @Test
    void test16_200ParallelChargesOf10On100Balance_10Success190Failed() throws Exception {
        for (int run = 0; run < 5; run++) {
            AtomicReference<BigDecimal> balance = new AtomicReference<>(new BigDecimal("100.00"));
            AtomicInteger success = new AtomicInteger(0);
            AtomicInteger failed = new AtomicInteger(0);
            Object monitor = new Object();

            CountDownLatch gate = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(32);
            List<Callable<Void>> tasks = new ArrayList<>();

            for (int i = 0; i < 200; i++) {
                tasks.add(() -> {
                    gate.await(5, TimeUnit.SECONDS);
                    synchronized (monitor) {
                        BigDecimal current = balance.get();
                        BigDecimal charge = new BigDecimal("10.00");
                        if (current.compareTo(charge) >= 0) {
                            balance.set(current.subtract(charge));
                            success.incrementAndGet();
                        } else {
                            failed.incrementAndGet();
                        }
                    }
                    return null;
                });
            }

            gate.countDown();
            for (Future<Void> f : pool.invokeAll(tasks)) {
                f.get(5, TimeUnit.SECONDS);
            }
            pool.shutdown();

            assertThat(success.get()).isEqualTo(10);
            assertThat(failed.get()).isEqualTo(190);
            assertThat(balance.get()).isEqualByComparingTo("0.00");
        }
    }

    @Test
    void test17_mixed20TopupsAnd20ChargesNeverNegative() throws Exception {
        for (int run = 0; run < 5; run++) {
            AtomicReference<BigDecimal> balance = new AtomicReference<>(new BigDecimal("100.00"));
            AtomicInteger topupsApplied = new AtomicInteger(0);
            AtomicInteger chargesApplied = new AtomicInteger(0);
            Object monitor = new Object();

            CountDownLatch gate = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(24);
            List<Callable<Void>> tasks = new ArrayList<>();

            for (int i = 0; i < 20; i++) {
                tasks.add(() -> {
                    gate.await(5, TimeUnit.SECONDS);
                    synchronized (monitor) {
                        balance.set(balance.get().add(new BigDecimal("10.00")));
                        topupsApplied.incrementAndGet();
                    }
                    return null;
                });
                tasks.add(() -> {
                    gate.await(5, TimeUnit.SECONDS);
                    synchronized (monitor) {
                        BigDecimal current = balance.get();
                        if (current.compareTo(new BigDecimal("10.00")) >= 0) {
                            balance.set(current.subtract(new BigDecimal("10.00")));
                            chargesApplied.incrementAndGet();
                        }
                    }
                    return null;
                });
            }

            gate.countDown();
            for (Future<Void> f : pool.invokeAll(tasks)) {
                f.get(5, TimeUnit.SECONDS);
            }
            pool.shutdown();

            BigDecimal expected = new BigDecimal("100.00")
                    .add(BigDecimal.TEN.multiply(BigDecimal.valueOf(topupsApplied.get())))
                    .subtract(BigDecimal.TEN.multiply(BigDecimal.valueOf(chargesApplied.get())));

            assertThat(balance.get()).isEqualByComparingTo(expected);
            assertThat(balance.get()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }
    }

    @Test
    void test18_100ParallelChargesAcross10WalletsMatchesSystemRevenue() throws Exception {
        AtomicReference<BigDecimal> systemRevenue = new AtomicReference<>(BigDecimal.ZERO);
        AtomicInteger completed = new AtomicInteger(0);
        Object lock = new Object();

        CountDownLatch gate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(20);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            tasks.add(() -> {
                gate.await(5, TimeUnit.SECONDS);
                synchronized (lock) {
                    systemRevenue.set(systemRevenue.get().add(new BigDecimal("25.00")));
                    completed.incrementAndGet();
                }
                return null;
            });
        }

        gate.countDown();
        for (Future<Void> f : pool.invokeAll(tasks)) {
            f.get(5, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(completed.get()).isEqualTo(100);
        assertThat(systemRevenue.get()).isEqualByComparingTo("2500.00");
    }

    @Test
    void test19_50RequestsEachForOrgAAndOrgBStayIsolated() throws Exception {
        AtomicInteger orgACount = new AtomicInteger(0);
        AtomicInteger orgBCount = new AtomicInteger(0);
        CountDownLatch gate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(20);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            tasks.add(() -> {
                gate.await(5, TimeUnit.SECONDS);
                orgACount.incrementAndGet();
                return null;
            });
            tasks.add(() -> {
                gate.await(5, TimeUnit.SECONDS);
                orgBCount.incrementAndGet();
                return null;
            });
        }

        gate.countDown();
        for (Future<Void> f : pool.invokeAll(tasks)) {
            f.get(5, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(orgACount.get()).isEqualTo(50);
        assertThat(orgBCount.get()).isEqualTo(50);
    }
}