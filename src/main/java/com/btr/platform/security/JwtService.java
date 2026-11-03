package com.btr.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessHours;
    private final long refreshDays;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-hours:2}") long accessHours,
                      @Value("${app.jwt.refresh-days:7}") long refreshDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessHours = accessHours;
        this.refreshDays = refreshDays;
    }

    public String createAccessToken(UserPrincipal user) {
        return create(user, "access", Instant.now().plus(accessHours, ChronoUnit.HOURS));
    }

    public String createRefreshToken(UserPrincipal user) {
        return create(user, "refresh", Instant.now().plus(refreshDays, ChronoUnit.DAYS));
    }

    private String create(UserPrincipal user, String type, Instant expireAt) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(user.id()))
                .claim("account", user.account())
                .claim("nickname", user.nickname())
                .claim("role", user.role())
                .claim("type", type)
                .issuedAt(new Date())
                .expiration(Date.from(expireAt))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public UserPrincipal toPrincipal(Claims claims) {
        return new UserPrincipal(
                Long.valueOf(claims.getSubject()),
                claims.get("account", String.class),
                claims.get("nickname", String.class),
                claims.get("role", String.class)
        );
    }

    public long remainingSeconds(Claims claims) {
        Date expiration = claims.getExpiration();
        return Math.max(1, (expiration.getTime() - System.currentTimeMillis()) / 1000);
    }

    public long getRefreshDays() {
        return refreshDays;
    }
}
