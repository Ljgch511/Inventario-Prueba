package com.globalr.inventory.infrastructure.security;

import com.globalr.inventory.domain.enums.Role;
import com.globalr.inventory.domain.model.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationHours;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-hours:12}") long expirationHours
    ) {
        this.signingKey = deriveKey(secret);
        this.expirationHours = expirationHours;
    }

    public String generateToken(UserEntity user, String countryCode) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId().toString())
                .claim("fullName", user.getFullName())
                .claim("role", user.getRole().name())
                .claim("cid", user.getCountryId().toString())
                .claim("ccode", countryCode)
                .claim("mca", user.isMultiCountryAccess())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationHours, ChronoUnit.HOURS)))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Role extractRole(Claims claims) {
        return Role.valueOf(claims.get("role", String.class));
    }

    public UUID extractUuid(Claims claims, String key) {
        return UUID.fromString(claims.get(key, String.class));
    }

    private SecretKey deriveKey(String secret) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot derive JWT key", e);
        }
    }
}
