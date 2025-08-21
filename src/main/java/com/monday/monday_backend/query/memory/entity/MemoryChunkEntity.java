package com.monday.monday_backend.query.memory.entity;

import com.monday.monday_backend.query.memory.dto.MemoryChunkDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.util.List;

@Getter
@NoArgsConstructor
@Entity
public class MemoryChunkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long memoryChunkId;

    @Setter
    @Column(nullable = false)
    private String content;

    @Setter
    @Column(nullable = false)
    private Instant timestamp;

    @Setter
    @Column(nullable = false)
    private List<String> tags;

    public MemoryChunkEntity(MemoryChunkDTO memoryChunkDTO) {
        this.content = memoryChunkDTO.content();
        this.timestamp = memoryChunkDTO.timestamp();
        this.tags = memoryChunkDTO.tags();
    }
}
