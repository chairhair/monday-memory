package com.monday.monday_backend.communication.entity;

import com.monday.monday_backend.auth.users.UserEntity;
import com.monday.shared.auth.utils.ExternalProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Entity
@Table(name = "user_external_account")
public class UserExternalAccount {

    @Id
    @GeneratedValue
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Setter
    @Enumerated(EnumType.STRING)
    private ExternalProvider provider;

    @Setter
    private String externalId;

    @Setter
    private Instant createdAt;
}
