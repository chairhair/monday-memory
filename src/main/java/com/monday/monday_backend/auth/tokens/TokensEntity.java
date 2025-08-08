package com.monday.monday_backend.auth.tokens;

import com.monday.monday_backend.auth.roles.AccessLevel;
import com.monday.monday_backend.auth.users.UserEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@NoArgsConstructor
@Entity
public class TokensEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Setter
    @Column(nullable = false)
    private String token;

    @Setter
    @Column(name = "service_name")
    private String serviceName;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "access_level")
    private AccessLevel accessLevel;

    @Setter
    private Instant timeCreated;

    @Setter
    private boolean expired;

    @Setter
    private boolean revoked;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private UserEntity user;
}
