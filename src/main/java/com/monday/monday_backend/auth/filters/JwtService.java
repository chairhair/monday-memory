package com.monday.monday_backend.auth.filters;

import com.monday.monday_backend.auth.dto.VerificationRequestDTO;
import com.monday.monday_backend.auth.roles.AccessLevel;
import com.monday.monday_backend.auth.roles.RolesEntity;
import com.monday.monday_backend.auth.tokens.JwtUtil;
import com.monday.monday_backend.auth.tokens.TokensEntity;
import com.monday.monday_backend.auth.tokens.TokensRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtService {

    private final JwtUtil jwtUtil;
    private final TokensRepository tokensRepository;

    public JwtService(JwtUtil jwtUtil, TokensRepository tokensRepository) {
        this.jwtUtil = jwtUtil;
        this.tokensRepository = tokensRepository;
    }

    public boolean assignToken(VerificationRequestDTO verificationRequestDTO) {
        AccessLevel accessLevel;
        try {
            accessLevel = AccessLevel.valueOf(verificationRequestDTO.role());
        } catch(IllegalArgumentException e) {
            accessLevel = AccessLevel.GUEST;
        }

        TokensEntity tokensEntity = TokensEntity.builder()
                .token(jwtUtil.generateToken(verificationRequestDTO.serviceName(), verificationRequestDTO.role()))
                .serviceName(verificationRequestDTO.serviceName())
                .accessLevel(accessLevel)
                .timeCreated(Instant.now())
                .expired(false)
                .revoked(false)
                .build();
        tokensRepository.save(tokensEntity);

        RolesEntity rolesEntity = RolesEntity.builder().build();
        return true;
    }

}
