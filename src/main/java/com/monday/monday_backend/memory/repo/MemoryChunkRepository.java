package com.monday.monday_backend.memory.repo;

import com.monday.monday_backend.memory.entity.MemoryChunkEntity;
import com.monday.monday_backend.memory.entity.SessionMemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemoryChunkRepository extends JpaRepository<MemoryChunkEntity, String> {
    List<MemoryChunkEntity> findTop5BySessionOrderByCreatedAtDesc(SessionMemoryEntity sessionMemoryEntity);
}
