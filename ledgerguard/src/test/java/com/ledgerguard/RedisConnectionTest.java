package com.ledgerguard;

import com.ledgerguard.lock.RedisKeyBuilder;
import com.ledgerguard.multitenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RedisConnectionTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisKeyBuilder keyBuilder;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldConnectSetAndGetValue() {
        String key = "lg:test:health";
        redisTemplate.opsForValue().set(key, "ok", Duration.ofSeconds(10));
        assertEquals("ok", redisTemplate.opsForValue().get(key));
        redisTemplate.delete(key);
    }

    @Test
    void shouldScopeKeysStrictlyToTenantContext() {
        TenantContext.setTenantId("orgA");
        assertEquals("lg:orgA:lock:wallet:w_101", keyBuilder.walletLock("w_101"));
        assertEquals("lg:orgA:lock:idem:req_99", keyBuilder.idempotencyLock("req_99"));

        TenantContext.clear();
        assertThrows(IllegalStateException.class, () -> keyBuilder.walletLock("w_101"));
    }

    @Test
    void shouldFailClearlyOnWrongPassword() {
        RedisStandaloneConfiguration badConfig = new RedisStandaloneConfiguration("localhost", 6379);
        badConfig.setPassword(RedisPassword.of("wrong-password-123"));

        LettuceConnectionFactory badFactory = new LettuceConnectionFactory(badConfig);
        badFactory.afterPropertiesSet();

        try {
            assertThrows(RedisConnectionFailureException.class, () -> badFactory.getConnection().ping());
        } finally {
            badFactory.destroy();
        }
    }
}