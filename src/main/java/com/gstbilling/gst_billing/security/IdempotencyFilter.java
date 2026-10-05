package com.gstbilling.gst_billing.security;

import com.gstbilling.gst_billing.entity.IdempotencyKeyRecord;
import com.gstbilling.gst_billing.service.IdempotencyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(30)
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    public static final String X_IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    private final IdempotencyService idempotencyService;

    public IdempotencyFilter(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String key = extractIdempotencyKey(request);
        String method = request.getMethod().toUpperCase();

        // Idempotency applies to state-mutating HTTP methods
        boolean isMutating = "POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method) || "PATCH".equals(method);

        if (key == null || key.isBlank() || !isMutating) {
            filterChain.doFilter(request, response);
            return;
        }

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            // Tenant not resolved yet or public endpoint
            filterChain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletRequest cachedRequest = (request instanceof CachedBodyHttpServletRequest cbr)
                ? cbr
                : new CachedBodyHttpServletRequest(request);
        ContentCachingResponseWrapper wrappedResponse = (response instanceof ContentCachingResponseWrapper ccr)
                ? ccr
                : new ContentCachingResponseWrapper(response);

        // Read body to compute hash
        byte[] requestBody = cachedRequest.getCachedBody();
        String uri = request.getRequestURI();
        String requestHash = idempotencyService.computeRequestHash(method, uri, requestBody);

        IdempotencyService.IdempotencyCheckResult check = idempotencyService.checkOrLockKey(tenantId, key.trim(), requestHash);

        if (check.status() == IdempotencyService.IdempotencyStatus.CACHED_RESPONSE) {
            IdempotencyKeyRecord record = check.record();
            response.setStatus(record.getResponseStatus() != null ? record.getResponseStatus() : 200);
            response.setContentType(record.getResponseContentType() != null ? record.getResponseContentType() : "application/json");
            response.setHeader("X-Cache-Lookup", "HIT-IDEMPOTENT");
            response.getWriter().write(record.getResponseBody() != null ? record.getResponseBody() : "");
            return;
        }

        if (check.status() == IdempotencyService.IdempotencyStatus.IN_PROGRESS) {
            response.setStatus(HttpStatus.CONFLICT.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Conflict\",\"message\":\"A request with this Idempotency-Key is currently being processed. Please retry shortly.\"}");
            return;
        }

        if (check.status() == IdempotencyService.IdempotencyStatus.PAYLOAD_MISMATCH) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unprocessable Entity\",\"message\":\"Idempotency-Key was previously used with a different request payload.\"}");
            return;
        }

        // New key: execute chain with cached request and record response
        try {
            filterChain.doFilter(cachedRequest, wrappedResponse);

            int status = wrappedResponse.getStatus();
            byte[] responseBytes = wrappedResponse.getContentAsByteArray();
            String responseBody = new String(responseBytes, StandardCharsets.UTF_8);
            String contentType = wrappedResponse.getContentType();

            if (status < 500) {
                idempotencyService.recordResponse(tenantId, key.trim(), status, responseBody, contentType);
            } else {
                idempotencyService.releaseKey(tenantId, key.trim());
            }

            wrappedResponse.copyBodyToResponse();
        } catch (Throwable t) {
            idempotencyService.releaseKey(tenantId, key.trim());
            throw t;
        }
    }

    private String extractIdempotencyKey(HttpServletRequest request) {
        String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (key == null || key.isBlank()) {
            key = request.getHeader(X_IDEMPOTENCY_KEY_HEADER);
        }
        return key;
    }
}
