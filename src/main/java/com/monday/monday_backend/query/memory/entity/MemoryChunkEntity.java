package com.monday.monday_backend.query.memory.entity;

import com.monday.monday_backend.query.utils.JsonMapConverter;
import com.monday.monday_backend.query.utils.JsonStringListConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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
@Getter @Setter @NoArgsConstructor
public class MemoryChunkEntity {

    @Id @Column(length = 50)               // ULID/string
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sessionId", referencedColumnName = "sessionId")
    private SessionMemoryEntity session;   // FK to session

    @Column(nullable = false, length = 64)
    private String service;                // denormalized for filtering

    @Column(nullable = false, length = 64)
    private String subjectId;              // denormalized for filtering

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private Instant ingestedAt;

    @Convert(converter = JsonStringListConverter.class)
    @Column(columnDefinition = "text")
    private List<String> tags;

    @Convert(converter = JsonMapConverter.class)
    @Column(columnDefinition = "text")     // swap to jsonb later
    private Map<String,Object> content;

    @Column(length = 64, nullable = false) // SHA-256 of normalized body
    private String hashSha256;
}
