package com.monday.monday_backend.auth.filters;

import com.monday.monday_backend.auth.credentials.UserCredentialsEntity;
import com.monday.monday_backend.auth.credentials.UserCredentialsRepository;
import com.monday.monday_backend.auth.tokens.JwtUtil;
import com.monday.monday_backend.auth.tokens.TokensEntity;
import com.monday.monday_backend.auth.tokens.TokensRepository;
import com.monday.monday_backend.auth.users.UserEntity;
import com.monday.monday_backend.auth.users.UserRepository;
import com.monday.shared.auth.dto.VerificationRequestDTO;
import com.monday.shared.auth.dto.VerificationResponseDTO;
import com.monday.shared.auth.utils.AccessLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TokenService {

    private final JwtUtil jwtUtil;
    private final TokensRepository tokensRepository;
    private final UserRepository userRepository;

    private final UserCredentialsRepository userCredentialsRepository;

    public TokenService(JwtUtil jwtUtil, TokensRepository tokensRepository, UserRepository userRepository, UserCredentialsRepository userCredentialsRepository) {
        this.jwtUtil = jwtUtil;
        this.tokensRepository = tokensRepository;
        this.userRepository = userRepository;
        this.userCredentialsRepository = userCredentialsRepository;
    }

    /**
     * This code does the following. It will...
     * - Provide a token to guests if they currently do not have an account with the current system
     * - Provide a token if the user provides its credentials.
     * - Generate a token if the user has the correct role, but does not have the corresponding credential
     *
     * @param req
     * @return
     */
    @Transactional
    public VerificationResponseDTO assignToken(VerificationRequestDTO req, PasswordEncoder passwordEncoder) {

        AccessLevel requestedRole = parseAccessLevelOrGuest(req.requestedRole());

        // 1) Load user (if not guest)
        UserEntity user = null;
        if (!req.isGuest() && req.principalId() != null) {
            try {
                Optional<UserEntity> userOpt = userRepository.findByUserId(UUID.fromString(req.principalId()));
                if (userOpt.isEmpty()) {
                    return VerificationResponseDTO.failedDTO(HttpStatus.NOT_FOUND, "User not found or password incorrect");
                }
                user = userOpt.get();
            } catch(Exception e) {
                log.warn(e.getMessage());
                return VerificationResponseDTO.failedDTO(HttpStatus.NOT_FOUND, "User not found or password incorrect");
            }
        }

        // 2) Load/create credentials (for guest, you may still want a creds row, but be consistent)
        UserCredentialsEntity creds = null;
        if (user != null) {
            creds = userCredentialsRepository.findByUser_UserId(user.getUserId()).orElse(null);

            // 3) Verify password
            if (creds != null && (req.password() == null || creds.getPassword() == null ||
                    !passwordEncoder.matches(req.password(), creds.getPassword()))) {
                return VerificationResponseDTO.failedDTO(HttpStatus.NOT_FOUND, "Password incorrect!");
            }
        }


        // 3) Reuse active token if present
        if (creds != null && creds.getTokens() != null) {
            List<String> active = creds.getTokens().stream()
                    .filter(t -> !t.isExpired() && !t.isRevoked())
                    .filter(t -> requestedRole.equals(t.getAccessLevel()))
                    .map(TokensEntity::getToken)
                    .toList();

            if (!active.isEmpty()) {
                return VerificationResponseDTO.successfulDTO(Map.of(
                        "token", active.get(0),
                        "requestedRole", requestedRole,
                        "tokensAvailable", active
                ));
            }
        } else if (creds == null) {
            creds = new UserCredentialsEntity();
            creds.setUser(user);
            creds.setPassword(passwordEncoder.encode(req.password()));
        }

        // 5) Issue new token: subject should be principalId / userId, not sourceName
        String subject = req.isGuest()
                ? req.principalId()
                : user.getUserId().toString();

        String token = jwtUtil.generateToken(subject, requestedRole.name()); // tokenGenerator.newToken();

        TokensEntity tokenEntity = new TokensEntity();
        tokenEntity.setToken(token);
        tokenEntity.setSourceName(req.sourceName());
        tokenEntity.setAccessLevel(requestedRole);
        tokenEntity.setTimeCreated(Instant.now());
        tokenEntity.setExpired(false);
        tokenEntity.setRevoked(false);

        // 7) Attach token to creds if applicable (for guests you might store elsewhere; choose one approach)
        creds.addToken(tokenEntity);
        userCredentialsRepository.save(creds);

        return VerificationResponseDTO.successfulDTO(Map.of(
                "token", token,
                "requestedRole", requestedRole
        ));
    }

    private AccessLevel parseAccessLevelOrGuest(String role) {
        try {
            return AccessLevel.valueOf(role);
        } catch (Exception e) {
            return AccessLevel.GUEST;
        }
    }


    @Transactional
    public Map<String, Object> verify(String token) {
        TokensEntity row = tokensRepository.findByToken(token).orElseThrow(() -> new TokenInvalidException("Invalid/expired token"));
        if (row.isExpired() || row.isRevoked()) {
            throw new TokenInvalidException("Invalid/expired token");
        }

        // Fake a “claims” map from your DB row so the filter can stay generic
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", row.getId());
        claims.put("source", row.getSourceName());
        claims.put("access", row.getAccessLevel());
        claims.put("roles", List.of(row.getAccessLevel())); // e.g., USER/PRO
        UserCredentialsEntity userCreds = row.getUserCredentials();
        if (userCreds != null && userCreds.getUser() != null) {
            claims.put("userId", userCreds.getUser().getUserId());
        }
        return claims;
    }

    static final class TokenInvalidException extends RuntimeException {
        public TokenInvalidException(String msg) { super(msg); }
        public TokenInvalidException(String msg, Throwable c) { super(msg, c); }
    }

}
