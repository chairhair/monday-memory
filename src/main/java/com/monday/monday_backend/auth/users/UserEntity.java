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
import java.util.UUID;

/**
 * Our canonical internal account
 */
@Getter
@NoArgsConstructor
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
})
@Entity
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    @Setter
    @Column(name = "email")
    private String email;

    @Setter
    @Column(name = "display_name")
    private String displayName;

    @ManyToMany
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")
    )
    private Set<RolesEntity> roles;

    @Setter
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private UserPlanEntity userPlan;

    @Setter
    @OneToOne(mappedBy = "user_preferences", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private UserPreferencesEntity userPreferences;

    @Setter
    @Column
    private boolean linked;


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
