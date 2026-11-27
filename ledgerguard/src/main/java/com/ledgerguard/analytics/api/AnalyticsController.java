package com.ledgerguard.analytics.api;

import com.ledgerguard.analytics.dto.TrendResponse;
import com.ledgerguard.analytics.service.AnalyticsTrendService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@RestController("trendAnalyticsController")
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsTrendService trendService;
    private final Clock utcClock;

    public AnalyticsController(AnalyticsTrendService trendService, Clock utcClock) {
        this.trendService = trendService;
        this.utcClock = utcClock;
    }

    @GetMapping("/transactions/trend")
    public ResponseEntity<Map<String, Object>> getTransactionTrend(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String granularity,
            @RequestParam(required = false) String currency,
            @RequestParam(name = "type", required = false) List<String> types,
            @RequestHeader(name = "X-Base-Currency", required = false) String currencyHeader) {

        LocalDate todayUtc = LocalDate.ofInstant(utcClock.instant(), ZoneOffset.UTC);
        LocalDate parsedTo = (to == null || to.isBlank()) ? todayUtc : LocalDate.parse(to);
        LocalDate parsedFrom = (from == null || from.isBlank()) ? parsedTo.minusDays(29) : LocalDate.parse(from);
        String resolvedGranularity = (granularity == null || granularity.isBlank()) ? "day" : granularity;
        String effectiveCurrency = (currency != null && !currency.isBlank())
                ? currency
                : ((currencyHeader != null && !currencyHeader.isBlank()) ? currencyHeader : "USD");

        TrendResponse response = trendService.getTrend(
                parsedFrom, parsedTo, resolvedGranularity, types, effectiveCurrency);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("success", true, "data", response));
    }
}