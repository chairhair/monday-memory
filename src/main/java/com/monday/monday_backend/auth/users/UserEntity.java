package com.monday.monday_backend.auth.users;

import com.monday.monday_backend.auth.roles.RolesEntity;
import com.monday.monday_backend.auth.tokens.TokensEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@NoArgsConstructor
@Entity
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long user_id;

    @Setter
    @Column(name = "serviceName")
    private String serviceName;

    @Setter
    @Column(name = "email")
    private String email;

    @Setter
    @Column(name = "password")
    private String password;

    @ManyToMany
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "roles_id")
    )
    private Set<RolesEntity> roles;

    // For guest tokens
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TokensEntity> tokensEntity;

    public void addRoles(RolesEntity role) {
        if (roles == null || roles.isEmpty()) {
            roles = new HashSet<>();
        }
        roles.add(role);
    }
}
