package com.monday.monday_backend.auth.principal;

import com.monday.monday_backend.auth.roles.RolesEntity;
import com.monday.shared.auth.utils.AccessLevel;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AccessLevelResolver {

    public AccessLevel resolve(Set<RolesEntity> roles) {
        if (roles == null || roles.isEmpty()) {
            return AccessLevel.GUEST;
        }

        // Example: pick the "highest" role by some priority
        boolean isAdmin = roles.stream().anyMatch(r -> r.getAccessLevel().toString().equals("ADMIN"));
        if (isAdmin) return AccessLevel.ADMIN;

        boolean isUser = roles.stream().anyMatch(r -> r.getAccessLevel().toString().equals("USER"));
        if (isUser) return AccessLevel.USER;

        // fallback
        return AccessLevel.GUEST;
    }
}
