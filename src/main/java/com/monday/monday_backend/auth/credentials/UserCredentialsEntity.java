package com.monday.monday_backend.auth.credentials;

import com.monday.monday_backend.auth.tokens.TokensEntity;
import com.monday.monday_backend.auth.users.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;

@Getter
@Entity
@Table(name="user_credentials")
public class UserCredentialsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    UUID id;

    @Setter
    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    UserEntity user;

    @Setter
    @Column(name = "password")
    private String password;

    @Setter
    @OneToMany(
            mappedBy = "userCredentials",
            fetch = LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<TokensEntity> tokens;

    public void addToken(TokensEntity token) {
        if (tokens == null) {
            tokens = new ArrayList<>();
        }
        tokens.add(token);
        token.setUserCredentials(this);
    }
}
