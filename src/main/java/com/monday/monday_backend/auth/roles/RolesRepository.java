package com.monday.monday_backend.auth.roles;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolesRepository extends JpaRepository<RolesEntity, Long> {
    Optional<RolesEntity> findByAccessLevel(AccessLevel accessLevel);

    List<RolesEntity> findAllByAccessLevelIn(List<AccessLevel> levels);
}
