package com.ledgerguard.analytics.controller;

import com.ledgerguard.analytics.AnalyticsQueryParams;
import com.ledgerguard.dto.SummaryResponse;
import com.ledgerguard.analytics.service.AnalyticsSummaryService;
import com.ledgerguard.multitenancy.TenantContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final long SLOW_QUERY_WARN_MS = 100L;

    private final AnalyticsSummaryService summaryService;
    private final Timer summaryTimer;

    public AnalyticsController(AnalyticsSummaryService summaryService, MeterRegistry meterRegistry) {
        this.summaryService = summaryService;
        this.summaryTimer = Timer.builder("analytics.summary.query")
                .description("Execution time for GET /api/analytics/summary")
                .register(meterRegistry);
    }

    @GetMapping("/summary")
    public ResponseEntity<SummaryResponse> getSummary(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader,
            @RequestHeader(value = "X-Base-Currency", required = false, defaultValue = "INR") String baseCurrency,
            @Valid @ModelAttribute AnalyticsQueryParams params
    ) {
        long startNanos = System.nanoTime();

        String resolvedTenant = (TenantContext.getTenantId() != null && !TenantContext.getTenantId().isBlank())
                ? TenantContext.getTenantId()
                : ((tenantHeader != null && !tenantHeader.isBlank()) ? tenantHeader.trim() : "default");

        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        LocalDate toDate = (params != null && params.getTo() != null && !params.getTo().isBlank())
                ? LocalDate.parse(params.getTo())
                : todayUtc;
        LocalDate fromDate = (params != null && params.getFrom() != null && !params.getFrom().isBlank())
                ? LocalDate.parse(params.getFrom())
                : toDate.minusDays(29);

        SummaryResponse response = summaryService.getSummary(
                resolvedTenant,
                baseCurrency,
                fromDate,
                toDate
        );

        long elapsedNanos = System.nanoTime() - startNanos;
        summaryTimer.record(elapsedNanos, TimeUnit.NANOSECONDS);

        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
        if (elapsedMs > SLOW_QUERY_WARN_MS) {
            log.warn("Slow analytics summary query: tenant={}, elapsedMs={}, from={}, to={}",
                    resolvedTenant, elapsedMs, fromDate, toDate);
        }

        return ResponseEntity.ok(response);
    }
}
