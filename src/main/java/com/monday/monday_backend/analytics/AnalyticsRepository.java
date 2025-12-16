package com.monday.monday_backend.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AnalyticsRepository extends JpaRepository<AnalyticsEventEntity, UUID> {
}
