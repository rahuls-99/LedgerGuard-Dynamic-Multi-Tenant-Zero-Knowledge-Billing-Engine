package com.ledgerguard.controller;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemHealthController {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    public SystemHealthController(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity <Object> checkSystemHealth() {
        Map<String, Object> response = new LinkedHashMap<>();
        Map<String, String> components = new LinkedHashMap<>();

        // 1. Check MySQL Connectivity
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            components.put("mysql", (result != null && result == 1) ? "UP" : "DOWN");
        } catch (Exception e) {
            components.put("mysql", "DOWN (" + e.getMessage() + ")");
        }

        // 2. Check Redis Connectivity (Safe fallback when Redis is stopped)
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            components.put("redis", "PONG".equalsIgnoreCase(pong) ? "UP" : "DOWN");
        } catch (Exception e) {
            components.put("redis", "DOWN");
        }

        response.put("success", true);
        response.put("status", "UP".equals(components.get("mysql")) ? "HEALTHY" : "DEGRADED");
        response.put("components", components);
        response.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(response);
    }
}