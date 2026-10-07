package com.gstbilling.gst_billing.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IP-based Rate Limiter for authentication endpoints to prevent brute-force and credential stuffing attacks.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthRateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private static final long WINDOW_MS = 60_000L;

    private static class RequestTracker {
        final long windowStart;
        final AtomicInteger count;

        RequestTracker(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(1);
        }
    }

    private final Map<String, RequestTracker> ipTrackers = new ConcurrentHashMap<>();
    private volatile long lastCleanup = System.currentTimeMillis();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // Only rate-limit public authentication endpoints
        return !uri.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        cleanupIfNecessary();

        String clientIp = getClientIp(request);
        long now = System.currentTimeMillis();

        RequestTracker tracker = ipTrackers.compute(clientIp, (ip, current) -> {
            if (current == null || (now - current.windowStart) > WINDOW_MS) {
                return new RequestTracker(now);
            }
            current.count.incrementAndGet();
            return current;
        });

        if (tracker.count.get() > MAX_REQUESTS_PER_MINUTE) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"Too many authentication requests. Please try again in 1 minute.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private void cleanupIfNecessary() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup > WINDOW_MS * 5) {
            lastCleanup = now;
            ipTrackers.entrySet().removeIf(entry -> (now - entry.getValue().windowStart) > WINDOW_MS * 2);
        }
    }
}
