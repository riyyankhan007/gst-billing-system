package com.gstbilling.gst_billing.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email, Long userId) {
        return generateToken(email, userId, null, "OWNER");
    }

    public String generateToken(String email, Long userId, String role) {
        return generateToken(email, userId, null, role);
    }

    public String generateToken(String email, Long userId, Long tenantId, String role) {
        var builder = Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", (role != null && !role.isBlank()) ? role.toUpperCase() : "OWNER");

        if (tenantId != null) {
            builder.claim("tenantId", tenantId);
        }

        return builder
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }

    public String extractRole(String token) {
        Object role = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().get("role");
        return (role != null && !role.toString().isBlank()) ? role.toString().toUpperCase() : "VIEWER";
    }

    public Long extractUserId(String token) {
        Object userId = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().get("userId");
        if (userId instanceof Number num) {
            return num.longValue();
        }
        return null;
    }

    public Long extractTenantId(String token) {
        Object tenantId = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().get("tenantId");
        if (tenantId instanceof Number num) {
            return num.longValue();
        }
        return null;
    }
}
