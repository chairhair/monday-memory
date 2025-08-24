package com.monday.monday_backend.auth.filters;

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
import java.util.Optional;

/**
 * DEV-Environment Only
 * Allows Developers to impersonate users when authneticating
 */
final public class DevImpersonationFilter extends OncePerRequestFilter {

    private final boolean enabled;
    public DevImpersonationFilter(boolean enabled) { this.enabled = enabled; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {
        if (enabled) {
            var user = req.getHeader("X-Dev-User");
            var scopes = Optional.ofNullable(req.getHeader("X-Dev-Scopes")).orElse("");
            if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var authorities = Arrays.stream(scopes.split(" ")).filter(s -> !s.isBlank())
                        .map(s -> "SCOPE_" + s).map(SimpleGrantedAuthority::new).toList();
                var auth = new UsernamePasswordAuthenticationToken(user, "N/A", authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(req, res);
    }
}
