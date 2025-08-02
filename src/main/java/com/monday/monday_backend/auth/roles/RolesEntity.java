package com.monday.monday_backend.auth.roles;

import com.monday.monday_backend.auth.tokens.TokensEntity;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
public class RolesEntity {
    @Id
    private long id;

    @Enumerated(EnumType.STRING)
    private AccessLevel accessLevel;

    private Instant expiration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "token")
    private TokensEntity token;
}
