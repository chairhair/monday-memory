package com.monday.monday_backend.memory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "session_memory",
        indexes = {
                @Index(name="idx_session_subject_service", columnList = "subjectId,service,sessionId"),
                @Index(name="idx_session_last_occurred", columnList = "lastOccurredAt")
        })
@Getter @Setter @NoArgsConstructor
public class SessionMemoryEntity {

    @Id @Column(length = 50)               // ULID/string
    private String sessionId;

    @Column(nullable = false, length = 64)
    private String service;                // e.g. "chat", "note", "etl"

    @Column(nullable = false, length = 64)
    private String subjectId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private int chunkCount;

    private Instant lastOccurredAt;

    @Version
    private long version;                  // optimistic updates on counters
}
