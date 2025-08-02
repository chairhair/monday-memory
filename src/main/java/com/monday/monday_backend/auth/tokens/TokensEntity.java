package com.monday.monday_backend.auth.tokens;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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
    private String accessLevel;

    private boolean expired;

    private boolean revoked;

}
