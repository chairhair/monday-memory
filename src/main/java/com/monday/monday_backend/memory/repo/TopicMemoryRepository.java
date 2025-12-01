package com.monday.monday_backend.memory.repo;

import com.monday.monday_backend.auth.users.UserEntity;
import com.monday.monday_backend.memory.entity.TopicMemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TopicMemoryRepository extends JpaRepository<TopicMemoryEntity, String> {
    public Optional<TopicMemoryEntity> findByNameAndUser(String name, UserEntity user);
}
