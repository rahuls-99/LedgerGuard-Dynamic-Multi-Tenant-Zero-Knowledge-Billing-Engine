package com.ledgerguard.live;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Component
@ConditionalOnBean({StringRedisTemplate.class, RedisMessageListenerContainer.class})
public class RedisLiveBridge implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisLiveBridge.class);

    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer listenerContainer;
    private final LiveBroker liveBroker;
    private final LiveProperties properties;
    private final ObjectMapper objectMapper;
    private final String instanceId = UUID.randomUUID().toString();
    private final AtomicLong lastWarnEpochSec = new AtomicLong(0);

    @Autowired
    public RedisLiveBridge(
            StringRedisTemplate redisTemplate,
            RedisMessageListenerContainer listenerContainer,
            LiveBroker liveBroker,
            LiveProperties properties,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.listenerContainer = listenerContainer;
        this.liveBroker = liveBroker;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void subscribe() {
        String pattern = properties.getRedisChannelPrefix() + "*";
        listenerContainer.addMessageListener(this, new PatternTopic(pattern));
    }

    public void publishRemote(String tenantId, LiveEvent event) {
        try {
            String channel = properties.getRedisChannelPrefix() + tenantId;
            Map<String, Object> envelope = Map.of(
                    "originInstanceId", instanceId,
                    "tenantId", tenantId,
                    "event", event
            );
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(envelope));
        } catch (Exception ex) {
            warnRateLimited("Failed publishing SSE event to Redis channel for tenant=" + tenantId, ex);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onMessage(Message message, byte[] pattern) {
        try {
            String raw = new String(message.getBody(), StandardCharsets.UTF_8);
            Map<String, Object> envelope = objectMapper.readValue(raw, Map.class);
            Object origin = envelope.get("originInstanceId");
            if (instanceId.equals(origin)) {
                return;
            }
            String tenantId = String.valueOf(envelope.get("tenantId"));
            Map<String, Object> eventMap = (Map<String, Object>) envelope.get("event");
            LiveEvent event = new LiveEvent(
                    (String) eventMap.get("id"),
                    (String) eventMap.get("type"),
                    tenantId,
                    eventMap.get("timestamp") != null ? Instant.parse(String.valueOf(eventMap.get("timestamp"))) : Instant.now(),
                    (Map<String, Object>) eventMap.get("data")
            );
            liveBroker.publishFromRemote(tenantId, event);
        } catch (Exception ex) {
            warnRateLimited("Failed consuming SSE event from Redis pub/sub", ex);
        }
    }

    private void warnRateLimited(String msg, Exception ex) {
        long now = Instant.now().getEpochSecond();
        long prev = lastWarnEpochSec.get();
        if (now - prev >= 10 && lastWarnEpochSec.compareAndSet(prev, now)) {
            log.warn("{}: {}", msg, ex.getMessage());
        }
    }
}