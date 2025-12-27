package com.monday.monday_backend.auth.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.monday_backend.auth.principal.AuthUser;
import com.monday.monday_backend.auth.principal.AuthorizationMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;


import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * DEV-Environment Only
 * Allows Developers to impersonate users when authenticating
 */
final public class DevImpersonationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {

        var user = req.getHeader("X-Dev-User");
        if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var node = new ObjectMapper().readTree(user);
            String id = node.path("id").asText(null);
            String email = node.path("email").asText(null);
            var roles = Optional.ofNullable(req.getHeader("X-Dev-Roles")).orElse("");
            var scopes = Optional.ofNullable(req.getHeader("X-Dev-Scopes")).orElse("");
            var authorities = AuthorizationMapper.toAuthorities(roles, scopes);

            var principal = new AuthUser(id, email, roles.isEmpty() ? List.of(new SimpleGrantedAuthority(("ROLE_USER"))) : authorities);
            var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(req, res);
    }
}
