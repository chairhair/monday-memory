package com.monday.monday_backend.memory.repo;

import com.monday.monday_backend.memory.entity.SessionMemoryEntity;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.memory.session.utils.SessionSource;
import com.monday.shared.memory.session.utils.SessionState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionMemoryRepository extends JpaRepository<SessionMemoryEntity, String> {
    Optional<SessionMemoryEntity> findByPrincipalIdAndIdempotencyKey(String principalId, String idempotencyKey);

    Optional<SessionMemoryEntity> findBySessionIdAndPrincipalTypeAndPrincipalId(UUID sessionId, PrincipalType principalType, String principalId);

    List<SessionMemoryEntity> findByPrincipalTypeAndPrincipalId(PrincipalType principalType, String principalId);

    Optional<SessionMemoryEntity> findBySourceAndSourceConversationAndPrincipalTypeAndPrincipalIdAndIdempotencyKeyAndSessionState(
            SessionSource source,
            String sourceConversation,
            PrincipalType principalType,
            String principalId,
            String idempotencyKey,
            SessionState sessionState
    );

    Optional<SessionMemoryEntity> findTopBySourceConversationAndPrincipalTypeAndPrincipalIdOrderByUpdatedAtDesc(
            String sourceConversationKey,
            PrincipalType principalType,
            String principalId
    );

    // Allows us to find by the "guest key"
    Optional<SessionMemoryEntity> findBySourceConversation(String sourceConversation);
}
