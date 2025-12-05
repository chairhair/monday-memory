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

        // If no username or password is provided, we can affirm that this is a guest token and should be returned as such
        if (verificationRequestDTO.isGuest()) {
            TokensEntity tokensEntity = new TokensEntity();
            tokensEntity.setToken(jwtUtil.generateToken(verificationRequestDTO.serviceName(), "GUEST"));
            tokensEntity.setServiceName(verificationRequestDTO.serviceName());
            tokensEntity.setAccessLevel(accessLevel);
            tokensEntity.setTimeCreated(Instant.now());
            tokensEntity.setExpired(false);
            tokensEntity.setRevoked(false);
            tokensRepository.save(tokensEntity);

            return VerificationResponseDTO.successfulDTO(Map.of("token", tokensEntity.getToken(), "requestedRole", "GUEST"));
        }

        Optional<UserEntity> findUser = userRepository.findByEmail(verificationRequestDTO.email());
        if (findUser.isEmpty()) {
            return VerificationResponseDTO.failedDTO(HttpStatus.NOT_FOUND.value(), "User not found or password incorrect");
        }
        UserEntity foundUser = findUser.get();

        UserCredentialsEntity userCreds = userCredentialsRepository.findByUser_UserId(foundUser.getUserId()).orElse(null);

        List<TokensEntity> tokensEntityList = userCreds == null ? new ArrayList<>() : userCreds.getTokens();
        AccessLevel requestedRole = AccessLevel.valueOf(verificationRequestDTO.requestedRole());
        List<String> tokensAvailable = tokensEntityList.stream()
                .filter(x-> !x.isExpired() && !x.isRevoked())
                .filter(x -> requestedRole.equals(x.getAccessLevel()))
                .map(TokensEntity::getToken)
                .collect(Collectors.toList());
        boolean findRole = foundUser.getRoles().stream().anyMatch(role -> role.getAccessLevel().equals(requestedRole));
        if (!findRole && tokensAvailable.isEmpty()) {
            return VerificationResponseDTO.failedDTO(HttpStatus.NOT_FOUND.value(), "Could not find user role");
        }
        if (findRole && tokensAvailable.isEmpty()) {
            // Since we don't have a token generated, we need to generate one.
            String createToken = jwtUtil.generateToken(verificationRequestDTO.serviceName(), verificationRequestDTO.requestedRole());
            TokensEntity tokensEntity = new TokensEntity();
            tokensEntity.setToken(createToken);
            tokensEntity.setServiceName(verificationRequestDTO.serviceName());
            tokensEntity.setUserCredentials(userCreds);
            tokensEntity.setAccessLevel(accessLevel);
            tokensEntity.setTimeCreated(Instant.now());
            tokensEntity.setExpired(false);
            tokensEntity.setRevoked(false);
            tokensRepository.save(tokensEntity);
            tokensAvailable.add(createToken);
        }
        return VerificationResponseDTO.successfulDTO(Map.of(
                "token", tokensAvailable.get(0),
                "requestedRole", requestedRole,
                "tokensAvailable", tokensAvailable));
    }

    public Map<String, Object> verify(String token) {
        var row = tokensRepository.findByToken(token)
                .filter(t -> !t.isExpired() && !t.isRevoked())
                .orElseThrow(() -> new TokenInvalidException("Invalid/expired token"));

        // Fake a “claims” map from your DB row so the filter can stay generic
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", row.getServiceName());       // or user id if you store it
        claims.put("roles", List.of(row.getAccessLevel())); // e.g., USER/PRO
        return claims;
    }

    static final class TokenInvalidException extends RuntimeException {
        public TokenInvalidException(String msg) { super(msg); }
        public TokenInvalidException(String msg, Throwable c) { super(msg, c); }
    }

}
