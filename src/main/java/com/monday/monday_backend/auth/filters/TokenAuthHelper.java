package com.monday.monday_backend.auth.filters;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TokenAuthHelper {
    public static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    public static Object firstNonNull(Object... xs) {
        for (Object x : xs) if (x != null) return x;
        return null;
    }

    /** Convert roles & scopes to Spring authorities, supporting strings or arrays. */
    public static Collection<GrantedAuthority> toAuthorities(Object rolesClaim, Object scopesClaim, Collection<String> extraRoles) {
        Stream<String> roles = Stream.concat(
                flatten(rolesClaim),
                extraRoles == null ? Stream.empty() : extraRoles.stream()
        ).map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r);

        Stream<String> scopes = flatten(scopesClaim)
                .map(s -> s.startsWith("SCOPE_") ? s : "SCOPE_" + s);

        return Stream.concat(roles, scopes)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Raw scopes list (for UI/logs) without any prefixing. */
    public static Collection<String> toScopeList(Object scopesClaim) {
        return flatten(scopesClaim)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Accept collection or comma/space-separated string. */
    public static Stream<String> flatten(Object claim) {
        if (claim == null) return Stream.empty();
        if (claim instanceof Collection<?> c) {
            return c.stream().filter(Objects::nonNull).map(Object::toString).flatMap((s) -> splitTokens(s));
        }
        return splitTokens(claim.toString());
    }

    public static Stream<String> splitTokens(String s) {
        return Arrays.stream(s.split("[,\\s]+"));
    }
}
