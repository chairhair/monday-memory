package com.monday.monday_backend.auth.tokens;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.WeakKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    private final Key key;
    private final long EXPIRATION_MS;


    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expirationMs,
                   @Value("${spring.profiles.active}") String environment) {
        String currentKey = secret.replaceAll("[^A-Za-z0-9+/=]","");
        Key obtainedKey;
        try {
            byte[] decodedKey = Base64.getDecoder().decode(currentKey);
            if (decodedKey.length >= 32) {
                // If we can read the keys, we're going to use our key
                obtainedKey = Keys.hmacShaKeyFor(decodedKey);
            } else {
                if (environment.equalsIgnoreCase("prod")) {
                    throw new RuntimeException("Cannot continue; key could not be interpreted.");
                }
                // Otherwise, generate the key
                logger.warn("Cannot use JWT key");
                obtainedKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
                logger.info("Generated fallback JWT key (Base64): {}", Base64.getEncoder().encodeToString(obtainedKey.getEncoded()));
            }
        } catch(WeakKeyException | IllegalArgumentException e) {
            logger.error("Cannot parse JWT key");
            obtainedKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
            logger.info("Generated fallback JWT key (Base64): {}", Base64.getEncoder().encodeToString(obtainedKey.getEncoded()));
        }
        this.key = obtainedKey;
        this.EXPIRATION_MS = expirationMs;
    }

    public String generateToken(String serviceName, String role) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setSubject(serviceName)
                .claim("role", role)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + EXPIRATION_MS))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
