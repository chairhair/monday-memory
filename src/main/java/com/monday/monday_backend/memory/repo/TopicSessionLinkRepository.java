package com.monday.monday_backend.memory.repo;

import com.monday.monday_backend.memory.entity.TopicSessionLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TopicSessionLinkRepository extends JpaRepository<TopicSessionLinkEntity, String> {
}
