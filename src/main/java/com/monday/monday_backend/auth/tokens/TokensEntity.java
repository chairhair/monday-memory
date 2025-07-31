package com.monday.monday_backend.auth.tokens;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity
public class TokensEntity {
    @Id
    private long id;

    @Getter
    String token;

    String serviceName;

    String accessLevel;

    @Getter
    boolean expired;

    @Getter
    boolean revoked;


}
