package com.monday.monday_backend.auth.utils;

import com.monday.monday_backend.auth.principal.AuthUser;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthHelper {
    public static Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;

        Object p = auth.getPrincipal();

        // Your AuthUser class
        if (p instanceof AuthUser au) {
            return safeToLong(au.id()); // au.id() is String
        }
        if (p instanceof String s) {
            return safeToLong(s);
        }
        if (p instanceof Long l) {
            return l;
        }
        return null;
    }

    public static Long safeToLong(String s) {
        try { return s == null ? null : Long.valueOf(s); }
        catch (NumberFormatException e) { return null; }
    }
}
