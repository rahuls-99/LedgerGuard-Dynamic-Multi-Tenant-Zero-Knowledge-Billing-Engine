package com.ledgerguard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        boolean success,
        String message,
        String code,
        String traceId,
        String timestamp,
        List<FieldErrorDetail> errors
) {
    public record FieldErrorDetail(String field, String message) {}

    // Backward-compatible constructor for existing 2-arg calls (e.g., AuthController)
    public ErrorResponse(boolean success, String message) {
        this(
                success,
                message,
                success ? "OK" : "UNAUTHORIZED",
                MDC.get("traceId") != null ? MDC.get("traceId") : "no-trace",
                Instant.now().toString(),
                null
        );
    }

    public static ErrorResponse of(String message, String code, String traceId) {
        return new ErrorResponse(false, message, code, traceId, Instant.now().toString(), null);
    }

    public static ErrorResponse ofValidation(String message, String traceId, List<FieldErrorDetail> errors) {
        return new ErrorResponse(false, message, "VALIDATION_ERROR", traceId, Instant.now().toString(), errors);
    }
}