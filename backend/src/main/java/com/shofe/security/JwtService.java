package com.shofe.security;

import com.shofe.domain.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;
    private final CustomUserDetailsService userDetailsService;

    public JwtService(
            @Value("${shofe.jwt.secret}") String secret,
            @Value("${shofe.jwt.expiration-minutes}") long expirationMinutes,
            CustomUserDetailsService userDetailsService) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
        this.userDetailsService = userDetailsService;
    }

    public String issue(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
                .signWith(key)
                .compact();
    }

    public Authentication authenticate(String token) {
        Claims claims = claims(token);
        AuthUser user = (AuthUser) userDetailsService.loadUserByUsername(claims.getSubject());
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    public boolean isValid(String token) {
        try {
            claims(token);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public UUID userId(String token) {
        return UUID.fromString(claims(token).get("uid", String.class));
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
