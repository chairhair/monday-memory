package com.monday.monday_backend.memory.repo;

import com.monday.monday_backend.memory.entity.SessionMemoryEntity;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.memory.session.utils.SessionSource;
import com.monday.shared.memory.session.utils.SessionState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionMemoryRepository extends JpaRepository<SessionMemoryEntity, String> {
    Optional<SessionMemoryEntity> findByPrincipalIdAndIdempotencyKey(String principalId, String idempotencyKey);

    Optional<SessionMemoryEntity> findBySessionIdAndPrincipalTypeAndPrincipalId(UUID sessionId, PrincipalType principalType, String principalId);

    Optional<SessionMemoryEntity> findBySessionId(UUID sessionId);

    List<SessionMemoryEntity> findByPrincipalTypeAndPrincipalId(PrincipalType principalType, String principalId);

    Optional<SessionMemoryEntity> findBySourceAndSourceConversationAndPrincipalTypeAndPrincipalIdAndIdempotencyKeyAndSessionState(
            SessionSource source,
            String sourceConversation,
            PrincipalType principalType,
            String principalId,
            String idempotencyKey,
            SessionState sessionState
    );

    @Query("""
        SELECT s
        FROM SessionMemoryEntity s
        WHERE s.principalType = :principalType
          AND s.principalId = :principalId
          AND (:since IS NULL OR s.updatedAt >= :since)
          AND (:until IS NULL OR s.updatedAt <= :until)
        ORDER BY s.updatedAt DESC
    """)
    Page<SessionMemoryEntity> findSessionsForPrincipal(
            @Param("principalType") PrincipalType principalType,
            @Param("principalId") String principalId,
            @Param("since") Instant since,
            @Param("until") Instant until,
            Pageable pageable
    );

    // Allows us to find by the "guest key"
    Optional<SessionMemoryEntity> findBySourceConversation(String sourceConversation);
}
