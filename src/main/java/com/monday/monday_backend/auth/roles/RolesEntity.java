package com.monday.monday_backend.auth.roles;

import com.monday.monday_backend.auth.tokens.TokensEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@Entity
public class RolesEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private long role_id;

    @Enumerated(EnumType.STRING)
    private AccessLevel accessLevel;
}
