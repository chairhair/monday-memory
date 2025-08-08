package com.monday.monday_backend.auth.filters;

import com.monday.monday_backend.auth.dto.VerificationRequestDTO;
import com.monday.monday_backend.auth.dto.VerificationResponseDTO;
import com.monday.monday_backend.auth.roles.AccessLevel;
import com.monday.monday_backend.auth.roles.RolesEntity;
import com.monday.monday_backend.auth.tokens.JwtUtil;
import com.monday.monday_backend.auth.tokens.TokensEntity;
import com.monday.monday_backend.auth.tokens.TokensRepository;
import com.monday.monday_backend.auth.users.UserEntity;
import com.monday.monday_backend.auth.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final JwtUtil jwtUtil;
    private final TokensRepository tokensRepository;
    private final UserRepository userRepository;

    public JwtService(JwtUtil jwtUtil, TokensRepository tokensRepository, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.tokensRepository = tokensRepository;
        this.userRepository = userRepository;
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

        Optional<UserEntity> findUser = userRepository.findByEmailAndPassword(verificationRequestDTO.email(), verificationRequestDTO.password());
        if (findUser.isEmpty()) {
            return VerificationResponseDTO.failedDTO(HttpStatus.NOT_FOUND.value(), "User not found or password incorrect");
        }
        UserEntity foundUser = findUser.get();
        List<TokensEntity> tokensEntityList = foundUser.getTokensEntity();
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
            tokensEntity.setUser(foundUser);
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

}
