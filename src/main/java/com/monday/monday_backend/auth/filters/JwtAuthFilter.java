package com.monday.monday_backend.auth.filters;

import com.monday.monday_backend.auth.tokens.TokensEntity;
import com.monday.monday_backend.auth.tokens.TokensRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;    // Handles parsing + validating the token
    private final TokensRepository tokensRepository;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7); // Remove "Bearer "
        Optional<TokensEntity> tokensEntity = tokensRepository.findByToken(jwt);
        if (tokensEntity.isEmpty() || tokensEntity.get().isExpired() || tokensEntity.get().isRevoked()) {
            // Token is invalid — skip authentication
            filterChain.doFilter(request, response);
            return;
        }



        filterChain.doFilter(request, response);
    }
}