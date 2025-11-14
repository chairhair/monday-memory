package com.monday.monday_backend.communication.entity;

import com.monday.monday_backend.auth.principal.AuthUser;
import com.monday.shared.auth.utils.ExternalProvider;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_external_account")
public class UserExternalAccount {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private AuthUser user;

    @Enumerated(EnumType.STRING)
    private ExternalProvider provider;

    private String externalId;

    private Instant createdAt;
}
