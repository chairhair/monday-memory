package com.monday.monday_backend.auth.filters;

import com.monday.monday_backend.auth.credentials.UserCredentialsEntity;
import com.monday.monday_backend.auth.credentials.UserCredentialsRepository;
import com.monday.monday_backend.auth.principal.AuthUser;
import com.monday.monday_backend.auth.tokens.TokensEntity;
import com.monday.monday_backend.auth.tokens.TokensRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.monday.monday_backend.auth.filters.JwtAuthHelper.*;

/**
 * This handles our JWT authentication.
 */
@ConditionalOnProperty(name = "app.security.jwt.enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;    // Handles parsing + validating the token
    private final TokensRepository tokensRepository;
    private final UserCredentialsRepository userCredentialsRepository;

    /**
     * This is just a standard JWT + DB backed token state
     * @param request
     * @param response
     * @param filterChain
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // This short-circuits if we're already authenticated.
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        final String jwt;

        logger.info("Processing Auth Header...");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.error("Auth Header is null or does not have it's Bearer");
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7); // Remove "Bearer "
        logger.info("JWT removed Bearer " + jwt);
        if (jwt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Optional<TokensEntity> tokensEntity = tokensRepository.findByToken(jwt);
            if (tokensEntity.isEmpty() || tokensEntity.get().isExpired() || tokensEntity.get().isRevoked()) {
                // Token is invalid — skip authentication
                logger.warn("JWT couldn't be found in TokensEntity; treating it as invalid");
                tokensEntity.ifPresent(tokensRepository::delete);
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            Map<String, Object> claims = jwtService.verify(jwt);

            String userId = asString(claims.get("principalId"));
            if (userId == null) {
                // Fallback to your DB column if you're using service tokens
                UserCredentialsEntity uCE = tokensEntity.get().getUserCredentials();
                if (uCE == null || uCE.getUser() == null) {
                    logger.warn("Could not find user credentials under token; treating it as invalid");
                    tokensRepository.delete(tokensEntity.get());
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
                userId = uCE.getUser().getUserId().toString();
            }

            String email = asString(claims.get("email"));

            Object rolesClaim  = firstNonNull(
                    claims.get("roles"),            // your custom
                    claims.get("role"),
                    claims.get("cognito:groups"),   // AWS Cognito groups
                    claims.get("permissions")       // Auth0 often uses this
            );
            Object scopesClaim = firstNonNull(
                    claims.get("scope"),            // space-separated
                    claims.get("scp")               // array (Azure AD style)
            );

            String dbAccess = String.valueOf(tokensEntity.get().getAccessLevel()); // e.g., USER / PRO
            List<String> dbRoles = (dbAccess == null || dbAccess.isBlank())
                    ? List.of()
                    : List.of(dbAccess);

            Collection<GrantedAuthority> authorities = toAuthorities(rolesClaim, scopesClaim, dbRoles);

            // Also fold in DB access level if you want it to act like a role
            AuthUser principal = new AuthUser(userId, email, authorities);

            // Create the Authentication object
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);

            // Optionally add request details
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // ✅ Tell Spring Security the user/service is authenticated
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtService.TokenInvalidException e) {
            logger.debug("JWT invalid: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            logger.error("JWT filter error", e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}