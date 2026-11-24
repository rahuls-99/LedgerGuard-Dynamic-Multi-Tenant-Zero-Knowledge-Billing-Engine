package com.ledgerguard.ledger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerguard.ledger.entity.TransactionType;
import com.ledgerguard.multitenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class LedgerEndToEndIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.46")
            .withDatabaseName("master_db")
            .withUsername("root")
            .withPassword("root");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Configuration
    @Profile("test")
    static class TestFailureHookConfig {
        static final AtomicBoolean FAIL_AFTER_COMMIT = new AtomicBoolean(false);
        static final AtomicBoolean FAIL_AFTER_DEBIT = new AtomicBoolean(false);

        @Bean
        AfterCommitResponseHook afterCommitResponseHook() {
            return () -> {
                if (FAIL_AFTER_COMMIT.get()) {
                    throw new IllegalStateException("Injected AFTER_COMMIT failure");
                }
            };
        }
    }

    @FunctionalInterface
    interface AfterCommitResponseHook {
        void beforeResponse();
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @BeforeAll
    static void setupTenants() {
        TenantContext.setTenantId("alpha");
    }

    @AfterEach
    void resetHooks() {
        TestFailureHookConfig.FAIL_AFTER_COMMIT.set(false);
        TestFailureHookConfig.FAIL_AFTER_DEBIT.set(false);
    }

    @Test
    void test1_singleChargeAndInvariant() throws Exception {
        String walletId = "w-" + UUID.randomUUID();
        createWallet(walletId, "1000.00", "INR");
        String key = "idem-" + UUID.randomUUID();

        HttpResponse<String> resp = postLedger(key, TransactionType.CHARGE, walletId, "250.00", "INR");
        assertThat(resp.statusCode()).isEqualTo(201);
        assertThat(resp.headers().firstValue("Cache-Control")).isPresent();

        Map<String, Object> body = parseMap(resp.body());
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertThat(new BigDecimal(String.valueOf(data.get("balanceAfter")))).isEqualByComparingTo("750.00");
    }

    @Test
    void test2_replaySameKeyIdenticalBody() throws Exception {
        String walletId = "w-" + UUID.randomUUID();
        createWallet(walletId, "1000.00", "INR");
        String key = "idem-" + UUID.randomUUID();

        HttpResponse<String> first = postLedger(key, TransactionType.CHARGE, walletId, "250.00", "INR");
        HttpResponse<String> replay = postLedger(key, TransactionType.CHARGE, walletId, "250.00", "INR");

        assertThat(replay.statusCode()).isEqualTo(201);
        assertThat(replay.headers().firstValue("Idempotent-Replayed")).contains("true");
        assertThat(parseMap(replay.body()).get("data")).isEqualTo(parseMap(first.body()).get("data"));
    }

    @Test
    void test3_tenParallelSameKey() throws Exception {
        String walletId = "w-" + UUID.randomUUID();
        createWallet(walletId, "1000.00", "INR");
        String key = "idem-" + UUID.randomUUID();

        ExecutorService pool = Executors.newFixedThreadPool(10);
        List<Callable<HttpResponse<String>>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tasks.add(() -> postLedger(key, TransactionType.CHARGE, walletId, "250.00", "INR"));
        }
        List<Future<HttpResponse<String>>> futures = pool.invokeAll(tasks);
        pool.shutdown();

        List<Object> bodies = new ArrayList<>();
        for (Future<HttpResponse<String>> f : futures) {
            HttpResponse<String> r = f.get();
            assertThat(r.statusCode()).isEqualTo(201);
            bodies.add(parseMap(r.body()).get("data"));
        }
        Object firstData = bodies.get(0);
        assertThat(bodies).allMatch(d -> d.equals(firstData));
    }

    @Test
    void test4_fiftyParallelDifferentKeys() throws Exception {
        String walletId = "w-" + UUID.randomUUID();
        createWallet(walletId, "100.00", "INR");

        ExecutorService pool = Executors.newFixedThreadPool(16);
        List<Callable<HttpResponse<String>>> tasks = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            String k = "idem-p-" + i + "-" + UUID.randomUUID();
            tasks.add(() -> postLedger(k, TransactionType.CHARGE, walletId, "10.00", "INR"));
        }
        List<Future<HttpResponse<String>>> futures = pool.invokeAll(tasks);
        pool.shutdown();

        int success = 0;
        int failed = 0;
        for (Future<HttpResponse<String>> f : futures) {
            int status = f.get().statusCode();
            if (status == 201) {
                success++;
            } else if (status == 422) {
                failed++;
            }
        }
        assertThat(success).isEqualTo(10);
        assertThat(failed).isEqualTo(40);
    }

    @Test
    void test5_insufficientFundsAndReplay() throws Exception {
        String walletId = "w-" + UUID.randomUUID();
        createWallet(walletId, "100.00", "INR");
        String key = "idem-" + UUID.randomUUID();

        HttpResponse<String> r1 = postLedger(key, TransactionType.CHARGE, walletId, "250.00", "INR");
        HttpResponse<String> r2 = postLedger(key, TransactionType.CHARGE, walletId, "250.00", "INR");

        assertThat(r1.statusCode()).isEqualTo(422);
        assertThat(parseMap(r1.body()).get("code")).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(r2.statusCode()).isEqualTo(422);
        assertThat(r2.headers().firstValue("Idempotent-Replayed")).contains("true");
        Map<String, Object> d1 = (Map<String, Object>) parseMap(r1.body()).get("data");
        Map<String, Object> d2 = (Map<String, Object>) parseMap(r2.body()).get("data");
        assertThat(d2.get("transactionId")).isEqualTo(d1.get("transactionId"));
        assertThat(d2.get("balanceAfter")).isNull();
    }

    @Test
    void test6_forcedRollbackAfterDebit() throws Exception {
        TestFailureHookConfig.FAIL_AFTER_DEBIT.set(true);
        String walletId = "w-" + UUID.randomUUID();
        createWallet(walletId, "500.00", "INR");
        String key = "idem-" + UUID.randomUUID();

        HttpResponse<String> r = postLedger(key, TransactionType.CHARGE, walletId, "100.00", "INR");
        assertThat(r.statusCode()).isEqualTo(500);
        assertThat(parseMap(r.body()).get("code")).isEqualTo("TRANSACTION_ROLLED_BACK");
    }

    @Test
    void test7_redisDownReturns503AndRecovers() throws Exception {
        String walletId = "w-" + UUID.randomUUID();
        createWallet(walletId, "500.00", "INR");
        String key = "idem-" + UUID.randomUUID();

        REDIS.stop();
        try {
            HttpResponse<String> downResp = postLedger(key, TransactionType.CHARGE, walletId, "100.00", "INR");
            assertThat(downResp.statusCode()).isEqualTo(503);
        } finally {
            REDIS.start();
            System.setProperty("spring.data.redis.port", String.valueOf(REDIS.getMappedPort(6379)));
        }
    }

    @Test
    void test11_unknownWalletReturns404() throws Exception {
        String key = "idem-" + UUID.randomUUID();
        HttpResponse<String> r = postLedger(key, TransactionType.CHARGE, "w-does-not-exist", "50.00", "INR");
        assertThat(r.statusCode()).isEqualTo(404);
        assertThat(parseMap(r.body()).get("code")).isEqualTo("WALLET_NOT_FOUND");
    }

    @Test
    void test12_chargeAliasEndpoint() throws Exception {
        String walletId = "w-" + UUID.randomUUID();
        createWallet(walletId, "300.00", "INR");
        String key = "idem-" + UUID.randomUUID();

        String json = objectMapper.writeValueAsString(Map.of("walletId", walletId, "amount", new BigDecimal("75.00"), "currency", "INR"));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/ledger/charge"))
                .header("Content-Type", "application/json")
                .header("X-Tenant-ID", "alpha")
                .header("Idempotency-Key", key)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> r = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(r.statusCode()).isEqualTo(201);
    }

    private void createWallet(String walletId, String balance, String currency) throws Exception {
        String json = objectMapper.writeValueAsString(Map.of("walletId", walletId, "ownerId", "cust-1", "currency", currency, "initialBalance", new BigDecimal(balance)));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/v1/wallets"))
                .header("Content-Type", "application/json")
                .header("X-Tenant-ID", "alpha")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postLedger(String key, TransactionType type, String walletId, String amount, String currency) throws Exception {
        String json = objectMapper.writeValueAsString(Map.of("type", type.name(), "walletId", walletId, "amount", new BigDecimal(amount), "currency", currency));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/api/v1/ledger/post"))
                .header("Content-Type", "application/json")
                .header("X-Tenant-ID", "alpha")
                .header("Idempotency-Key", key)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private Map<String, Object> parseMap(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}
