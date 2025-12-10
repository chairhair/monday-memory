package com.monday.monday_backend.memory.repo;

import com.monday.monday_backend.memory.entity.SessionOptionsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SessionOptionsRepository extends JpaRepository<SessionOptionsEntity, UUID> {
}
