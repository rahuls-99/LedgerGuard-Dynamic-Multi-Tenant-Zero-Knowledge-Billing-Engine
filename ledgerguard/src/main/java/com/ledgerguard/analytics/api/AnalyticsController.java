package com.ledgerguard.analytics.api;
import com.ledgerguard.analytics.AnalyticsQueryParams;
import com.ledgerguard.analytics.dto.UsageResponse;
import com.ledgerguard.analytics.service.AnalyticsUsageService;
import com.ledgerguard.analytics.service.AnalyticsUsageService.UnknownResourcesException;
import com.ledgerguard.multitenancy.TenantContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.TimeUnit;
@RestController("usageAnalyticsController")
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);
    private static final long SLOW_QUERY_THRESHOLD_MS = 100L;
    private final AnalyticsUsageService usageService;
    private final MeterRegistry meterRegistry;
    private final Clock utcClock;
    public AnalyticsController(AnalyticsUsageService usageService,
                               MeterRegistry meterRegistry,
                               Clock utcClock) {
        this.usageService = usageService;
        this.meterRegistry = meterRegistry;
        this.utcClock = utcClock;
    }
    @GetMapping("/usage")
    public ResponseEntity<?> getUsage(
            @Valid @ModelAttribute AnalyticsQueryParams params,
            @RequestParam(required = false, defaultValue = "USD") String currency
    ) {
        String tenantId = TenantContext.getTenantId();
        LocalDate todayUtc = utcClock.instant().atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate resolvedTo = (params.getTo() == null || params.getTo().isBlank())
                ? todayUtc
                : LocalDate.parse(params.getTo().trim());
        LocalDate resolvedFrom = (params.getFrom() == null || params.getFrom().isBlank())
                ? resolvedTo.minusDays(29)
                : LocalDate.parse(params.getFrom().trim());
        String resolvedGranularity = (params.getGranularity() == null || params.getGranularity().isBlank())
                ? "day"
                : params.getGranularity().trim();
        long startNanos = System.nanoTime();
        try {
            UsageResponse payload = usageService.getUsage(
                    resolvedFrom,
                    resolvedTo,
                    resolvedGranularity,
                    currency,
                    params.getResource()
            );
            long elapsedNanos = System.nanoTime() - startNanos;
            Timer.builder("analytics.usage.query")
                    .tag("tenant", tenantId != null ? tenantId : "unknown")
                    .tag("granularity", resolvedGranularity)
                    .register(meterRegistry)
                    .record(elapsedNanos, TimeUnit.NANOSECONDS);
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
            if (elapsedMs > SLOW_QUERY_THRESHOLD_MS) {
                log.warn("Slow analytics usage query: tenant={} granularity={} elapsedMs={}",
                        tenantId, resolvedGranularity, elapsedMs);
            }
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of(
                            "success", true,
                            "data", payload
                    ));
        } catch (UnknownResourcesException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of(
                            "success", false,
                            "code", "VALIDATION_ERROR",
                            "errors", ex.getErrors()
                    ));
        }
    }
}