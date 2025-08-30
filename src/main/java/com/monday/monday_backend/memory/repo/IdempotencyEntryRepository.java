package com.monday.monday_backend.memory.repo;

import com.monday.monday_backend.memory.entity.IdempotencyEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyEntryRepository extends JpaRepository<IdempotencyEntry, String> {
}
