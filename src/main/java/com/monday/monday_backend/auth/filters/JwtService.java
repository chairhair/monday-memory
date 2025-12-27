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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final JwtUtil jwtUtil;
    private final TokensRepository tokensRepository;
    private final UserRepository userRepository;
    private final UserCredentialsRepository userCredentialsRepository;

    public JwtService(JwtUtil jwtUtil, TokensRepository tokensRepository, UserRepository userRepository, UserCredentialsRepository userCredentialsRepository) {
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
     * @param verificationRequestDTO
     * @return
     */
    public VerificationResponseDTO assignToken(VerificationRequestDTO verificationRequestDTO) {
        AccessLevel accessLevel;
        try {
            accessLevel = AccessLevel.valueOf(verificationRequestDTO.requestedRole());
        } catch (IllegalArgumentException e) {
            accessLevel = AccessLevel.GUEST;
        }

        Optional<UserEntity> findUser = userRepository.findByEmail(verificationRequestDTO.email());
        if (findUser.isEmpty()) {
            return VerificationResponseDTO.failedDTO(HttpStatus.NOT_FOUND, "User not found or password incorrect");
        }
        UserEntity foundUser = findUser.get();

        UserCredentialsEntity userCreds = userCredentialsRepository.findByUser_UserId(foundUser.getUserId()).orElse(null);
        if (userCreds == null) {
            userCreds = new UserCredentialsEntity();
            userCreds.setUser(foundUser);
            userCreds.setPassword(verificationRequestDTO.password());
        }

        // If no username or password is provided, we can affirm that this is a guest token and should be returned as such
        if (verificationRequestDTO.isGuest()) {
            TokensEntity tokensEntity = new TokensEntity();
            tokensEntity.setToken(jwtUtil.generateToken(verificationRequestDTO.principalId(), "GUEST"));
            tokensEntity.setSourceName(verificationRequestDTO.sourceName());
            tokensEntity.setAccessLevel(accessLevel);
            tokensEntity.setTimeCreated(Instant.now());
            tokensEntity.setExpired(false);
            tokensEntity.setRevoked(false);
            tokensRepository.save(tokensEntity);

            userCreds.addToken(tokensEntity);
            userCredentialsRepository.save(userCreds);

            return VerificationResponseDTO.successfulDTO(Map.of("token", tokensEntity.getToken(), "requestedRole", "GUEST"));
        }

        List<TokensEntity> tokensEntityList = userCreds == null || userCreds.getTokens() == null ? new ArrayList<>() : userCreds.getTokens();
        AccessLevel requestedRole = AccessLevel.valueOf(verificationRequestDTO.requestedRole());
        List<String> tokensAvailable = tokensEntityList.stream()
                .filter(x-> !x.isExpired() && !x.isRevoked())
                .filter(x -> requestedRole.equals(x.getAccessLevel()))
                .map(TokensEntity::getToken)
                .collect(Collectors.toList());
        boolean findRole = foundUser.getRoles().stream().anyMatch(role -> role.getAccessLevel().equals(requestedRole));
        if (!findRole && tokensAvailable.isEmpty()) {
            return VerificationResponseDTO.failedDTO(HttpStatus.NOT_FOUND, "Could not find user role");
        }
        if (findRole && tokensAvailable.isEmpty()) {
            // Since we don't have a token generated, we need to generate one.
            String createToken = jwtUtil.generateToken(verificationRequestDTO.sourceName(), verificationRequestDTO.requestedRole());
            TokensEntity tokensEntity = new TokensEntity();
            tokensEntity.setToken(createToken);
            tokensEntity.setSourceName(verificationRequestDTO.sourceName());
            tokensEntity.setUserCredentials(userCreds);
            tokensEntity.setAccessLevel(accessLevel);
            tokensEntity.setTimeCreated(Instant.now());
            tokensEntity.setExpired(false);
            tokensEntity.setRevoked(false);

            userCreds.addToken(tokensEntity);
            tokensRepository.save(tokensEntity);
            userCredentialsRepository.save(userCreds);

            tokensAvailable.add(createToken);
        }
        return VerificationResponseDTO.successfulDTO(Map.of(
                "token", tokensAvailable.get(0),
                "requestedRole", requestedRole,
                "tokensAvailable", tokensAvailable));
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
