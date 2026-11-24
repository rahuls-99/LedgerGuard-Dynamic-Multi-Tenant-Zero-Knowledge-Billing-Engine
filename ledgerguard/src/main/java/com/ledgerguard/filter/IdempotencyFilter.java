package com.ledgerguard.filter;

import com.ledgerguard.entity.IdempotencyRecord;
import com.ledgerguard.multitenancy.TenantContext;
import com.ledgerguard.repository.IdempotencyRecordRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotencyRecordRepository idempotencyRepository;

    public IdempotencyFilter(IdempotencyRecordRepository idempotencyRepository) {
        this.idempotencyRepository = idempotencyRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().startsWith("/api/v2/ledger/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String tenantHeader = request.getHeader("X-Tenant-ID");
        if (tenantHeader == null || tenantHeader.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Missing X-Tenant-ID header\"}");
            return;
        }

        String idempotencyKey = request.getHeader("Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Missing Idempotency-Key header\"}");
            return;
        }

        TenantContext.setTenantId(tenantHeader.trim());

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 1024 * 64);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        String trimmedKey = idempotencyKey.trim();
        Optional<IdempotencyRecord> existingOpt = idempotencyRepository.findById(trimmedKey);

        if (existingOpt.isPresent()) {
            IdempotencyRecord existing = existingOpt.get();
            if ("COMPLETED".equals(existing.getStatus())) {
                response.setStatus(existing.getHttpStatus());
                response.setHeader("X-Idempotent-Replay", "true");
                response.setContentType("application/json");
                response.getWriter().write(existing.getResponseBody());
                return;
            }
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Request with this Idempotency-Key is currently processing\"}");
            return;
        }

        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(trimmedKey);
        record.setRequestHash("PENDING");
        record.setStatus("IN_PROGRESS");
        record.setCreatedAt(Instant.now());

        try {
            idempotencyRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException ex) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Concurrent duplicate Idempotency-Key\"}");
            return;
        }

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);

            TenantContext.setTenantId(tenantHeader.trim());
            String requestBody = new String(wrappedRequest.getContentAsByteArray(), StandardCharsets.UTF_8);
            String responseBody = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
            int status = wrappedResponse.getStatus();

            if (status >= 200 && status < 300) {
                record.setRequestHash(sha256(requestBody));
                record.setStatus("COMPLETED");
                record.setHttpStatus(status);
                record.setResponseBody(responseBody);
                idempotencyRepository.saveAndFlush(record);
            } else {
                idempotencyRepository.deleteById(trimmedKey);
            }

            wrappedResponse.copyBodyToResponse();
        } catch (Exception ex) {
            TenantContext.setTenantId(tenantHeader.trim());
            idempotencyRepository.deleteById(trimmedKey);
            throw ex;
        } finally {
            TenantContext.clear();
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "HASH_ERR";
        }
    }
}