package com.monday.monday_backend.auth.roles;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
public class RolesEntity {
    @Id
    private long id;

    @Enumerated(EnumType.STRING)
    private AccessLevel accessLevel;

    private Instant expiration;

    @ManyToOne
    @JoinColumn(name = "token_id")
    private String token;
}
