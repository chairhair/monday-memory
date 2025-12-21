package com.monday.monday_backend.options;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OptionsRepository extends JpaRepository<OptionsEntity, UUID> {
}
