package com.monday.monday_backend.auth.roles;

import com.monday.monday_backend.auth.users.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@NoArgsConstructor
@Table(name = "role_entity")
@Entity
public class RolesEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Setter
    @Enumerated(EnumType.STRING)
    private AccessLevel accessLevel;

    @Setter
    @ManyToMany(mappedBy = "roles")
    private Set<UserEntity> users = new HashSet<>();
}
