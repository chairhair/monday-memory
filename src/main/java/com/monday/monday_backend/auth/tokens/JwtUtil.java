package com.monday.monday_backend.auth.tokens;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;

import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {
    private final Key key;
    private final long EXPIRATION_MS;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("{jwt.expiration}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.EXPIRATION_MS = expirationMs;
    }
}
