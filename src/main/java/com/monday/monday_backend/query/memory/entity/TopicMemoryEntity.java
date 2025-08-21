package com.monday.monday_backend.query.memory.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToMany;
import org.springframework.data.annotation.Id;

import java.util.List;

@Schema(description = "Provides a database entry for grouping/summarization across sessions")
@Entity
public class TopicMemoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long topicMemoryId;



}
