package com.monday.monday_backend.memory.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.monday_backend.query.utils.JsonMapConverter;
import com.monday.monday_backend.query.utils.JsonStringListConverter;
import com.monday.shared.memory.dto.ResponseMemoryChunkDTO;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "memory_chunk",
        indexes = {
                @Index(name="idx_chunk_session_time", columnList = "sessionId,occurredAt"),
                @Index(name="idx_chunk_service", columnList = "service")
        },
        uniqueConstraints = {
                // helps de-dupe per session using canonical hash if you want
                @UniqueConstraint(name="uq_chunk_session_hash", columnNames = {"sessionId","hashSha256"})
        })
@Getter @NoArgsConstructor
public class MemoryChunkEntity {

    @Id
    @Setter(AccessLevel.PACKAGE)
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", length = 50)               // ULID/string
    private UUID memoryId;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sessionId", referencedColumnName = "sessionId")
    private SessionMemoryEntity session;   // FK to session

    @Setter
    @Column(nullable = false)
    private Instant occurredAt;

    @Setter
    @Column(nullable = false)
    private Instant ingestedAt;

    @Setter
    @Convert(converter = JsonStringListConverter.class)
    @Column(columnDefinition = "text")
    private List<String> tags;

    @Setter
    @Convert(converter = JsonMapConverter.class)
    @Column(columnDefinition = "text")     // swap to jsonb later
    private Map<String,Object> content;

    @Setter
    @Column(length = 64, nullable = false) // SHA-256 of normalized body
    private String hashSha256;

    public ResponseMemoryChunkDTO toDTO() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return new ResponseMemoryChunkDTO(objectMapper.writeValueAsString(content.entrySet()), session.getPrincipalType(), session.getPrincipalId(), ingestedAt, tags);
    }
}
