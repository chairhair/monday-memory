package com.monday.monday_backend.memory.repo;

import com.monday.monday_backend.memory.entity.SessionMemoryEntity;
import com.monday.shared.memory.session.utils.PrincipalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionMemoryRepository extends JpaRepository<SessionMemoryEntity, String> {
    Optional<SessionMemoryEntity> findByPrincipalIdAndIdempotencyKey(String principalId, String idempotencyKey);

    Optional<SessionMemoryEntity> findBySessionIdAndPrincipalTypeAndPrincipalId(UUID sessionId, PrincipalType principalType, String principalId);
}
