package com.monday.monday_backend.memory.repo;

import com.monday.monday_backend.memory.entity.SessionMemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionMemoryRepository extends JpaRepository<SessionMemoryEntity, String> {
}
