package com.ledgerguard.demo;
import com.ledgerguard.live.LiveEventPublisher;
import com.ledgerguard.live.TransactionFinalizedPayload;
import com.ledgerguard.multitenancy.TenantContext;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
@RestController
@RequestMapping("/api/dev/live")
@Profile("demo")
public class DevLiveTrigger {
    private final LiveEventPublisher publisher;
    public DevLiveTrigger(LiveEventPublisher publisher) {
        this.publisher = publisher;
    }
    @PostMapping("/test-event")
    public ResponseEntity <Object> publishTestEvent() {
        String txId = "tx-demo-" + UUID.randomUUID().toString().substring(0, 8);
        String tenantId = TenantContext.getTenantId();
        TransactionFinalizedPayload payload = TransactionFinalizedPayload.of(
                txId,
                "CHARGE",
                "SUCCESS",
                new BigDecimal("25.00"),
                "USD",
                Instant.now()
        );
        publisher.publishFinalized(payload);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("published", true);
        body.put("tenantId", tenantId != null ? tenantId : "");
        body.put("payload", payload.toMap());
        return ResponseEntity.ok(body);
    }
}