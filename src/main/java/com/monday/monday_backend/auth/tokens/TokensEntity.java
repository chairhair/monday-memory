package com.monday.monday_backend.auth.tokens;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class TokensEntity {
    @Id
    private long id;

    private String token;
}
