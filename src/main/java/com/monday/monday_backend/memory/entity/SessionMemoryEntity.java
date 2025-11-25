package com.monday.monday_backend.memory.entity;

import com.monday.shared.memory.session.dto.SessionMemoryResponseDTO;
import com.monday.shared.memory.session.utils.PrincipalType;
import com.monday.shared.memory.session.utils.SessionSource;
import com.monday.shared.memory.session.utils.SessionState;
import com.monday.shared.recording.RecordingScope;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "session_memory",
        indexes = {
                @Index(name="idx_session_identity", columnList = "principalId,sessionId,source,sourceConversation"),
                @Index(name="idx_session_last_occurred", columnList = "lastOccurredAt"),
                @Index(
                        name = "idx_session_idempotency",
                        columnList = "principalId,source,sourceConversation,idempotencyKey,sessionState"
                )
        })
@Getter @NoArgsConstructor
public class SessionMemoryEntity {

    @Id
    @Setter(AccessLevel.PACKAGE)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", length=50)
    private UUID sessionId;

    @Setter
    @OneToMany(mappedBy = "session")
    private List<MemoryChunkEntity> chunks;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private SessionSource source;                // e.g. "Discord", "Notion", etc.

    @Setter
    @Column(nullable = false, length = 128)
    private String sourceConversation;    // e.g. The chat number associated with it; thread id, etc.

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PrincipalType principalType;

    @Setter
    @Column(nullable = false, length = 64)
    private String principalId;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionState sessionState;

    @Setter
    @Column(nullable = false)
    private Instant createdAt;

    @Setter
    @Column(nullable = false)
    private Instant updatedAt;

    @Setter
    @Column
    private Instant endedAt;

    @Setter
    @Column(nullable = false)
    private int chunkCount;

    @Setter
    @Column
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Setter
    private RecordingScope scope;

    @Setter
    private Instant lastOccurredAt;

    @Setter
    private Long topicId;

    @Setter
    @Version
    private long version;                  // optimistic updates on counters

    public SessionMemoryResponseDTO toDTO(int statusCode, String message) {
        return new SessionMemoryResponseDTO(HttpStatus.valueOf(statusCode), scope, message, Collections.singletonList(sessionId.toString()), null, null, this.principalId, null);
    }

    public SessionMemoryResponseDTO toDTO(HttpStatus statusCode, RecordingScope scope, String message) {
        return new SessionMemoryResponseDTO(statusCode, scope, message, Collections.singletonList(sessionId.toString()), null, null, this.principalId, null);
    }
}
