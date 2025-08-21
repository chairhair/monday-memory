package com.monday.monday_backend.query.memory.entity;

import com.monday.monday_backend.query.memory.dto.SessionMemoryRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "Provides a database entry for a logical sequence of chunks tied to a serviceName and sessionId (e.g., a chat or task run).")
@Entity
public class SessionMemoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long sessionMemoryId;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private String sessionId;

    @OneToMany(mappedBy = "memoryChunkId")
    private List<MemoryChunkEntity> sessionsDiscussed;

    public SessionMemoryEntity(SessionMemoryRequestDTO sessionMemoryRequestDTO) {
        this.serviceName = sessionMemoryRequestDTO.serviceName();
        this.sessionId = sessionMemoryRequestDTO.sessionId();
        this.sessionsDiscussed = sessionMemoryRequestDTO.convertMemoryChunkListToEntityList();
    }
}
