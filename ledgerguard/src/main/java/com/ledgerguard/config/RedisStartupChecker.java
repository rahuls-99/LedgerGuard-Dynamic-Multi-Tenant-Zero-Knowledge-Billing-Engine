package com.ledgerguard.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

// @Component
public class RedisStartupChecker implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(RedisStartupChecker.class);
    private final StringRedisTemplate redisTemplate;

    public RedisStartupChecker(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            String pong = redisTemplate.execute(RedisConnection::ping);
            if (!"PONG".equalsIgnoreCase(pong)) {
                throw new IllegalStateException("Unexpected Redis PING response: " + pong);
            }
            log.info("Redis connection verified successfully (PING -> PONG)");
        } catch (Exception ex) {
            log.error("Failed to connect to Redis on startup. Ensure Redis is running and password matches.", ex);
            throw new IllegalStateException("Redis startup check failed", ex);
        }
    }
}
