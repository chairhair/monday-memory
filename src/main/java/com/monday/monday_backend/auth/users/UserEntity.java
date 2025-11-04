package com.monday.monday_backend.auth.users;

import com.monday.monday_backend.auth.roles.RolesEntity;
import com.monday.monday_backend.auth.tokens.TokensEntity;
import com.monday.monday_backend.payment.entity.UserPlanEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Getter
@NoArgsConstructor
@Table(name = "user_entity", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
})
@Entity
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

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
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")
    )
    private Set<RolesEntity> roles;

    // For guest tokens
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TokensEntity> tokensEntity;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private UserPlanEntity userPlan;

    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() { this.createdAt = this.updatedAt = Instant.now(); }
    @PreUpdate
    void preUpdate() { this.updatedAt = Instant.now(); }

    public void addRole(RolesEntity role) { this.roles.add(role); }
    public boolean isPro() {
        return userPlan != null && userPlan.getPlan() != null
                && userPlan.getPlan().getCode().startsWith("PRO");
    }

}
