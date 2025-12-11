package com.monday.monday_backend.memory.repo;

import com.monday.monday_backend.memory.entity.MemoryChunkEntity;
import com.monday.monday_backend.memory.entity.SessionMemoryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MemoryChunkRepository extends JpaRepository<MemoryChunkEntity, UUID> {
    List<MemoryChunkEntity> findTop5BySessionOrderByOccurredAtAsc(SessionMemoryEntity sessionMemoryEntity);

    List<MemoryChunkEntity> findBySessionOrderByOccurredAtAsc(SessionMemoryEntity sessionMemoryEntity, Pageable pageable);

    List<MemoryChunkEntity> findBySessionAndOccurredAtAfter(
            SessionMemoryEntity session,
            Instant since,
            Pageable pageable
    );
}
