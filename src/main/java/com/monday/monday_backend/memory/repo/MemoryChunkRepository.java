package com.monday.monday_backend.memory.repo;

import com.monday.monday_backend.memory.entity.MemoryChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemoryChunkRepository extends JpaRepository<MemoryChunkEntity, String> {
}
