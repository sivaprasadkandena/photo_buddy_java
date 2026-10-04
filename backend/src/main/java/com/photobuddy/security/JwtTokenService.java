package com.photobuddy.security;

import com.photobuddy.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
    @Value("${app.jwt.secret}")
    private String encodedSecret;
    @Value("${app.jwt.access-token-minutes:15}")
    private long accessTokenMinutes;

    private SecretKey key;

    @PostConstruct
    void initialize() {
        byte[] secret = Decoders.BASE64.decode(encodedSecret);
        if (secret.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be a Base64-encoded key of at least 32 bytes");
        }
        key = Keys.hmacShaKeyFor(secret);
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("userId", user.getId())
                .claim("username", user.getUsername())
                .claim("roles", List.of("ROLE_" + user.getRole().name()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenMinutes * 60)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long getAccessTokenLifetimeSeconds() { return accessTokenMinutes * 60; }
}
