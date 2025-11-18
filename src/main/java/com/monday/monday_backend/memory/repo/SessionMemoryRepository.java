package com.monday.monday_backend.memory.repo;

import com.monday.monday_backend.memory.entity.SessionMemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionMemoryRepository extends JpaRepository<SessionMemoryEntity, String> {
    Optional<SessionMemoryEntity> findByPrincipalIdAndIdempotencyKey(String principalId, String idempotencyKey);
}
