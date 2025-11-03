package com.monday.monday_backend.auth.principal;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The following mapper will push both roles and scopes into
 * an authority.
 *
 * Scopes (i.e. SCOPE_) -> fits into the external OAuth2 setup (Auth0, Okta, Cognito)
 * Roles -> Fits into JWT and Dev Filters.
 *
 */
public final class AuthorizationMapper {

    private AuthorizationMapper() {}

    public static Collection<GrantedAuthority> toAuthorities(Object rolesClaim, Object scopesClaim) {
        // roles can be ["USER","PRO"] or "USER PRO" or null
        Stream<String> roleStream = flattenClaim(rolesClaim)
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r);

        // scopes can be ["read","write"] or "read write" or "email openid" or null
        Stream<String> scopeStream = flattenClaim(scopesClaim)
                .map(s -> s.startsWith("SCOPE_") ? s : "SCOPE_" + s);

        return Stream.concat(roleStream, scopeStream)
                .filter(s -> !s.isBlank())
                .map(String::trim)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Accepts array/list or space/comma-separated string; returns uppercased tokens without prefix. */
    private static Stream<String> flattenClaim(Object claim) {
        if (claim == null) return Stream.empty();
        if (claim instanceof Collection<?> col) {
            return col.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .flatMap(AuthorizationMapper::splitTokens)
                    .map(String::toUpperCase);
        }
        return splitTokens(claim.toString()).map(String::toUpperCase);
    }

    private static Stream<String> splitTokens(String s) {
        // split on comma or whitespace
        return Arrays.stream(s.split("[,\\s]+")).filter(t -> !t.isBlank());
    }
}
