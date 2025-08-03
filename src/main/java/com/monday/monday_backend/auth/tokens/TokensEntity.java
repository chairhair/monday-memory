package com.monday.monday_backend.auth.tokens;

import com.monday.monday_backend.auth.roles.AccessLevel;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@Entity
public class TokensEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String token;

    @Column(name = "service_name")
    private String serviceName;

    @Column(name = "access_level")
    private AccessLevel accessLevel;

    private Instant timeCreated;

    private boolean expired;

    private boolean revoked;

}
