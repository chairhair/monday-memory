package com.monday.monday_backend.memory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "topic_memory",
        indexes = {
                @Index(name="idx_topic_subject", columnList = "subjectId"),
                @Index(name="idx_topic_updated", columnList = "updatedAt")
        })
@Getter @NoArgsConstructor
public class TopicMemoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)               // ULID/string
    @Column(columnDefinition = "id")
    private UUID topicId;

    @Setter
    @OneToMany(mappedBy = "topic", fetch = FetchType.LAZY)
    private List<SessionMemoryEntity> sessions;

    @Setter
    @Column(nullable = false, length = 64)
    private String subjectId;

    @Setter
    @Column(nullable = false, length = 160)
    private String title;

    @Setter
    @Lob
    private String summaryMd;

    @Setter
    @Column(nullable = false)
    private Instant createdAt;

    @Setter
    @Column(nullable = false)
    private Instant updatedAt;
}
