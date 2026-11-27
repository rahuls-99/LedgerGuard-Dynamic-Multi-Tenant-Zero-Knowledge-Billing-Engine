package com.ledgerguard.live;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Component
public class SseFrameWriter {

    private final ObjectMapper objectMapper;

    public SseFrameWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void writeEvent(SseEmitter emitter, LiveEvent event) throws IOException {
        String json = toCompactJson(event.data());
        SseEmitter.SseEventBuilder builder = SseEmitter.event()
                .name(event.type())
                .data(json);
        if (event.id() != null && !event.id().isBlank()) {
            builder.id(event.id());
        }
        emitter.send(builder);
    }

    public void writeControl(SseEmitter emitter, String eventType, Map<String, Object> payload) throws IOException {
        emitter.send(SseEmitter.event()
                .name(eventType)
                .data(toCompactJson(payload)));
    }

    private String toCompactJson(Map<String, Object> map) throws IOException {
        try {
            return objectMapper.writeValueAsString(map == null ? Map.of() : map);
        } catch (JsonProcessingException ex) {
            throw new IOException("Failed to serialize SSE frame payload", ex);
        }
    }
}