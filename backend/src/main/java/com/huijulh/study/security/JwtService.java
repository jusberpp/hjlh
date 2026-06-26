package com.huijulh.study.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

@Service
public class JwtService {
    private final SecretKey key;
    private final Duration expiration;

    public JwtService(
            @Value("${study.security.jwt-secret}") String secret,
            @Value("${study.security.jwt-expiration}") Duration expiration
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String issue(AdminPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(Long.toString(principal.userId()))
                .claim("username", principal.username())
                .claim("displayName", principal.displayName())
                .claim("orgId", principal.orgId())
                .claim("permissions", String.join(",", principal.permissions()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    public AdminPrincipal parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        String rawPermissions = claims.get("permissions", String.class);
        return new AdminPrincipal(
                Long.parseLong(claims.getSubject()),
                claims.get("username", String.class),
                claims.get("displayName", String.class),
                claims.get("orgId", Long.class),
                rawPermissions == null || rawPermissions.isBlank()
                        ? new HashSet<>()
                        : new HashSet<>(Arrays.asList(rawPermissions.split(",")))
        );
    }

    public long expirationSeconds() {
        return expiration.toSeconds();
    }
}
